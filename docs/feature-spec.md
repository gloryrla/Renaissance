# Renaissance 기능 명세서

그룹 외식 메뉴 **추천 → 투표 → 집계** 백엔드 서비스의 기능/도메인 명세.
구현(코드) 기준으로 작성했으며, REST 인터페이스 상세는 [`api-spec.md`](./api-spec.md) 참고.

- 스택: Spring Boot 4.x · Spring Data JPA(Hibernate) · JWT 인증 · 카카오 OAuth/로컬 API
- DB: 로컬 H2 / 운영 PostgreSQL (Railway)
- 형태: 순수 JSON REST API (프론트엔드 SPA 별도)

---

## 1. 한눈에 보는 흐름

```
[카카오 로그인] → JWT 발급
        │
        ▼
[그룹 생성] ──초대코드──▶ [그룹 참여]
        │
        ▼
[투표 생성]  status=RECOMMENDING
        │
   ② 그룹원 선호 제출 (안 당기는 카테고리 + 알레르기)
        │
   ③ 추천 실행  ──▶ 후보 메뉴 3개 확정  status=VOTING
        │
   ④ 후보에 호/불호 투표
        │   └─ 그룹원 전원이 제출하면
   ⑤ 자동 집계  ──▶ 최종 메뉴 1개 확정  status=CLOSED
        │
        ▼
[메뉴명으로 주변 식당 검색] (카카오 로컬)
```

---

## 2. 액터 / 권한

| 액터 | 설명 |
|------|------|
| 비로그인 사용자 | 카카오 로그인 URL 조회, 카카오 로그인(JWT 발급), 메뉴 조회·식당 검색만 가능 |
| 로그인 회원 (Member) | JWT 보유. 그룹 생성/참여 가능 |
| 그룹 OWNER | 그룹 생성자. 자동으로 그룹 멤버이며 OWNER 역할 |
| 그룹 MEMBER | 초대코드로 참여한 일반 그룹원 |

> **권한 규칙:** 투표 진행(②~④)과 그룹원 조회는 **해당 투표가 속한 그룹의 멤버만** 가능하다.
> 역할 차등 권한은 **강제 마감(⑤)뿐**이다 — 강제 마감은 OWNER만 가능. 그 외(추천 실행 ③ 포함)는 그룹 멤버라면 누구나 호출 가능하고, 자동 집계는 전원 투표 시 자동 실행된다.
> 행위자(memberId)는 항상 `Authorization: Bearer <JWT>` 헤더에서 추출한다(`@LoginMember`).

---

## 3. 도메인 모델

| 엔티티 | 핵심 필드 | 관계 / 비고 |
|--------|-----------|-------------|
| **Member** | id, name, accessToken(카카오) | 카카오 로그인 시 생성/조회(upsert) |
| **Group** | id, name, description, inviteCode, owner | inviteCode = UUID 앞 8자리 대문자 |
| **GroupMember** | id, group, member, role(OWNER/MEMBER) | 회원-그룹 N:M 연결 |
| **Menu** | id, name, cuisine, restrictions(Set) | 알레르겐 집합 보유. 166개 시드 |
| **Vote** | id, title, deadline, group, status, resultMenu, relaxed, impossible, relaxedCuisines | 투표 1건의 전체 상태 |
| **VotePreference** | vote, member, dislikedCuisines, restrictions | 그룹원 1명의 ② 선호 입력(멤버당 1건, 덮어쓰기) |
| **VoteCandidate** | vote, menu | ③ 추천으로 확정된 후보(최대 3개) |
| **Ballot** | vote, member, menu, choice(LIKE/DISLIKE) | ④ 호/불호 1표 |

---

## 4. 공통 Enum

### Cuisine — 음식 카테고리 (8종)
`FASTFOOD`(패스트푸드) `KOREAN`(한식) `STEW_SOUP`(찜·탕) `MEAT`(고기) `ASIAN`(아시안) `CHINESE`(중식) `WESTERN`(양식) `JAPANESE`(일식)

### Restriction — 알레르겐 (7종, 추천 1단계 하드필터)
`EGG`(계란) `MILK`(우유) `WHEAT`(밀) `SOY`(대두) `NUT`(견과) `CRUSTACEAN`(갑각류) `SEAFOOD`(해산물)
> CSV 데이터셋의 알레르겐 코드와 1:1 일치.

### Choice — 호/불호
`LIKE` · `DISLIKE`

### VoteStatus — 투표 단계
`RECOMMENDING`(선호 수집) → `VOTING`(호/불호 투표) → `CLOSED`(종료)

### GroupRole
`OWNER` · `MEMBER`

> 참고: `FoodCategory` enum이 코드에 남아 있으나 현재 추천/투표 로직에서는 사용하지 않는다(레거시). 실제 분류는 `Cuisine` 사용.

---

## 5. 기능별 상세

### 5.1 인증 (Auth)

| 기능 | 설명 | 인증 |
|------|------|------|
| 카카오 인가 URL 조회 | client_id·redirect_uri로 카카오 authorize URL 생성 반환 | 불필요 |
| 카카오 로그인 | 프론트가 받은 인가코드 → 카카오 토큰 교환 → 사용자 정보 조회 → **회원 upsert** → JWT 발급 | 불필요 |
| 내 정보 조회 | JWT의 memberId로 회원 정보 반환 | 필요 |
| 로그아웃 | 저장된 카카오 accessToken 만료 처리. **JWT 자체는 클라이언트가 폐기**(서버 블랙리스트 없음) | 필요 |

- 회원은 카카오 ID 기준으로 없으면 생성·있으면 조회(`getOrSaveMember`).
- JWT 만료 기본 1일(`JWT_EXPIRATION_MS`).

### 5.2 그룹 (Group) — 전부 로그인 필요

| 기능 | 규칙 |
|------|------|
| 그룹 생성 | 생성자가 **OWNER로 자동 가입**. 8자리 초대코드 자동 발급 |
| 내 그룹 목록 | 로그인 회원이 속한 그룹 전체 |
| 초대코드로 참여 | 코드로 그룹 조회. 없으면 404, **이미 가입했으면 409**(중복 가입 방지), 정상 시 MEMBER로 가입 |
| 그룹원 목록 | **그룹 멤버만** 조회 가능(아니면 403) |

### 5.3 메뉴 (Menu)

| 기능 | 규칙 |
|------|------|
| 전체 메뉴 조회 | 인증 불필요 |
| 메뉴 등록 | name·cuisine·restrictions로 단건 추가(시드 보강용) |
| **시드 자동 적재** | 앱 시작 시 `menu-data.csv`(166개) 1회 적재. **멱등** — 메뉴가 1개라도 있으면 건너뜀. `app.menu-seed.enabled=false`로 비활성 가능 |

- CSV 형식: `name,cuisine,allergens` (allergens는 `;` 구분). cuisine/allergens 값은 enum 이름과 일치해야 함.

### 5.4 투표 생성 (①)

- `POST /api/groups/{groupId}/votes` — title, deadline(선택)으로 생성.
- 생성 직후 status = `RECOMMENDING`.
- 그룹이 없으면 404.

### 5.5 투표 진행 (②~⑤) — 로그인 + 그룹 멤버 필요

각 단계는 투표 status가 정확히 맞아야 하며, 어긋나면 **409**.

| 단계 | 동작 | 전제 status | 주요 규칙 |
|------|------|-------------|-----------|
| ② 선호 제출 | dislikedCuisines + restrictions 저장 | RECOMMENDING | **멤버당 1건, 재제출 시 덮어쓰기.** 마감 지났으면 409 |
| ③ 추천 실행 | 모인 선호로 후보 3개 계산·저장, status→VOTING | RECOMMENDING | **제출된 선호 0건이면 409.** 재호출 시 기존 후보 삭제 후 재계산. 마감 지났으면 409 |
| ④ 호/불호 투표 | 후보별 LIKE/DISLIKE 저장 | VOTING | **재제출 시 그 멤버 표 전체 덮어쓰기.** 후보 아닌 메뉴면 400. 마감 지났으면 409. **전원 제출 시 ⑤ 자동 실행** |
| ⑤ 집계 (자동) | 최종 메뉴 확정, status→CLOSED | VOTING | **별도 마무리 버튼 없음.** ④에서 그룹원 전원이 투표를 마치면 자동 실행. 아래 7장 집계 규칙 적용 |
| ⑤ 강제 마감 (방장) | 모인 표로 즉시 집계, status→CLOSED | VOTING | **방장(OWNER)만.** 전원이 투표하지 않았어도 마감 가능. 방장 아니면 403 |
| 상태/결과 조회 | 후보·결과·완화정보 반환 | 모든 단계 | 추천 전 candidates=[], 집계 전 resultMenu=null. **집계 결과는 이 API의 resultMenu로 확인** |
| 미제출 그룹원 조회 | 아직 ② 선호 안 낸 그룹원 목록 | 모든 단계 | "누구를 기다려야 하나" 확인용 |

> **마감(deadline) 검증:** 선호 제출(②)·추천 실행(③)·호/불호 투표(④)에 적용된다. ⑤ 집계는 ④ 처리 중 자동 실행되므로(이미 마감 검증을 통과한 시점) 별도 검증이 없고, 조회 API에도 없다.
>
> **자동 집계 트리거:** 그룹원 전원이 **각자 한 표 이상** 제출하면 마지막 제출을 처리하는 트랜잭션 안에서 집계가 실행된다. 일부 그룹원이 끝까지 투표하지 않으면 자동 집계되지 않으며, 이때는 **방장이 강제 마감**(⑤ 강제 마감)으로 종료할 수 있다.

### 5.6 알아둘 점 / 엣지 케이스

- **전원이 투표하면 자동 집계, 아니면 방장이 강제 마감.** 자동 집계는 "그룹원 전원 제출"이 트리거다. 일부가 끝까지 투표하지 않으면 방장이 `POST /api/votes/{voteId}/close`로 모인 표만으로 마감한다.
- 마감시각(deadline) 도달만으로는 자동 집계되지 않는다(스케줄러 미구현). 마감 후 종료가 필요하면 방장 강제 마감을 사용한다.
- 마지막 투표자의 ④ 응답 본문에는 결과가 담기지 않는다. 결과는 조회 API의 `resultMenu`로 받는다.

---

## 6. 추천 알고리즘 (1~3단계) — `RecommendationService`

핵심 원칙: **한 명이라도 못/안 먹는 메뉴는 후보에 나오지 않는다.** 후보 목표 개수 = 3.

그룹 단위로 선호를 합집합 집계한다.
- `groupRestrictions` = 전원 알레르겐의 합집합
- `dislikeCuisines` = 전원 비선호 카테고리의 합집합

**1단계 — 알레르겐 하드필터 (절대 완화 안 함)**
전체 메뉴 중 그룹 알레르겐과 겹치지 않는(서로소) 메뉴만 남긴다.
→ 남은 메뉴가 3개 미만이면 **추천 불가**(`impossible=true`)로 종료. 있는 만큼만 후보로 반환.

**2단계 — 카테고리 비선호 하드필터**
1단계 통과 메뉴에서 비선호 카테고리를 제외한다.
→ 풀이 3개 이상이면 그대로 통과(완화 없음).

**3단계 — 부족 시 단계적 완화 (`relaxAndFill`)**
2단계 풀이 3개 미만일 때만:
- 비선호 표(dislike vote)가 **적은 카테고리부터** 하나씩 다시 허용(되살림)
- 풀이 3개 이상이 되면 중단
- 되살린 카테고리는 `relaxedCuisines`에 기록, `relaxed=true`
- 다 되살려도 3개 미만이면 있는 만큼만 후보로 반환

**후보 선택**
완성된 풀에서 **중복 없이 랜덤 3개**(풀이 3개 이하면 전부). 테스트에서는 `Random` 시드 고정 가능.

**추천 결과 부가정보**
| 필드 | 의미 |
|------|------|
| `relaxed` | 카테고리 완화가 일어났는가 |
| `relaxedCuisines` | 완화로 되살린 카테고리 집합 |
| `impossible` | 알레르겐만으로 후보를 3개 못 만든 경우 |

---

## 7. 집계 규칙 (4~5단계) — `VoteService`

후보 3개에 대한 호/불호를 멤버별로 모아 최종 1개를 고른다. 우선순위:

1. **호(LIKE) 수가 가장 많은 메뉴**
2. 동점이면 **불호(DISLIKE)가 적은 메뉴**
3. 그래도 동점이면 **(호 − 불호) 순값이 큰 메뉴**

- 후보가 없으면 예외(IllegalArgumentException → 400).
- 멤버가 일부 후보에만 투표해도 집계 가능(누락 후보는 0표 처리).

---

## 8. 비즈니스 규칙 / 검증 요약

- **단계 강제:** 잘못된 status에서의 호출은 모두 409. 단계는 한 방향(RECOMMENDING→VOTING→CLOSED).
- **재제출 멱등성:** ② 선호와 ④ 호/불호는 멤버 단위로 덮어쓰기 → 마지막 제출만 유효.
- **재추천 가능:** ③은 VOTING 진입 전(RECOMMENDING)에 한해 여러 번 호출 시 후보를 갈아엎고 재계산.
- **그룹 격리:** 그룹 멤버가 아니면 그룹원 조회·투표 진행 모두 403.
- **중복 가입 방지:** 같은 그룹에 이미 가입한 회원의 재참여는 409.

---

## 9. 오류 → HTTP 상태 매핑 (`GlobalExceptionHandler`)

| 예외 | 상태 | 대표 상황 |
|------|------|-----------|
| `UnauthorizedException` | 401 | 로그인 필요 / 토큰 무효·만료 |
| `ForbiddenException` | 403 | 그룹 멤버 아님 |
| `NotFoundException` | 404 | 그룹·투표·메뉴·초대코드 없음 |
| `IllegalStateException` | 409 | 잘못된 단계·마감된 투표·선호 0건·중복 가입 |
| `IllegalArgumentException` | 400 | 후보 아닌 메뉴 투표, 후보 없음 등 잘못된 입력 |

응답 본문 형식:
```json
{ "timestamp": "2026-06-28T12:00:00.000", "status": 404, "error": "Not Found", "message": "투표가 존재하지 않습니다. id=99" }
```

---

## 10. 환경 / 설정

- 프로파일: `SPRING_PROFILES_ACTIVE`(미지정 시 `local`). 로컬=H2, 운영(`prod`)=PostgreSQL.
- 세션: JDBC 세션 저장(테이블 자동 생성), 쿠키 30일 / 운영은 secure·same-site=strict.
- 시크릿(카카오 키, JWT_SECRET 등)은 **환경변수로만** 주입 — 파일에 실제 키를 적지 않는다.
- CORS 허용 출처: `CORS_ALLOWED_ORIGINS`(기본 `http://localhost:3000`).

상세 환경변수 표는 [`api-spec.md`](./api-spec.md) 참고.

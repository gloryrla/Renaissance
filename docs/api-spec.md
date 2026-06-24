# Renaissance API 명세서

그룹 외식 메뉴 추천·투표 백엔드 REST API. **순수 JSON API** (별도 프론트엔드 SPA 용).

- **Base URL (운영)**: `https://renaissance-production-f569.up.railway.app`
- **Base URL (로컬)**: `http://localhost:8080`
- 요청/응답 형식: `application/json`, 인코딩 `UTF-8`
- enum은 JSON에서 `name` 문자열로 주고받습니다. 예: `"KOREAN"`, `"NUT"`, `"LIKE"`.
- CORS: 허용 출처는 `CORS_ALLOWED_ORIGINS` 환경변수로 설정 (기본 `http://localhost:3000`).

---

## 인증 (JWT)

토큰 기반입니다. 카카오 로그인으로 **JWT**를 받고, 이후 요청 헤더에 실어 보냅니다.

```
Authorization: Bearer <발급받은 JWT>
```

### 로그인 흐름
1. 프론트가 사용자를 **카카오 인가 페이지**로 보냄 (URL은 `GET /api/auth/kakao/login-url`로 받거나 직접 구성)
2. 카카오가 `redirect_uri`(= 프론트 페이지)로 `?code=...`를 붙여 리다이렉트
3. 프론트가 그 `code`를 **`POST /api/auth/kakao`** 로 전달
4. 서버가 JWT + 회원정보를 JSON으로 반환 → 프론트가 토큰 저장(localStorage 등)
5. 이후 보호된 API 호출 시 `Authorization: Bearer` 헤더 사용

- 토큰 없음/만료/위조 → **401**
- 그룹 멤버 아님 → **403**

> `redirect_uri`(카카오 콘솔 등록값 = 프론트의 code 수신 페이지)는 `KAKAO_REDIRECT_URI` 환경변수로 설정. 기본 `http://localhost:3000/oauth/kakao`.

---

## 공통 Enum

### Cuisine — 음식 카테고리
`FASTFOOD`(패스트푸드) `KOREAN`(한식) `STEW_SOUP`(찜·탕) `MEAT`(고기) `ASIAN`(아시안) `CHINESE`(중식) `WESTERN`(양식) `JAPANESE`(일식)

### Restriction — 알레르겐 (추천 1단계 하드필터)
`EGG`(계란) `MILK`(우유) `WHEAT`(밀) `SOY`(대두) `NUT`(견과) `CRUSTACEAN`(갑각류) `SEAFOOD`(해산물)

### Choice — 투표 호/불호
`LIKE`, `DISLIKE`

### VoteStatus — 투표 단계
`RECOMMENDING`(선호 수집) → `VOTING`(호/불호 투표) → `CLOSED`(종료)

---

## 공통 오류 응답
```json
{ "timestamp": "2026-06-24T16:20:00.123", "status": 404, "error": "Not Found", "message": "투표가 존재하지 않습니다. id=99" }
```
| 상태 | 상황 |
|------|------|
| `400` | 잘못된 입력 (예: 후보에 없는 메뉴에 투표) |
| `401` | 로그인 필요 / 토큰 무효 |
| `403` | 그룹 멤버 아님 |
| `404` | 리소스 없음 |
| `409` | 잘못된 단계 (추천 전 투표, 마감된 투표, 이미 가입한 그룹 등) |

---

## 1. 인증 (Auth)

### 1-1. 카카오 인가 URL 조회 (선택)
**GET** `/api/auth/kakao/login-url`
```json
{ "url": "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=...&redirect_uri=..." }
```

### 1-2. 카카오 로그인 → JWT 발급
**POST** `/api/auth/kakao`

**Request**
```json
{ "code": "카카오에서 받은 인가코드" }
```
**Response 200**
```json
{ "token": "eyJhbGciOiJ...", "member": { "id": 1, "name": "홍길동" } }
```

### 1-3. 내 정보 🔒
**GET** `/api/auth/me` — `Authorization: Bearer` 필요
```json
{ "id": 1, "name": "홍길동" }
```

### 1-4. 로그아웃 🔒
**POST** `/api/auth/logout` — 카카오 토큰 만료 처리. JWT는 클라이언트가 폐기.
- Response 200 (본문 없음)

---

## 2. 그룹 (Group) 🔒 — 전부 로그인 필요

### 2-1. 그룹 생성
**POST** `/api/groups`
```json
{ "name": "점심팟", "description": "회사 점심 모임" }
```
**Response 200** (생성자는 OWNER로 자동 가입, 초대코드 발급)
```json
{ "id": 1, "name": "점심팟", "description": "회사 점심 모임", "inviteCode": "A1B2C3D4", "ownerId": 1 }
```

### 2-2. 내 그룹 목록
**GET** `/api/groups`
```json
[ { "id": 1, "name": "점심팟", "description": "...", "inviteCode": "A1B2C3D4", "ownerId": 1 } ]
```

### 2-3. 초대코드로 참여
**POST** `/api/groups/join`
```json
{ "inviteCode": "A1B2C3D4" }
```
**Response 200** — 참여한 그룹 (`GroupResponse`)
**오류**: 코드에 해당 그룹 없음 → `404` / 이미 가입 → `409`

### 2-4. 그룹원 목록 (그룹 멤버만)
**GET** `/api/groups/{groupId}/members`
```json
[ { "memberId": 1, "name": "홍길동", "role": "OWNER" }, { "memberId": 2, "name": "영희", "role": "MEMBER" } ]
```

---

## 3. 메뉴 (Menu)

### 3-1. 메뉴 전체 조회
**GET** `/api/menus`
```json
[ { "id": 1, "name": "간장치킨", "cuisine": "FASTFOOD", "restrictions": ["WHEAT", "SOY"] } ]
```

### 3-2. 메뉴 등록
**POST** `/api/menus` — body `{ name, cuisine, restrictions[] }`
> 메뉴 166개는 앱 시작 시 `menu-data.csv`로 자동 적재됨. 이 API는 추가용.

---

## 4. 투표 생성

### 4-1. 그룹에 투표 생성
**POST** `/api/groups/{groupId}/votes`
```json
{ "title": "오늘 점심 뭐 먹지", "deadline": "2026-06-30T12:00:00" }
```
**Response 200** — status `RECOMMENDING`인 투표. **오류**: 그룹 없음 → `404`.

---

## 5. 투표 진행 (②~⑤) 🔒 — 로그인 + 그룹 멤버 필요

순서: **② 선호 → ③ 추천 → ④ 호/불호 → ⑤ 집계**. 각 단계는 투표 `status`가 맞아야 함(아니면 `409`).

### 5-1. ② 선호 제출
**POST** `/api/votes/{voteId}/preferences` *(status `RECOMMENDING`)*
```json
{ "dislikedCuisines": ["CHINESE"], "restrictions": ["NUT"] }
```
- 같은 사람이 다시 내면 덮어씀. Response 200(본문 없음).

### 5-2. ③ 추천 실행
**POST** `/api/votes/{voteId}/recommend` *(status `RECOMMENDING` → `VOTING`)* — body 없음
```json
{
  "candidates": [
    { "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" },
    { "menuId": 31, "name": "스테이크", "cuisine": "MEAT" },
    { "menuId": 12, "name": "불고기", "cuisine": "KOREAN" }
  ],
  "relaxed": false, "relaxedCuisines": [], "impossible": false
}
```
- `relaxed`: 비선호 카테고리 완화 발생 / `relaxedCuisines`: 되살린 카테고리 / `impossible`: 알레르겐만으로 후보 0
- **오류**: 제출된 선호 없음 → `409`

### 5-3. ④ 호/불호 투표
**POST** `/api/votes/{voteId}/ballots` *(status `VOTING`)*
```json
{ "choices": { "30": "LIKE", "31": "DISLIKE", "12": "LIKE" } }
```
- 다시 내면 그 사람 표 덮어씀. **오류**: 후보 아닌 메뉴 → `400`.

### 5-4. ⑤ 집계 → 최종 메뉴
**POST** `/api/votes/{voteId}/result` *(status `VOTING` → `CLOSED`)* — body 없음
```json
{ "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" }
```
- 선정 규칙: ① 호 최다 → ② 동점 시 불호 최소 → ③ 그래도 동점 시 `호-불호` 최대.

### 5-5. 투표 상태/결과 조회
**GET** `/api/votes/{voteId}`
```json
{
  "voteId": 1, "title": "오늘 점심 뭐 먹지", "status": "CLOSED",
  "candidates": [ { "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" } ],
  "resultMenu": { "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" },
  "relaxed": false, "relaxedCuisines": [], "impossible": false
}
```
- `candidates`: 추천 전이면 `[]`, `resultMenu`: 집계 전이면 `null`.

### 5-6. 미제출 그룹원 조회
**GET** `/api/votes/{voteId}/pending-members`
```json
[ { "memberId": 2, "name": "영희" } ]
```

---

## 6. 식당 검색

### 6-1. 메뉴명으로 주변 식당 검색
**GET** `/api/restaurants?menu={메뉴명}&page={페이지}` (page 기본 1)
- 카카오 로컬 API 응답(`KakaoLocalResponseDto`)을 그대로 반환합니다.

---

## 전체 흐름 예시
```
[로그인] POST /api/auth/kakao {code}              → JWT 획득
[방장]   POST /api/groups {name}                  → 그룹 생성 (초대코드)
[멤버]   POST /api/groups/join {inviteCode}        → 그룹 참여
[방장]   POST /api/groups/1/votes {title}          → 투표 생성
[전원]   POST /api/votes/1/preferences {...}       → 선호 제출
[방장]   POST /api/votes/1/recommend               → 후보 3개
[전원]   POST /api/votes/1/ballots {choices}       → 호/불호
[방장]   POST /api/votes/1/result                  → 최종 메뉴
```

### curl 예시
```bash
# 1) 로그인 → 토큰
curl -X POST https://renaissance-production-f569.up.railway.app/api/auth/kakao \
  -H "Content-Type: application/json" -d '{"code":"<카카오 인가코드>"}'

# 2) 이후 요청은 토큰 헤더
TOKEN=eyJhbGciOiJ...
curl -X POST https://renaissance-production-f569.up.railway.app/api/groups \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"점심팟","description":"회사 점심"}'

curl -X POST https://renaissance-production-f569.up.railway.app/api/votes/1/recommend \
  -H "Authorization: Bearer $TOKEN"
```

---

## 운영 환경변수 (Railway)
| 변수 | 설명 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | PostgreSQL 접속 |
| `KAKAO_CLIENT_ID` / `KAKAO_SECRET_ID` | 카카오 앱 키 |
| `KAKAO_REDIRECT_URI` | 프론트의 code 수신 페이지 (카카오 콘솔과 일치) |
| `JWT_SECRET` | JWT 서명 키 (32바이트 이상) |
| `JWT_EXPIRATION_MS` | 토큰 만료(ms), 기본 86400000 |
| `CORS_ALLOWED_ORIGINS` | 프론트 출처 (쉼표로 여러 개) |

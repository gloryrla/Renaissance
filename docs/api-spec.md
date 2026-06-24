# Renaissance API 명세서

그룹 외식 메뉴 추천·투표 백엔드 REST API 명세입니다.

- **Base URL (운영)**: `https://renaissance-production-f569.up.railway.app`
- **Base URL (로컬)**: `http://localhost:8080`
- 요청/응답 형식: `application/json`, 인코딩 `UTF-8`
- enum은 JSON에서 `name` 문자열로 주고받습니다. 예: `"KOREAN"`, `"NUT"`, `"LIKE"`.

---

## 인증 (중요)

카카오 로그인 **세션** 기반입니다. 로그인하면 서버 세션에 `memberId`가 저장되고, 응답으로 **세션 쿠키(`JSESSIONID`)** 가 내려옵니다.

1. 브라우저에서 `GET /login` → 카카오 로그인 → `GET /callback`에서 세션 생성
2. 이후 `/api/votes/**` 호출 시 그 **세션 쿠키를 함께 보내야** 합니다.

- 로그인 안 된 상태로 `/api/votes/**` 호출 → **401**
- 투표가 속한 그룹의 멤버가 아니면 → **403**
- 행위자(누가 선호/투표를 내는지)는 **요청 바디가 아니라 세션에서** 결정됩니다. 따라서 요청 바디의 `memberId`는 **무시**됩니다(생략 가능).

> Postman 테스트 시: 브라우저로 먼저 로그인 → 개발자도구에서 `JSESSIONID` 쿠키 값을 복사해 Postman 헤더 `Cookie: JSESSIONID=...`에 넣으세요.

---

## 공통 Enum

### Cuisine — 음식 카테고리
| name | label |
|------|-------|
| `FASTFOOD` | 패스트푸드 |
| `KOREAN` | 한식 |
| `STEW_SOUP` | 찜·탕 |
| `MEAT` | 고기 |
| `ASIAN` | 아시안 |
| `CHINESE` | 중식 |
| `WESTERN` | 양식 |
| `JAPANESE` | 일식 |

### Restriction — 알레르겐 (추천 1단계 하드필터)
| name | label |
|------|-------|
| `EGG` | 계란 |
| `MILK` | 우유 |
| `WHEAT` | 밀 |
| `SOY` | 대두 |
| `NUT` | 견과 |
| `CRUSTACEAN` | 갑각류 |
| `SEAFOOD` | 해산물 |

### Choice — 투표 호/불호
`LIKE`, `DISLIKE`

### VoteStatus — 투표 진행 단계
| name | 의미 |
|------|------|
| `RECOMMENDING` | 선호 수집 중 (추천 전) |
| `VOTING` | 후보 확정, 호/불호 투표 중 |
| `CLOSED` | 집계 완료, 최종 메뉴 확정 |

---

## 공통 오류 응답

도메인 예외는 아래 형식의 JSON으로 반환됩니다.

```json
{
  "timestamp": "2026-06-24T16:20:00.123",
  "status": 404,
  "error": "Not Found",
  "message": "투표가 존재하지 않습니다. id=99"
}
```

| 상태 | 발생 상황 |
|------|-----------|
| `400` | 잘못된 입력 (예: 후보에 없는 메뉴에 투표) |
| `401` | 로그인 필요 |
| `403` | 그룹 멤버가 아님 |
| `404` | 리소스 없음 (투표/멤버/메뉴) |
| `409` | 잘못된 단계에서 호출 (예: 추천 전에 투표, 마감된 투표) |

---

## 1. 메뉴 (Menu)

### 1-1. 메뉴 전체 조회
**GET** `/api/menus`

**Response 200**
```json
[
  { "id": 1, "name": "간장치킨", "cuisine": "FASTFOOD", "restrictions": ["WHEAT", "SOY"] },
  { "id": 2, "name": "감자튀김", "cuisine": "FASTFOOD", "restrictions": ["WHEAT"] }
]
```

### 1-2. 메뉴 등록
**POST** `/api/menus`

**Request Body** (`MenuRequest`)
| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | string | 메뉴 이름 |
| `cuisine` | Cuisine | 카테고리 |
| `restrictions` | Restriction[] | 알레르겐 (없으면 `[]`) |

```json
{ "name": "김밥", "cuisine": "KOREAN", "restrictions": [] }
```

**Response 200** — 저장된 메뉴
```json
{ "id": 167, "name": "김밥", "cuisine": "KOREAN", "restrictions": [] }
```

> 메뉴 166개는 앱 시작 시 `menu-data.csv`로 자동 적재됩니다. 이 API는 추가 등록용입니다.

---

## 2. 투표 생성

### 2-1. 그룹에 투표 생성
**POST** `/api/groups/{groupId}/votes`

**Path Variable**
| 이름 | 타입 | 설명 |
|------|------|------|
| `groupId` | long | 대상 그룹 id |

**Request Body** (`VoteCreateRequest`)
| 필드 | 타입 | 설명 |
|------|------|------|
| `title` | string | 투표 제목 |
| `deadline` | datetime(ISO-8601) | 마감 시각 (선택, `null` 가능) |

```json
{ "title": "오늘 점심 뭐 먹지", "deadline": "2026-06-30T12:00:00" }
```

**Response 200** — 생성된 투표 (상태 `RECOMMENDING`)
```json
{
  "id": 1,
  "title": "오늘 점심 뭐 먹지",
  "deadline": "2026-06-30T12:00:00",
  "status": "RECOMMENDING",
  "resultMenu": null,
  "relaxed": false,
  "impossible": false,
  "relaxedCuisines": []
}
```

**오류**: 그룹 없음 → `404`

---

## 3. 투표 진행 (②~⑤) — 로그인 + 그룹 멤버 필요

진행 순서: **② 선호 제출 → ③ 추천 → ④ 호/불호 투표 → ⑤ 집계**.
각 단계는 투표의 `status`가 맞아야 호출됩니다(아니면 `409`).

### 3-1. ② 선호 제출
**POST** `/api/votes/{voteId}/preferences` *(status: `RECOMMENDING`)*

같은 사람이 다시 내면 덮어씁니다.

**Request Body** (`MemberPreferenceRequest`) — `memberId`는 무시(세션 사용)
| 필드 | 타입 | 설명 |
|------|------|------|
| `dislikedCuisines` | Cuisine[] | 오늘 안 당기는 카테고리 (소프트) |
| `restrictions` | Restriction[] | 알레르겐 (하드, 절대 완화 안 됨) |

```json
{ "dislikedCuisines": ["CHINESE"], "restrictions": ["NUT"] }
```

**Response 200** — 본문 없음

---

### 3-2. ③ 추천 실행
**POST** `/api/votes/{voteId}/recommend` *(status: `RECOMMENDING` → `VOTING`)*

제출된 모든 선호를 모아 하드필터(알레르겐 → 비선호 카테고리)를 적용하고 후보 최대 3개를 **저장**합니다. 후보가 부족하면 비선호 카테고리를 단계적으로 완화합니다.

**Request Body** 없음

**Response 200** (`RecommendResultResponse`)
```json
{
  "candidates": [
    { "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" },
    { "menuId": 31, "name": "스테이크", "cuisine": "MEAT" },
    { "menuId": 12, "name": "불고기", "cuisine": "KOREAN" }
  ],
  "relaxed": false,
  "relaxedCuisines": [],
  "impossible": false
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `candidates` | {menuId,name,cuisine}[] | 후보 (최대 3개) |
| `relaxed` | boolean | 비선호 카테고리 완화 발생 여부 |
| `relaxedCuisines` | Cuisine[] | 완화로 되살린 카테고리 |
| `impossible` | boolean | 알레르겐만으로 후보를 못 만든 경우 |

**오류**: 제출된 선호가 없음 → `409`

---

### 3-3. ④ 호/불호 투표
**POST** `/api/votes/{voteId}/ballots` *(status: `VOTING`)*

같은 사람이 다시 내면 그 사람 표를 덮어씁니다.

**Request Body** (`BallotRequest`) — `memberId`는 무시(세션 사용)
| 필드 | 타입 | 설명 |
|------|------|------|
| `choices` | object | `{ "menuId": "LIKE"\|"DISLIKE" }` (후보 메뉴에 대해서만) |

```json
{ "choices": { "30": "LIKE", "31": "DISLIKE", "12": "LIKE" } }
```

**Response 200** — 본문 없음

**오류**: 후보에 없는 메뉴 id → `400`

---

### 3-4. ⑤ 집계 → 최종 메뉴
**POST** `/api/votes/{voteId}/result` *(status: `VOTING` → `CLOSED`)*

**선정 규칙**: ① 호(LIKE) 최다 → ② 동점이면 불호(DISLIKE) 최소 → ③ 그래도 동점이면 `호-불호` 최대.

**Request Body** 없음

**Response 200** (`CandidateMenuResponse`) — 최종 메뉴
```json
{ "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" }
```

---

### 3-5. 투표 상태/결과 조회
**GET** `/api/votes/{voteId}`

**Response 200** (`VoteDetailResponse`)
```json
{
  "voteId": 1,
  "title": "오늘 점심 뭐 먹지",
  "status": "CLOSED",
  "candidates": [
    { "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" },
    { "menuId": 31, "name": "스테이크", "cuisine": "MEAT" },
    { "menuId": 12, "name": "불고기", "cuisine": "KOREAN" }
  ],
  "resultMenu": { "menuId": 30, "name": "곱창구이", "cuisine": "MEAT" },
  "relaxed": false,
  "relaxedCuisines": [],
  "impossible": false
}
```
> `candidates`는 추천 전이면 `[]`, `resultMenu`는 집계 전이면 `null`.

---

### 3-6. 아직 선호를 안 낸 그룹원 조회
**GET** `/api/votes/{voteId}/pending-members`

추천 전에 누구를 기다려야 하는지 확인용.

**Response 200** (`MemberSummaryResponse[]`)
```json
[
  { "memberId": 2, "name": "영희" },
  { "memberId": 3, "name": "동길" }
]
```

---

## 전체 흐름 예시

```
[방장] POST /api/groups/1/votes              투표 생성 (RECOMMENDING)
[전원] POST /api/votes/1/preferences         각자 선호 제출
       GET  /api/votes/1/pending-members     미제출자 확인
[방장] POST /api/votes/1/recommend           후보 3개 확정 (VOTING)
[전원] POST /api/votes/1/ballots             후보에 호/불호
[방장] POST /api/votes/1/result              집계 → 최종 메뉴 (CLOSED)
       GET  /api/votes/1                      결과 조회
```

### curl 예시 (세션 쿠키 필요)
```bash
# 선호 제출
curl -X POST https://renaissance-production-f569.up.railway.app/api/votes/1/preferences \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<로그인후_쿠키>" \
  -d '{"dislikedCuisines":["CHINESE"],"restrictions":["NUT"]}'

# 추천
curl -X POST https://renaissance-production-f569.up.railway.app/api/votes/1/recommend \
  -H "Cookie: JSESSIONID=<로그인후_쿠키>"

# 호/불호
curl -X POST https://renaissance-production-f569.up.railway.app/api/votes/1/ballots \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<로그인후_쿠키>" \
  -d '{"choices":{"30":"LIKE","31":"DISLIKE","12":"LIKE"}}'

# 집계
curl -X POST https://renaissance-production-f569.up.railway.app/api/votes/1/result \
  -H "Cookie: JSESSIONID=<로그인후_쿠키>"
```

---

## 참고: 화면(MVC) 엔드포인트

아래는 HTML을 렌더링하는 서버사이드 화면이며 위 REST API와 별개입니다.

| 경로 | 설명 |
|------|------|
| `GET /login` | 카카오 로그인 페이지 |
| `GET /callback` | 카카오 인가 콜백 (세션 생성) |
| `GET /group/new`, `POST /group/new` | 그룹 생성 |
| `GET /group/list` | 내 그룹 목록 |
| `GET /group/in`, `POST /group/in` | 초대코드로 그룹 참여 |
| `GET /group/{id}/members` | 그룹원 목록 |
| `GET /restaurant/list?menu=&page=` | 메뉴명으로 식당 검색 (카카오 로컬) |

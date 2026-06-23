# project1 API 명세서

그룹 외식 메뉴 추천·투표 기능의 REST API 명세입니다.

- Base URL: `http://localhost:8080`
- 요청/응답 형식: `application/json`
- 인코딩: `UTF-8`

> 참고: 그룹 생성/참여, 카카오 로그인 등은 서버사이드 MVC(HTML 렌더링) 화면이며 본 문서의 REST API와 별개입니다.

---

## 공통 Enum

### Cuisine (음식 카테고리)
| name | label |
|------|-------|
| `FASTFOOD` | 패스트푸드 |
| `KOREAN` | 한식 |
| `STEW_SOUP` | 찜·탕 |
| `MEAT` | 고기 |
| `ASIAN` | 아시안 |
| `CHINESE` | 중식 |
| `WESTERN` | 양식 |
| `JAPANSES` | 일식 |

### Restriction (알레르기·식이 제한)
| name | label |
|------|-------|
| `NUTS` | 견과류 |
| `CRUSTACEAN` | 갑각류 |
| `DAIRY` | 유제품 |
| `GLUTEN` | 글루텐 |
| `EGG` | 계란 |
| `PORK` | 돼지고기 |
| `BEEF` | 소고기 |

### Choice (투표 호/불호)
`LIKE`, `DISLIKE`

> JSON에서는 enum의 `name` 문자열을 사용합니다. 예: `"KOREAN"`, `"NUTS"`, `"LIKE"`.

---

## 1. 메뉴 (Menu)

### 1-1. 메뉴 전체 조회
- **GET** `/api/menus`

**Response 200**
```json
[
  {
    "id": 1,
    "name": "김치찌개",
    "cuisine": "KOREAN",
    "restrictions": ["PORK"]
  },
  {
    "id": 2,
    "name": "마르게리타 피자",
    "cuisine": "WESTERN",
    "restrictions": ["DAIRY", "GLUTEN"]
  }
]
```

### 1-2. 메뉴 등록 (시드)
- **POST** `/api/menus`

**Request Body** (`MenuRequest`)
| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | string | 메뉴 이름 |
| `cuisine` | Cuisine | 음식 카테고리 |
| `restrictions` | Restriction[] | 포함된 제한 성분 (없으면 `[]`) |

```json
{
  "name": "김치찌개",
  "cuisine": "KOREAN",
  "restrictions": ["PORK"]
}
```

**Response 200** — 저장된 메뉴
```json
{
  "id": 1,
  "name": "김치찌개",
  "cuisine": "KOREAN",
  "restrictions": ["PORK"]
}
```

---

## 2. 투표 (Vote)

### 2-1. 그룹 투표 생성
- **POST** `/api/groups/{groupId}/votes`

**Path Variable**
| 이름 | 타입 | 설명 |
|------|------|------|
| `groupId` | long | 대상 그룹 id |

**Request Body** (`VoteCreateRequest`)
| 필드 | 타입 | 설명 |
|------|------|------|
| `title` | string | 투표 제목 |
| `deadline` | datetime(ISO-8601) | 마감 시각 |

```json
{
  "title": "오늘 점심 뭐 먹지",
  "deadline": "2026-06-18T12:00:00"
}
```

**Response 200** — 생성된 투표
```json
{
  "id": 1,
  "title": "오늘 점심 뭐 먹지",
  "deadline": "2026-06-18T12:00:00",
  "group": { "id": 1, "name": "점심팟", "...": "..." }
}
```

**오류**
- `groupId`에 해당하는 그룹이 없으면 `IllegalArgumentException` → `500` (그룹이 존재하지 않습니다).

---

### 2-2. 후보 메뉴 추천 (1~3단계)
- **POST** `/api/recommend`

그룹원들의 선호도를 받아 **하드필터(알레르기 → 카테고리 비선호)** 를 적용한 뒤 후보 메뉴 최대 3개를 반환합니다.
한 명이라도 제한이 걸린 메뉴는 후보에서 제외됩니다. 후보가 부족하면 비선호 카테고리를 단계적으로 완화합니다.

**Request Body** (`RecommendRequest`)
```json
{
  "members": [
    {
      "memberId": 1,
      "dislikedCuisines": ["JAPANSES"],
      "restrictions": ["NUTS"]
    },
    {
      "memberId": 2,
      "dislikedCuisines": [],
      "restrictions": ["DAIRY"]
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `members[].memberId` | long | 그룹원 id |
| `members[].dislikedCuisines` | Cuisine[] | 오늘 안 당기는 카테고리 (소프트, 후보 부족 시 완화) |
| `members[].restrictions` | Restriction[] | 알레르기·식이 제한 (하드, 절대 완화 안 함) |

**Response 200** (`CandidateResult`)
```json
{
  "candidates": [
    { "id": 1, "name": "김치찌개", "cuisine": "KOREAN", "restrictions": ["PORK"] },
    { "id": 5, "name": "비빔밥", "cuisine": "KOREAN", "restrictions": [] },
    { "id": 8, "name": "쌀국수", "cuisine": "ASIAN", "restrictions": [] }
  ],
  "relaxed": false,
  "relaxedCuisines": [],
  "impossible": false
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `candidates` | Menu[] | 최종 후보 (최대 3개, 부족하면 그 이하) |
| `relaxed` | boolean | 카테고리 비선호 완화가 발생했는지 |
| `relaxedCuisines` | Cuisine[] | 완화로 다시 허용된 카테고리 |
| `impossible` | boolean | 알레르기 제한만으로 후보를 못 만든 경우 `true` |

---

### 2-3. 호/불호 집계 → 최종 메뉴 (4~5단계)
- **POST** `/api/decide`

후보 메뉴에 대한 그룹원들의 호/불호 투표를 집계해 최종 1개를 선정합니다.

**선정 규칙**: ① 호(LIKE) 최다 → ② 동점이면 불호(DISLIKE) 최소 → ③ 그래도 동점이면 `호-불호` 최대.

**Request Body** (`DecideRequest`)
```json
{
  "candidateMenuIds": [1, 5, 8],
  "votes": {
    "1": { "1": "LIKE",    "5": "DISLIKE", "8": "LIKE" },
    "2": { "1": "LIKE",    "5": "LIKE",    "8": "DISLIKE" }
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `candidateMenuIds` | long[] | 후보 메뉴 id 목록 |
| `votes` | object | `{ "memberId": { "menuId": "LIKE"\|"DISLIKE" } }` |

**Response 200** — 최종 선정 메뉴 (`Menu`)
```json
{
  "id": 1,
  "name": "김치찌개",
  "cuisine": "KOREAN",
  "restrictions": ["PORK"]
}
```

**오류**
- `candidateMenuIds`가 비어 후보가 없으면 `IllegalArgumentException` → `500` (후보 메뉴가 없습니다).

---

## 전체 흐름 예시

```
1) POST /api/menus           메뉴 데이터 시드 (여러 번)
2) GET  /api/menus           등록된 메뉴 확인
3) POST /api/groups/1/votes  그룹 투표 생성
4) POST /api/recommend       그룹원 선호도 → 후보 3개
5) POST /api/decide          후보에 대한 호/불호 → 최종 메뉴 1개
```

### curl 예시
```bash
# 메뉴 등록
curl -X POST http://localhost:8080/api/menus \
  -H "Content-Type: application/json" \
  -d '{"name":"김치찌개","cuisine":"KOREAN","restrictions":["PORK"]}'

# 후보 추천
curl -X POST http://localhost:8080/api/recommend \
  -H "Content-Type: application/json" \
  -d '{"members":[{"memberId":1,"dislikedCuisines":["JAPANSES"],"restrictions":["NUTS"]}]}'

# 최종 결정
curl -X POST http://localhost:8080/api/decide \
  -H "Content-Type: application/json" \
  -d '{"candidateMenuIds":[1,5,8],"votes":{"1":{"1":"LIKE","5":"DISLIKE","8":"LIKE"}}}'
```

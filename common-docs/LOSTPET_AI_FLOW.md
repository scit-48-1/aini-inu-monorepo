# 실종 반려동물 AI 매칭 시스템 - 데이터 흐름 완전 해부

## 목차

1. [전체 흐름 한눈에 보기](#1-전체-흐름-한눈에-보기)
2. [핵심 개념: 임베딩이란?](#2-핵심-개념-임베딩이란)
3. [STEP 1: 목격 제보 등록 (벡터 저장)](#3-step-1-목격-제보-등록--벡터-저장)
4. [STEP 2: 실종 신고 & AI 분석 (벡터 검색)](#4-step-2-실종-신고--ai-분석--벡터-검색)
5. [STEP 3: 후보 스코어링 (점수 매기기)](#5-step-3-후보-스코어링--점수-매기기)
6. [STEP 4: 매칭 승인 & 채팅 연결](#6-step-4-매칭-승인--채팅-연결)
7. [서비스 간 통신 정리](#7-서비스-간-통신-정리)
8. [PgVector 테이블 구조](#8-pgvector-테이블-구조)
9. [설정값 요약](#9-설정값-요약)
10. [부록 A: 왜 MySQL이 아닌 PostgreSQL + pgvector인가?](#부록-a-왜-mysql이-아닌-postgresql--pgvector인가)
11. [부록 B: 자주 하는 질문](#부록-b-자주-하는-질문)
12. [부록 C: 트러블슈팅 - Document ID UUID 이슈](#부록-c-트러블슈팅---document-id-uuid-이슈)
13. [부록 D: 트러블슈팅 - 임베딩 차원 불일치 및 트랜잭션 오염](#부록-d-트러블슈팅---임베딩-차원-불일치-및-트랜잭션-오염)

---

## 1. 전체 흐름 한눈에 보기

```
[목격자] ──제보등록──▶ [우리 서버] ──텍스트──▶ [Gemini API] ──벡터──▶ [PgVector DB]
                            │                                              │
                            │                                              │
[실종신고 주인] ──분석요청──▶ [우리 서버] ──텍스트──▶ [Gemini API] ──벡터──▶ │
                            │                                              │
                            │◀───────── 유사도 높은 제보 목록 ──────────────┘
                            │
                            ▼
                     [스코어링 엔진]
                     (유사도 70% + 위치 20% + 시간 10%)
                            │
                            ▼
                     [후보 목록 반환] ──승인──▶ [채팅방 생성]
```

---

## 2. 핵심 개념: 임베딩이란?

### 사람의 언어를 숫자로 바꾸는 것

Gemini 같은 AI 모델은 텍스트를 **768개의 숫자 배열(벡터)** 로 변환합니다. 이것을 **임베딩(embedding)** 이라고 합니다.

```
"갈색 푸들, 빨간 목줄, 강남역 근처에서 발견"
    │
    ▼  (Gemini API가 변환)
    │
[0.0231, -0.0892, 0.1547, ..., 0.0034]   ← 768개의 숫자
```

**왜 이렇게 하나요?**

의미가 비슷한 문장은 비슷한 숫자 배열을 갖게 됩니다:

```
"갈색 푸들, 빨간 목줄"  →  [0.02, -0.09, 0.15, ...]
"브라운 푸들, 레드 칼라"  →  [0.03, -0.08, 0.14, ...]  ← 매우 비슷!
"검정 시바견, 파란 하네스" →  [0.51, 0.33, -0.22, ...]  ← 매우 다름!
```

두 벡터 사이의 거리(코사인 유사도)를 계산하면 "의미적으로 얼마나 비슷한지"를 수치로 알 수 있습니다.

---

## 3. STEP 1: 목격 제보 등록 (벡터 저장)

누군가 길에서 동물을 발견하면 **제보(Sighting)** 를 등록합니다. 이때 AI 인덱싱이 일어납니다.

### 3-1. 사용자가 보내는 데이터

```
POST /api/v1/sightings

{
  "photoUrl": "https://storage.example.com/found-dog.jpg",
  "foundAt": "2026-03-08T15:30:00",
  "foundLocation": "서울 강남구 역삼동 823-1",
  "memo": "갈색 소형 푸들, 빨간 목줄 착용, 겁먹은 상태"
}
```

### 3-2. 우리 서버가 하는 일

```
SightingController.create()
    │
    ▼
SightingService.create()
    │
    ├──▶ ① DB에 Sighting 엔티티 저장 (PostgreSQL)
    │
    └──▶ ② LostPetAiClient.indexSighting() 호출
              │
              ▼
         텍스트 조합 (Document 생성)
```

### 3-3. Document 생성 과정

우리 서버는 제보 데이터를 하나의 텍스트 문서로 조합합니다:

```java
// LostPetAiClientImpl.toSightingDocument() 에서 실행되는 로직

content = photoUrl + " " + foundLocation + " " + memo + " " + foundAt
// 결과: "https://...found-dog.jpg 서울 강남구 역삼동 823-1 갈색 소형 푸들, 빨간 목줄 착용, 겁먹은 상태 2026-03-08T15:30:00"

metadata = {
    "sightingId": 42,
    "finderId": 7,
    "foundLocation": "서울 강남구 역삼동 823-1",
    "foundAt": "2026-03-08T15:30:00",
    "photoUrl": "https://...found-dog.jpg",
    "memo": "갈색 소형 푸들, 빨간 목줄 착용, 겁먹은 상태"
}
```

### 3-4. Gemini API와의 통신

Spring AI의 `VectorStore.add()` 를 호출하면, 내부적으로 이런 일이 일어납니다:

```
[우리 서버]                         [Google Gemini API]
    │                                     │
    │  HTTP POST                          │
    │  https://generativelanguage.        │
    │  googleapis.com/v1beta/             │
    │  models/gemini-embedding-001:       │
    │  embedContent                       │
    │                                     │
    │  Request Body:                      │
    │  {                                  │
    │    "content": {                     │
    │      "parts": [{                    │
    │        "text": "https://...         │
    │         found-dog.jpg 서울 강남구    │
    │         역삼동 823-1 갈색 소형       │
    │         푸들..."                     │
    │      }]                             │
    │    }                                │
    │  }                                  │
    │ ─────────────────────────────────▶  │
    │                                     │
    │  Response Body:                     │
    │  {                                  │
    │    "embedding": {                   │
    │      "values": [                    │
    │        0.0231, -0.0892, 0.1547,     │
    │        ...(총 768개)...,            │
    │        0.0034                       │
    │      ]                              │
    │    }                                │
    │  }                                  │
    │ ◀─────────────────────────────────  │
    │                                     │
```

**핵심:** 우리 서버가 Gemini에 보내는 것은 **텍스트**이고, 돌려받는 것은 **768개의 숫자 배열**입니다.

### 3-5. PgVector에 저장

Gemini에서 받은 벡터를 PostgreSQL의 pgVector 확장을 통해 저장합니다:

```sql
-- Spring AI가 내부적으로 실행하는 SQL (개념적 표현)

INSERT INTO lostpet_vector_store (id, content, metadata, embedding)
VALUES (
    'uuid-xxx-xxx',
    'https://...found-dog.jpg 서울 강남구 역삼동 823-1 갈색 소형 푸들...',
    '{"sightingId":42, "finderId":7, "foundLocation":"서울 강남구...", ...}',
    '[0.0231, -0.0892, 0.1547, ..., 0.0034]'::vector(768)
);
```

### 전체 요약 다이어그램

```
[사용자 앱]                [우리 서버]              [Gemini API]           [PostgreSQL + PgVector]
    │                          │                        │                         │
    │  POST /sightings         │                        │                         │
    │  {photo, location,       │                        │                         │
    │   memo, foundAt}         │                        │                         │
    │ ────────────────────▶    │                        │                         │
    │                          │                        │                         │
    │                          │ ① Sighting 엔티티 저장  │                         │
    │                          │ ──────────────────────────────────────────────▶  │
    │                          │                        │                         │
    │                          │ ② 텍스트 조합 후        │                         │
    │                          │   임베딩 요청           │                         │
    │                          │ ──────────────────▶    │                         │
    │                          │                        │                         │
    │                          │ ③ 768차원 벡터 수신     │                         │
    │                          │ ◀──────────────────    │                         │
    │                          │                        │                         │
    │                          │ ④ 벡터 + 메타데이터 저장 │                         │
    │                          │ ──────────────────────────────────────────────▶  │
    │                          │                        │                         │
    │  201 Created             │                        │                         │
    │ ◀────────────────────    │                        │                         │
```

---

## 4. STEP 2: 실종 신고 & AI 분석 (벡터 검색)

반려동물 주인이 실종 신고 후 **AI 분석**을 요청하면, 기존 제보들 중 유사한 것을 찾습니다.

### 4-1. 사용자가 보내는 데이터

```
POST /api/v1/lost-pets/analyze

{
  "lostPetId": 15,
  "imageUrl": "https://storage.example.com/my-dog.jpg",
  "mode": "LOST",
  "queryText": "갈색 소형 푸들, 이름은 초코, 빨간 목줄",
  "latitude": 37.4979,
  "longitude": 127.0276
}
```

### 4-2. 벡터 검색 과정

```
[우리 서버]                         [Gemini API]
    │                                     │
    │  ① 검색 쿼리 텍스트 조합              │
    │  "LOST https://...my-dog.jpg        │
    │   갈색 소형 푸들, 이름은 초코,         │
    │   빨간 목줄 37.497900 127.027600"    │
    │                                     │
    │  ② 이 텍스트로 임베딩 요청             │
    │  POST .../gemini-embedding-001:     │
    │  embedContent                       │
    │ ─────────────────────────────────▶  │
    │                                     │
    │  ③ 검색용 벡터 수신                   │
    │  [0.0198, -0.0901, 0.1523, ...]     │
    │ ◀─────────────────────────────────  │
    │                                     │
```

### 4-3. PgVector 유사도 검색

받은 벡터로 DB에서 가장 비슷한 제보를 찾습니다:

```sql
-- Spring AI VectorStore.similaritySearch()가 내부적으로 실행하는 SQL (개념적 표현)

SELECT id, content, metadata, embedding,
       1 - (embedding <=> '[0.0198, -0.0901, 0.1523, ...]'::vector) AS similarity
FROM lostpet_vector_store
ORDER BY embedding <=> '[0.0198, -0.0901, 0.1523, ...]'::vector
LIMIT 50;
```

**`<=>`는 코사인 거리 연산자입니다.**

이것이 바로 벡터 검색의 핵심입니다:
- `저장된 제보 벡터`와 `검색 쿼리 벡터` 사이의 **코사인 거리**를 계산
- 거리가 가까울수록 (= 유사도가 높을수록) 의미가 비슷한 제보
- HNSW 인덱스 덕분에 모든 벡터를 일일이 비교하지 않고도 빠르게 검색 가능

### 4-4. 검색 결과 예시

```
┌──────────────────────────────────────────────────────────────────────┐
│                    PgVector 유사도 검색 결과 (상위 50건)                │
├──────┬─────────────────────────────────┬───────────┬────────────────┤
│ 순위 │ 제보 내용 (content 요약)          │ 유사도    │ sightingId    │
├──────┼─────────────────────────────────┼───────────┼────────────────┤
│  1   │ 갈색 소형 푸들, 빨간 목줄, 역삼동  │ 0.92     │ 42            │
│  2   │ 브라운 토이푸들, 강남역 근처       │ 0.87     │ 38            │
│  3   │ 갈색 작은 개, 목줄 있음, 선릉역    │ 0.81     │ 55            │
│  4   │ 하얀 말티즈, 파란 하네스          │ 0.43     │ 21            │
│ ...  │ ...                             │ ...      │ ...           │
│  50  │ 검정 리트리버, 목줄 없음          │ 0.12     │ 5             │
└──────┴─────────────────────────────────┴───────────┴────────────────┘
```

"갈색 소형 푸들, 빨간 목줄"이라는 실종 신고에 대해:
- "갈색 소형 푸들, 빨간 목줄" 제보 → 유사도 0.92 (매우 유사!)
- "하얀 말티즈, 파란 하네스" 제보 → 유사도 0.43 (별로 안 비슷)
- "검정 리트리버" 제보 → 유사도 0.12 (거의 무관)

---

## 5. STEP 3: 후보 스코어링 (점수 매기기)

AI 벡터 유사도만으로는 부족합니다. **위치**와 **시간**도 고려해야 하니까요.

### 5-1. 3가지 점수 요소

```
┌─────────────────────────────────────────────────────────────────────┐
│                        종합 점수 공식                                 │
│                                                                     │
│   종합점수 = (유사도 × 0.7) + (위치점수 × 0.2) + (시간점수 × 0.1)    │
│                                                                     │
│   가중치:  AI 유사도 70%  +  위치 근접성 20%  +  시간 근접성 10%      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5-2. 각 점수 계산 방법

#### AI 유사도 점수 (70% 가중치)
- Gemini 벡터 검색에서 나온 코사인 유사도 (0.0 ~ 1.0)
- 텍스트의 의미적 유사성을 반영

#### 위치 점수 (20% 가중치)
```
실종 장소: "서울 강남구 역삼동"
목격 장소: "서울 강남구 역삼동 823-1"

→ "역삼동"이 "역삼동 823-1"에 포함됨 → 점수: 0.6

실종 장소: "서울 강남구 역삼동"
목격 장소: "서울 강남구 역삼동"

→ 정확히 일치 → 점수: 1.0

실종 장소: "서울 강남구 역삼동"
목격 장소: "부산 해운대구"

→ 포함 관계 없음 → 점수: 0.2
```

#### 시간 점수 (10% 가중치)
```
실종 시각: 2026-03-08 10:00
목격 시각: 2026-03-08 15:30  (5.5시간 후)

→ 점수 = (72 - 5.5) / 72 = 0.924

실종 시각: 2026-03-08 10:00
목격 시각: 2026-03-10 10:00  (48시간 후)

→ 점수 = (72 - 48) / 72 = 0.333

실종 시각: 2026-03-08 10:00
목격 시각: 2026-03-12 10:00  (96시간 후, 72시간 초과)

→ 72시간 cap 적용 → 점수 = 0.0
```

### 5-3. 최종 스코어링 예시

```
┌────┬───────────┬───────────┬───────────┬───────────┬──────────────┐
│순위│ sightingId│ AI 유사도  │ 위치 점수  │ 시간 점수  │ 종합 점수     │
│    │           │ (×0.7)    │ (×0.2)    │ (×0.1)    │              │
├────┼───────────┼───────────┼───────────┼───────────┼──────────────┤
│ 1  │    42     │ 0.92      │ 0.6       │ 0.924     │ 0.856        │
│    │           │ → 0.644   │ → 0.12    │ → 0.092   │              │
├────┼───────────┼───────────┼───────────┼───────────┼──────────────┤
│ 2  │    38     │ 0.87      │ 1.0       │ 0.500     │ 0.859        │
│    │           │ → 0.609   │ → 0.20    │ → 0.050   │              │
├────┼───────────┼───────────┼───────────┼───────────┼──────────────┤
│ 3  │    55     │ 0.81      │ 0.2       │ 0.800     │ 0.687        │
│    │           │ → 0.567   │ → 0.04    │ → 0.080   │              │
└────┴───────────┴───────────┴───────────┴───────────┴──────────────┘

→ 상위 20건만 잘라서 후보 목록으로 반환
```

---

## 6. STEP 4: 매칭 승인 & 채팅 연결

주인이 후보 목록에서 "이 아이가 내 반려동물이다!"라고 승인하면:

```
[반려동물 주인 앱]           [우리 서버]                     [채팅 서버]
      │                        │                              │
      │ POST /lost-pets/       │                              │
      │   {lostPetId}/match    │                              │
      │ {sessionId,            │                              │
      │  sightingId: 42}       │                              │
      │ ──────────────────▶    │                              │
      │                        │                              │
      │                        │ ① 매칭 검증                   │
      │                        │   - 신고 소유권 확인           │
      │                        │   - 세션 만료 확인             │
      │                        │   - 후보 유효성 확인           │
      │                        │                              │
      │                        │ ② LostPetMatch 생성          │
      │                        │    status: APPROVED           │
      │                        │                              │
      │                        │ ③ 채팅방 생성 요청              │
      │                        │ POST /api/v1/chat-rooms/     │
      │                        │   direct                     │
      │                        │ {partnerId: 7,               │
      │                        │  origin: "LOST_PET",         │
      │                        │  roomTitle: "푸들 초코를       │
      │                        │   찾습니다"}                   │
      │                        │ ────────────────────────▶    │
      │                        │                              │
      │                        │ ④ chatRoomId 수신             │
      │                        │ ◀────────────────────────    │
      │                        │                              │
      │                        │ ⑤ Match status →             │
      │                        │    CHAT_LINKED               │
      │                        │                              │
      │ {matchId, status:      │                              │
      │  CHAT_LINKED,          │                              │
      │  chatRoomId: 99}       │                              │
      │ ◀──────────────────    │                              │
      │                        │                              │
      │   이제 목격자와 채팅 시작!│                              │
```

---

## 7. 서비스 간 통신 정리

### 우리 서버 → Gemini API (나가는 요청)

| 언제? | 무슨 데이터를 보내나? | 무슨 데이터를 받나? |
|-------|---------------------|-------------------|
| 제보 등록 시 | 제보 텍스트 (사진URL + 장소 + 메모 + 시간) | 768차원 벡터 (숫자 배열) |
| AI 분석 요청 시 | 검색 쿼리 텍스트 (모드 + 사진URL + 설명 + 좌표) | 768차원 벡터 (숫자 배열) |

**Gemini에 보내지는 민감 정보:**
- 사진 URL (URL 문자열만 전송, 이미지 자체는 아님)
- 목격 장소 주소
- 사용자가 작성한 메모/설명
- 위치 좌표 (위도/경도)

**Gemini에 보내지지 않는 정보:**
- 사용자 개인정보 (이름, 연락처 등)
- 회원 ID
- 인증 토큰

### 우리 서버 ↔ PgVector DB (내부 통신)

| 언제? | 무슨 SQL이 실행되나? |
|-------|-------------------|
| 제보 등록 시 | INSERT: 텍스트 + 메타데이터 + 벡터를 lostpet_vector_store에 저장 |
| AI 분석 시 | SELECT: 쿼리 벡터와 가장 가까운 벡터 50건을 코사인 거리로 검색 |

### 우리 서버 → 채팅 서버 (나가는 요청)

| 언제? | 무슨 데이터를 보내나? | 무슨 데이터를 받나? |
|-------|---------------------|-------------------|
| 매칭 승인 시 | 목격자 ID, 출처("LOST_PET"), 방 제목 | chatRoomId |

---

## 8. PgVector 테이블 구조

```sql
-- lostpet_vector_store 테이블 (pgVector 확장)

CREATE TABLE lostpet_vector_store (
    id        UUID PRIMARY KEY,           -- 고유 식별자
    content   TEXT,                        -- 원본 텍스트 (사진URL + 장소 + 메모 + 시간)
    metadata  JSON,                        -- 부가 정보 (sightingId, finderId 등)
    embedding VECTOR(768)                  -- Gemini가 생성한 768차원 벡터
);

-- HNSW 인덱스: 대량의 벡터를 빠르게 검색하기 위한 인덱스
-- (모든 벡터를 일일이 비교하지 않고, 그래프 탐색으로 근사 최근접 이웃을 찾음)
CREATE INDEX ON lostpet_vector_store
    USING hnsw (embedding vector_cosine_ops);
```

### 실제 저장되는 데이터 예시

```
┌──────────┬───────────────────────────────┬─────────────────────────┬───────────────┐
│ id       │ content                       │ metadata                │ embedding     │
├──────────┼───────────────────────────────┼─────────────────────────┼───────────────┤
│ uuid-001 │ "https://...dog1.jpg          │ {"sightingId": 42,      │ [0.023,       │
│          │  서울 강남구 역삼동 823-1       │  "finderId": 7,         │  -0.089,      │
│          │  갈색 소형 푸들, 빨간 목줄      │  "foundLocation":       │  0.155,       │
│          │  착용, 겁먹은 상태             │  "서울 강남구 역삼동      │  ...,         │
│          │  2026-03-08T15:30:00"         │  823-1", ...}           │  0.003]       │
│          │                               │                         │  (768개)      │
├──────────┼───────────────────────────────┼─────────────────────────┼───────────────┤
│ uuid-002 │ "https://...dog2.jpg          │ {"sightingId": 38,      │ [0.045,       │
│          │  서울 강남구 대치동             │  "finderId": 12,        │  -0.072,      │
│          │  브라운 토이푸들 배회 중        │  "foundLocation":       │  0.133,       │
│          │  2026-03-08T18:00:00"         │  "서울 강남구 대치동",   │  ...,         │
│          │                               │  ...}                   │  0.011]       │
│          │                               │                         │  (768개)      │
└──────────┴───────────────────────────────┴─────────────────────────┴───────────────┘
```

---

## 9. 설정값 요약

`application.properties`에서 관리되는 주요 설정:

| 설정 | 값 | 설명 |
|------|-----|------|
| `GEMINI_EMBEDDING_MODEL` | gemini-embedding-001 | Gemini 임베딩 모델명 |
| `SPRING_AI_PGVECTOR_DIMENSIONS` | 768 | 벡터 차원 수 |
| `SPRING_AI_PGVECTOR_DISTANCE_TYPE` | COSINE_DISTANCE | 유사도 측정 방식 |
| `SPRING_AI_PGVECTOR_INDEX_TYPE` | HNSW | 벡터 인덱스 알고리즘 |
| `LOSTPET_AI_VECTOR_TOP_K` | 50 | 벡터 검색 시 가져올 최대 건수 |
| `LOSTPET_SEARCH_TOP_N` | 20 | 최종 후보로 반환할 건수 |
| `LOSTPET_SEARCH_SESSION_TTL_HOURS` | 24 | 검색 세션 유효 시간 |

---

## 부록 A: 왜 MySQL이 아닌 PostgreSQL + pgvector인가?

MySQL 9.0에서 VECTOR 타입이 추가되었지만, 현시점(2026)에서 AI 벡터 검색에는 PostgreSQL + pgvector가 압도적으로 유리합니다.

### A-1. 가장 결정적인 차이: 벡터 인덱스

| | PostgreSQL + pgvector | MySQL 9.x |
|---|---|---|
| **벡터 인덱스** | HNSW, IVFFlat 지원 | **없음** (B-Tree만 있음) |
| **검색 방식** | 근사 최근접 이웃(ANN) 탐색 | **전체 테이블 스캔 (brute-force)** |
| **1만 건 검색** | ~수 밀리초 | 수십~수백 밀리초 |
| **100만 건 검색** | ~수십 밀리초 | **수 초 ~ 수십 초** |

MySQL 9의 B-Tree 인덱스는 `>`, `<`, `=` 같은 범위 검색용입니다. "768차원 공간에서 가장 가까운 점 찾기"에는 근본적으로 맞지 않아서, 데이터가 늘수록 성능이 **기하급수적으로** 나빠집니다.

pgvector의 HNSW 인덱스는 벡터 간 근접 관계를 그래프 구조로 미리 구축해두기 때문에, 수백만 건에서도 빠르게 검색합니다.

```
[HNSW 인덱스 동작 원리 - 계층적 그래프 탐색]

Layer 2 (최상위):    A ─────────────── D          ← 넓게 점프하며 탐색 시작
                     │                 │
Layer 1 (중간):      A ──── B ──── C ── D          ← 점점 좁혀감
                     │      │     │    │
Layer 0 (최하위):    A ─ E ─ B ─ F ─ C ─ G ─ D     ← 정밀 탐색

→ 전체를 다 비교하지 않고, 위에서 아래로 내려가며 근처만 탐색
→ 100만 건이어도 수십 번의 비교만으로 최근접 이웃을 찾음
```

### A-2. 기능 비교

| 기능 | pgvector | MySQL 9 |
|------|----------|---------|
| 코사인 유사도 | `<=>` 연산자 | `DISTANCE()` 함수 (제한적) |
| L2 거리 | `<->` 연산자 | 지원 |
| 내적 (Inner Product) | `<#>` 연산자 | 미지원 |
| 벡터 컬럼을 PK/FK로 사용 | N/A (별도 PK) | **불가능** |
| 집계 함수 (AVG 등) | 가능 | **COUNT만 가능** |
| 최대 차원 | 16,000 | 16,383 |

### A-3. AI 프레임워크 생태계 지원

```
                            pgvector        MySQL 9 VECTOR
                           ─────────       ────────────────
Spring AI VectorStore       ✅ 공식 지원     ❌ 미지원
LangChain                   ✅ 공식 지원     ❌ 미지원
LlamaIndex                  ✅ 공식 지원     ❌ 미지원
OpenAI Cookbook              ✅ 예제 포함     ❌ 없음
```

우리 프로젝트에서 쓰는 **Spring AI의 VectorStore도 pgvector만 공식 지원**합니다. MySQL 9 벡터를 쓰려면 SQL을 직접 모두 작성해야 합니다.

### A-4. 우리 서비스 기준 실질적 차이

```sql
-- [PostgreSQL + pgvector + HNSW] (우리 서비스가 사용하는 방식)
SELECT id, content, metadata,
       1 - (embedding <=> query_vector) AS similarity
FROM lostpet_vector_store
ORDER BY embedding <=> query_vector    -- HNSW 인덱스가 빠르게 탐색
LIMIT 50;
-- → 제보 10만 건 기준 ~10ms

-- [MySQL 9] (가상 시나리오)
SELECT id, content, metadata,
       1 - DISTANCE(embedding, query_vector, 'COSINE') AS similarity
FROM lostpet_vector_store
ORDER BY DISTANCE(embedding, query_vector, 'COSINE')    -- 인덱스 없이 전체 스캔
LIMIT 50;
-- → 제보 10만 건 기준 ~수 초 (사용자 체감 느림)
```

### A-5. 성능 벤치마크 (2026 기준)

pgvector + pgvectorscale(Timescale 제공) 조합은 **5천만 벡터에서 99% recall 기준 471 QPS**를 달성했습니다. 이는 전문 벡터 DB인 Qdrant보다 11.4배 빠른 수치입니다.

MySQL 9은 ANN 인덱스가 없어서 수만 건만 넘어가도 실용적인 응답 시간을 보장하기 어렵습니다.

### A-6. 결론

MySQL 9의 VECTOR 타입은 스펙시트에 "벡터 지원"이라고 적을 수 있는 수준이지만, 실무에서 AI 서비스를 운영하기에는 **HNSW 같은 ANN 인덱스의 부재**가 치명적입니다. PostgreSQL + pgvector는 인덱싱, 성능, 프레임워크 생태계 모두에서 현시점 최선의 선택입니다.

> 참고 자료:
> - [The 'Vector Lite' Trap: MySQL 9.x AI Parity Illusion](https://tech-champion.com/database/mysql/the-vector-lite-trap-mysql-9-x-ai-parity-illusion/)
> - [pgvector: Key features, tutorial, and pros and cons (2026 guide)](https://www.instaclustr.com/education/vector-database/pgvector-key-features-tutorial-and-pros-and-cons-2026-guide/)
> - [PostgreSQL vs MySQL: Which Database Should You Choose in 2026?](https://www.bytebase.com/blog/postgres-vs-mysql/)

---

## 부록 B: 자주 하는 질문

### Q. Gemini에 이미지가 전송되나요?
**아니요.** 이미지 URL 문자열만 텍스트에 포함될 뿐, 실제 이미지 파일이 Gemini로 전송되지는 않습니다. 임베딩은 텍스트 기반으로만 생성됩니다.

### Q. 벡터 DB에서 검색이 느리지 않나요?
HNSW(Hierarchical Navigable Small World) 인덱스를 사용하므로, 수만~수십만 건의 벡터가 있어도 밀리초 단위로 검색이 가능합니다. 정확도를 약간 희생하고 속도를 얻는 근사 최근접 이웃(ANN) 알고리즘입니다.

### Q. 임베딩 모델을 바꾸면 기존 데이터는?
모델마다 같은 텍스트도 다른 벡터를 생성합니다. 모델 변경 시 기존 벡터 데이터를 새 모델로 다시 임베딩해야 정확한 검색이 됩니다. (text-embedding-004 → gemini-embedding-001 변경 시 해당)

### Q. 검색 세션이 24시간 후 만료되면?
새로 AI 분석을 요청하면 됩니다. 세션은 검색 결과의 스냅샷이므로, 그 사이에 새로운 제보가 등록되었을 수 있어 오히려 더 좋은 결과를 얻을 수 있습니다.

---

## 부록 C: 트러블슈팅 - Document ID UUID 이슈

### C-1. 증상

- 실종 신고에서 "분석하기"를 눌러도 **매칭 후보가 0건**으로 나옴
- 제보(Sighting) 등록 자체는 정상 동작
- Google AI Studio에서 Gemini API 사용량은 확인됨 (분석 요청 시 호출)
- **PostgreSQL `lostpet_vector_store` 테이블이 비어있음**

### C-2. 원인

`LostPetAiClientImpl.toSightingDocument()`에서 Document ID를 다음과 같이 생성하고 있었음:

```java
// 수정 전 (버그)
return new Document("sighting-" + sighting.getId(), content, metadata);
// 결과: Document ID = "sighting-42" (문자열)
```

그런데 `lostpet_vector_store.id` 컬럼은 **UUID 타입**:

```sql
CREATE TABLE lostpet_vector_store (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,  -- UUID 타입!
    ...
);
```

Spring AI의 `PgVectorStore.add()`가 내부적으로 실행하는 INSERT문에서 `"sighting-42"`를 UUID로 캐스팅하려 할 때 **PostgreSQL 타입 변환 오류**가 발생:

```
ERROR: invalid input syntax for type uuid: "sighting-42"
```

이 에러가 `indexSighting()`의 try-catch에 잡혀서 **경고 로그만 남기고 조용히 무시**됨:

```java
// LostPetAiClientImpl.indexSighting() - 예외를 삼키는 코드
try {
    vectorStore.add(List.of(toSightingDocument(sighting)));
} catch (Exception exception) {
    log.warn("lostpet.vector.index failed sightingId={} reason={}",
             sighting.getId(), exception.getClass().getSimpleName());
    // ← 여기서 끝. 예외가 전파되지 않음
}
```

결과적으로:
- 제보는 `sighting` 테이블에 저장됨 (성공)
- 벡터 인덱싱은 매번 실패 → `lostpet_vector_store` 테이블은 항상 비어있음
- AI 분석 시 빈 테이블에서 검색 → 후보 0건

### C-3. 수정

Document ID를 UUID로 변경 (커밋 `2b53626`):

```java
// 수정 후
return new Document(UUID.randomUUID().toString(), content, metadata);
// 결과: Document ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890" (유효한 UUID)
```

`sightingId`는 이미 metadata에 저장되고 있으므로 (`metadata.put("sightingId", sighting.getId())`), 검색 결과에서 sightingId를 조회하는 `toCandidate()` 로직에는 영향 없음.

### C-4. 배포 후 필요한 작업: 기존 제보 재인덱싱

수정 배포 후에도 **기존에 등록된 제보들은 벡터 인덱싱이 안 된 상태**입니다. 새로 등록되는 제보만 정상 인덱싱됩니다.

기존 제보를 재인덱싱하려면 일회성 마이그레이션이 필요합니다:

```sql
-- 먼저 기존 벡터 데이터 확인 (비어있을 것)
SELECT COUNT(*) FROM lostpet_vector_store;

-- 인덱싱이 필요한 제보 수 확인
SELECT COUNT(*) FROM sighting WHERE status = 'OPEN';
```

재인덱싱 방법 (택 1):
1. **API 기반:** 각 sighting을 순회하며 `LostPetAiClient.indexSighting()`을 호출하는 일회성 배치 엔드포인트 작성
2. **서비스 재시작 기반:** ApplicationRunner에서 벡터 미등록 제보를 감지하여 자동 인덱싱하는 로직 추가
3. **수동:** 제보 수가 적다면, 기존 제보를 삭제 후 다시 등록

### C-5. 교훈

- Spring AI PgVectorStore의 Document ID는 **반드시 유효한 UUID 문자열**이어야 함
- 외부 서비스(벡터 스토어, AI API) 호출 시 예외를 삼키는 패턴은 디버깅을 어렵게 함
- 벡터 인덱싱 실패를 감지할 수 있는 모니터링(예: `lostpet_vector_store` 건수 체크)이 필요

---

## 부록 D: 트러블슈팅 - 임베딩 차원 불일치 및 트랜잭션 오염

### D-1. 증상

- **제보(Sighting) 등록 자체가 실패** (저장되지 않음)
- PostgreSQL 로그에 두 가지 에러가 연쇄적으로 발생:

```
ERROR: expected 768 dimensions, not 3072
STATEMENT: INSERT INTO public.lostpet_vector_store (id, content, metadata, embedding) VALUES ($1, $2, $3::jsonb, $4)
           ON CONFLICT (id) DO UPDATE SET content = $5, metadata = $6::jsonb, embedding = $7

ERROR: current transaction is aborted, commands ignored until end of transaction block
STATEMENT: insert into timeline_event (...) values ($1,$2,$3,...) RETURNING *
```

### D-2. 원인: 두 가지 문제의 연쇄 작용

#### 문제 1: 임베딩 차원 불일치

`gemini-embedding-001` 모델은 기본적으로 **3072차원** 벡터를 출력합니다. 그러나 `lostpet_vector_store.embedding` 컬럼은 `VECTOR(768)`로 정의되어 있어, INSERT 시 차원 불일치 에러가 발생합니다.

```
이전 모델: text-embedding-004  → 기본 768차원  ← DB 컬럼과 일치
현재 모델: gemini-embedding-001 → 기본 3072차원 ← DB 컬럼과 불일치!
```

모델 업그레이드(커밋 `19cc9d4`) 시 차원 설정을 함께 변경하지 않아 발생한 문제입니다.

#### 문제 2: PostgreSQL 트랜잭션 오염 (Transaction Poisoning)

`SightingService.create()`는 `@Transactional` 메서드입니다. 내부에서 호출되는 `indexSighting()`의 `vectorStore.add()`가 같은 트랜잭션 안에서 SQL INSERT를 실행합니다.

```java
@Transactional
public SightingResponse create(Long memberId, SightingCreateRequest request) {
    Sighting saved = sightingRepository.save(...);     // ① DB INSERT (성공)
    try {
        lostPetAiClient.indexSighting(saved);           // ② 벡터 INSERT (실패!)
    } catch (Exception exception) {
        log.warn(...);                                  // ③ Java에서 예외 catch
    }
    eventPublisher.publishEvent(...);                   // ④ timeline INSERT (실패!)
    return ...;                                         // ⑤ 트랜잭션 커밋 시도 → 롤백
}
```

**핵심:** PostgreSQL은 트랜잭션 내에서 SQL 에러가 한 번이라도 발생하면, **그 트랜잭션 전체를 "중단됨(aborted)" 상태로 표시**합니다. Java에서 예외를 catch해도 PostgreSQL의 트랜잭션 상태는 복구되지 않습니다. 이후 같은 트랜잭션 내의 모든 SQL은 `current transaction is aborted` 에러로 실패합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                     하나의 @Transactional 안에서                      │
│                                                                     │
│  ① sighting INSERT        → 성공 (아직 커밋 안 됨)                   │
│  ② vector_store INSERT    → 실패! (차원 불일치)                      │
│     └─ PostgreSQL 트랜잭션 → "aborted" 상태로 전환                   │
│  ③ Java catch             → 예외를 잡음 (하지만 PG 상태는 이미 망가짐)│
│  ④ timeline_event INSERT  → 실패! ("transaction is aborted")        │
│  ⑤ 트랜잭션 커밋          → 실패 → 전체 롤백                         │
│     └─ ①의 sighting도 롤백됨!                                       │
│                                                                     │
│  결과: 제보 등록이 완전히 실패                                        │
└─────────────────────────────────────────────────────────────────────┘
```

이것이 MySQL과 PostgreSQL의 중요한 차이점입니다. MySQL은 트랜잭션 내 SQL 에러를 catch하면 계속 진행할 수 있지만, **PostgreSQL은 트랜잭션이 한번 오염되면 ROLLBACK 전까지 어떤 SQL도 실행할 수 없습니다.**

### D-3. 수정 (커밋 `c682293`)

#### 수정 1: 임베딩 출력 차원 명시 설정

`application.properties`에 `gemini-embedding-001`의 출력 차원을 768로 제한:

```properties
# 수정 전: 차원 설정 없음 → gemini-embedding-001이 기본 3072차원 출력
spring.ai.google.genai.embedding.text.options.model=gemini-embedding-001

# 수정 후: 출력 차원을 768로 명시
spring.ai.google.genai.embedding.text.options.model=gemini-embedding-001
spring.ai.google.genai.embedding.text.options.dimensions=768
```

768차원을 사용하는 이유:
- PgVector HNSW 인덱스가 최대 2000차원까지 지원 (3072차원 사용 시 HNSW 불가)
- 기존 DB 스키마(`VECTOR(768)`)와 호환
- 실종 반려동물 매칭 용도로 768차원이면 충분

#### 수정 2: 벡터 인덱싱 트랜잭션 분리

`indexSighting()`에 `@Transactional(propagation = REQUIRES_NEW)`를 적용하여, 벡터 인덱싱이 **별도 트랜잭션**에서 실행되도록 변경:

```java
// 수정 후
@Override
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void indexSighting(Sighting sighting) {
    // 이 메서드는 별도 트랜잭션에서 실행됨
    // 실패해도 호출자(SightingService)의 트랜잭션에 영향 없음
}
```

```
┌──────────────────────────────────────────────────────────┐
│  SightingService.create()의 트랜잭션 (TX-A)                │
│                                                          │
│  ① sighting INSERT → 성공                                │
│                                                          │
│  ② indexSighting() 호출                                   │
│     ┌────────────────────────────────────────────┐       │
│     │  별도 트랜잭션 (TX-B, REQUIRES_NEW)          │       │
│     │  vector_store INSERT → 성공 or 실패         │       │
│     │  (실패해도 TX-A에 영향 없음)                  │       │
│     └────────────────────────────────────────────┘       │
│                                                          │
│  ③ timeline_event INSERT → 성공 (TX-A는 정상 상태)        │
│  ④ 트랜잭션 커밋 → 성공                                   │
│                                                          │
│  결과: 벡터 인덱싱이 실패해도 제보 등록은 정상 완료          │
└──────────────────────────────────────────────────────────┘
```

### D-4. 교훈

- 임베딩 모델 업그레이드 시 **출력 차원 변경 여부를 반드시 확인**하고 설정을 맞춰야 함
- PostgreSQL의 트랜잭션 오염(Transaction Poisoning)은 MySQL과 다른 중요한 동작 차이
- 외부 서비스(AI API, 벡터 스토어) 호출은 **핵심 비즈니스 로직과 트랜잭션을 분리**하는 것이 안전
- `REQUIRES_NEW` 전파 속성을 활용하면, 부가 기능의 실패가 핵심 기능에 영향을 주지 않음

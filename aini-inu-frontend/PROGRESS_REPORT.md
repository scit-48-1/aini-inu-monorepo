# 아이니이누 (Aini Inu) — 프론트엔드 구현 진행 보고서

> 작성일: 2026-02-23
> 담당: Claude (Implementer)
> 협업: Gemini (Architect)

---

## 프로젝트 개요

반려견 견주들을 위한 **동네 산책 메이트 매칭 & 로컬 커뮤니티 플랫폼**.

- **기술 스택**: Next.js 14 (App Router) · TypeScript · Tailwind CSS · Zustand · MSW (Mock Service Worker)
- **API 모킹**: MSW + localStorage 가상 DB (`aini_inu_v6_db`)
- **지도**: Leaflet · 주소입력: Daum Postcode API · 좌표변환: Nominatim

---

## 전체 진행 현황

| 우선순위 | 완료 | 미완료 |
|---------|------|-------|
| 🔴 Critical (3개) | 3 / 3 | 0 |
| 🟠 High (7개) | 7 / 7 | 0 |
| 🟡 Medium (4개) | 4 / 4 | 0 |
| 🟢 Low (3개) | 2 / 3 | 1 |
| **합계** | **16 / 17** | **1** |

---

## 완료된 작업 상세

---

### 🔴 Critical — 즉시 수정 (2026-02-20)

#### C-1. 인증 가드 구현
- **파일**: `src/app/layout.tsx`
- **내용**: `useEffect` + `localStorage.currentUserId` 확인 → 보호 경로 접근 시 `/login` 리다이렉트

#### C-2. `db.currentUserId` 초기화 불일치 수정
- **파일**: `src/mocks/handlers.ts`
- **내용**: 소셜 로그인 핸들러가 `u-` 접두사 가입 유저 우선 탐색, 없으면 neighbor fallback

#### C-3. `/posts` 핸들러 `memberId` 필터링
- **파일**: `src/mocks/handlers.ts`
- **내용**: `GET /posts?memberId=xxx` → `author.id` 기준 필터링 구현

---

### 🟠 High — 기능 이상 수정 (2026-02-20)

#### H-1. 누락 핸들러 3개 추가
- **파일**: `src/mocks/handlers.ts`
- **내용**:
  - `GET /api/v1/members/:memberId/dogs` — 타인 강아지 목록 조회
  - `POST /api/v1/members/me/dogs` — 내 강아지 등록
  - `POST /api/v1/walk-diaries/:id` — 산책 일기 저장

#### H-2. 유저 ID 체계 통일
- **파일**: `src/constants/index.ts`
- **내용**: `MOCK_CHAT_ROOMS.messages[].senderId`를 `u1/u2` → `neighbor-1/neighbor-2`로 수정

#### H-3. `ThreadType` 필드 정규화
- **파일**: `src/types/index.ts`
- **내용**: 런타임 확장 필드 추가 — `owner?`, `place?`, `thumbnail?`, `image?`, `time?`, `name?`, `breed?`, `content?`

#### H-4. `FeedPostType` 필드명 통일
- **파일**: `src/types/index.ts`
- **내용**: `content?`, `createdAt?` 필드 추가

#### H-5. `ChatMessage` 필드명 통일
- **파일**: `src/types/index.ts`
- **내용**: `content?` 필드 추가

#### H-6. ID 체계 단일화 (2026-02-23)
- **파일**: `src/mocks/handlers.ts`, `src/app/dashboard/page.tsx`, `src/hooks/useRadarLogic.ts`
- **내용**:
  - `seedDatabase()` seed 유저 3명 → 10명으로 확장
  - 각 유저별 고유 닉네임·견종·위치·성별·MBTI·매너점수 부여
  - DB 버전 키 `v5` → `v6` 업그레이드 (구버전 localStorage 자동 초기화)
  - 미사용 `MOCK_USER` import 2곳 제거

  **10명의 seed 유저:**
  | ID | 닉네임 | 견종 | 지역 |
  |----|--------|------|------|
  | neighbor-1 | 보리누나 | 말티즈 | 성수동 |
  | neighbor-2 | 초코아빠 | 푸들 | 이촌동 |
  | neighbor-3 | 해피매니저 | 시바이누 | 마포구 |
  | neighbor-4 | 몽이파파 | 비글 | 용산구 |
  | neighbor-5 | 루이언니 | 포메라니안 | 강남구 |
  | neighbor-6 | 두부맘 | 골든리트리버 | 서대문구 |
  | neighbor-7 | 별이달이 | 웰시코기 | 송파구 |
  | neighbor-8 | 맥스가디언 | 사모예드 | 강동구 |
  | neighbor-9 | 코코리 | 비숑프리제 | 은평구 |
  | neighbor-10 | 힐링펫 | 치와와 | 종로구 |

#### H-7. 프로필 수정 기능 실제 연동 (2026-02-23)
- **파일**: `src/app/profile/[memberId]/page.tsx`
- **내용**: `ProfileEditModal`의 `onSave` 콜백에서 `memberService.updateMe()` 실제 호출 + 성공/실패 toast

---

### 🟡 Medium — 개선 (2026-02-20)

#### M-1. 쿼리 파라미터 처리 개선
- **파일**: `src/mocks/handlers.ts`
- **내용**:
  - `GET /threads` — Haversine 공식으로 5km 이내 스레드만 반환
  - `GET /threads/hotspot` — `hours` 파라미터 동적 count 계산
  - `POST /threads/:id/join` — `joinedUsers` 배열로 중복 참여 방지 + 409 반환

#### M-2. 이미지 도메인 등록
- **파일**: `next.config.ts`
- **내용**: `api.dicebear.com`, `gstatic.com`, `clova-phinf.pstatic.net`, `developers.kakao.com` 추가

#### M-3. `author.name` → `author.nickname` 수정
- **파일**: `src/components/feed/FeedItem.tsx`
- **내용**: `author?.name` 폴백 제거, `author?.nickname` 단일 사용

#### M-4. 팔로워/팔로잉 기능 구현
- **파일**: `src/mocks/handlers.ts`, `src/services/api/memberService.ts`
- **내용**:
  - `follows: [{followerId, followingId}]` DB 구조 추가
  - `POST/DELETE /members/me/follow/:targetId` 핸들러 구현
  - `/walk-diaries/following` 팔로잉 유저 기반 필터링
  - `memberService.follow()`, `memberService.unfollow()` 추가

---

### 🟢 Low — 정리 (2026-02-20)

#### L-1. 인증 서비스 확인
- `src/services/authService.ts` 이미 완비 확인 (login, signup, social login 등)

#### L-2. TypeScript 오류 해소
- H-3/H-4/H-5 타입 정규화로 주요 오류 해소
- 현재 TypeScript 에러: **0개**

---

### 🔵 기타 완료 작업 (이전 세션, 2026-02-19~20)

| 항목 | 내용 |
|------|------|
| Around Me 런타임 에러 수정 | Invalid LatLng 에러 — `DynamicMap.tsx` marker 필터링 추가 |
| MSW 핸들러 11개 추가 | 초기 API 커버리지 확보 |
| locationService 수정 | AbortController 타임아웃 버그 수정 |
| 플립북 뷰어 버그 8개 수정 | 다이어리 표시·애니메이션·소멸 버그 |
| walk-diaries authorId 통일 | neighbor 체계와 일치하도록 수정 |
| DATA_CONTRACTS.md 생성 | ID 체계 및 데이터 규약 문서화 |
| LocalFeedPreview 링크 수정 | `/feed` → `/around-me` |

---

### 🆕 이번 세션 추가 기능 (2026-02-23)

| 기능 | 파일 | 내용 |
|------|------|------|
| 회원가입 성별 선택 UI | `ManagerStep.tsx` | 남성/여성 버튼, 기본값 없이 필수 선택 |
| 성별 필드 기본값 제거 | `useSignupForm.ts` | `gender: ''` (미선택 시 가입 불가) |
| 산책 모집글 수정 기능 | `threadService.ts`, `handlers.ts`, `useRadarLogic.ts`, `RecruitForm.tsx`, `RadarMapSection.tsx` | 수정 모드 폼 pre-fill, PUT /threads/:id 핸들러 |
| 산책 모집글 삭제 기능 | 동일 | 삭제 인라인 확인 UI, DELETE /threads/:id 핸들러 |
| 중복 모집 방지 | `useRadarLogic.ts`, `RecruitForm.tsx` | `myActiveThread` 체크 → 차단 화면 표시 |
| 시간 과거 선택 방지 | `RecruitForm.tsx` | `min={현재시각}` 속성 |
| 삭제 확인 인라인 UI | `RadarMapSection.tsx` | "삭제하기" → "취소 / 삭제 확인" 전환 |

---

## 남은 작업

| 항목 | 우선순위 | 내용 |
|------|---------|------|
| **L-3. `any` 타입 제거** | 🟢 Low | 약 66곳의 `any` 타입을 실제 인터페이스로 교체 |

---

## 아키텍처 결정 사항

| 결정 | 이유 |
|------|------|
| GPS/GeoLocation 미사용 | 웹 서비스 전용 — 앱 전환 시 추가 예정. 위치는 Daum Postcode 입력만 사용 |
| MSW + localStorage | 백엔드 연동 전 완전한 API 시뮬레이션 |
| Zustand persist | 위치(currentLocation, lastCoordinates) 세션 간 유지 |
| neighbor ID 체계 | 런타임 DB: `neighbor-{n}` (seed) / `u-{timestamp}` (가입) |
| DB 버전 키 관리 | `aini_inu_v6_db` — 구조 변경 시 키 업그레이드로 자동 마이그레이션 |

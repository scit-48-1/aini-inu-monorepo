# Phase 11: Dashboard - Context

**Gathered:** 2026-03-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Cross-domain composition dashboard: greeting/manner score/walk stats, walk recommendations from hotspots, neighborhood thread summary, pending review detection with modal submit. Each section handles its own loading/error state independently (partial-failure fallback). Frontend-only changes — no backend or common-docs modifications.

</domain>

<decisions>
## Implementation Decisions

### Section composition & order
- DraftNotification 제거 (죽은 코드 — 백엔드에 isDraft 개념 없음)
- PendingReviewCard로 교체 (최상단, 미작성 리뷰 있을 때만 표시)
- RecentFriends 유지 (DASH 요구사항 외지만 기존 기능 보존, API만 정리)
- 최종 렌더링 순서: (1) PendingReviewCard (conditional) → (2) AIBanner (hotspot coaching) → (3) DashboardHero (greeting + manner + stats) → (4) RecentFriends (chat history) → (5) LocalFeedPreview (latest threads)
- 모든 컴포넌트 old service imports 제거 → `@/api/*` 모듈로 rewire
- Legacy types (`ThreadType`, `UserType`) → proper API response types (`ThreadResponse`, `MemberResponse`, `WalkStatsResponse`, `ThreadHotspotResponse`)

### Pending review UX (DASH-04)
- 대시보드 최상단에 PendingReviewCard 알림 카드 표시 (미작성 리뷰 N건)
- 카드 클릭 → 모달 오픈 (페이지 로드 시 자동 팝업 아님)
- 여러 건이면 모달 내 목록 표시 → 하나 선택 → 리뷰 작성 → 다음 건
- 제출 실패 시 모달 닫지 않고 에러 메시지 + 재시도 버튼 표시
- 모달 닫기(dismiss) 가능 — 다음 방문 시 다시 알림 카드 노출
- 감지 로직: `getRooms()` → 각 방 reviews/me 체크 → 미작성 방 필터

### Walk stats heatmap (DASH-01)
- `WalkStatsResponse.points` (date+count 배열) → 7행 N열 grid로 변환 (고정 126 대신 실제 windowDays 기반)
- 빈 날짜는 count=0으로 채움
- `totalWalks`는 API 값(`WalkStatsResponse.totalWalks`) 직접 사용
- streak, successRate는 프론트에서 points 배열 기반으로 계산 (현재 로직 유지)

### Hotspot recommendation (DASH-02)
- `getHotspots()` 결과에서 count가 가장 높은 hotspot 선택
- AI coaching 배너에 해당 region과 추천 메시지 표시

### Data fetching strategy
- 4+ 병렬 API 호출: `Promise.allSettled` — `getMe()` + `getWalkStats()` + `getHotspots()` + `getThreads()` + `getRooms()`
- 각 섹션 독립적 error boundary / loading 처리
- 섹션별 5-state guarantee: default / loading / empty / error / success

### Partial failure fallback (DASH-05)
- 한 섹션 API 실패 시 해당 섹션만 에러 fallback (retry 버튼 포함)
- 다른 섹션은 정상 표시
- 전체 페이지 에러 없음 (개별 섹션 단위)

### Claude's Discretion
- Suspense boundary vs custom loading state 구현 방식
- Error fallback 카드 비주얼 디자인
- Loading skeleton vs spinner 선택
- PendingReviewCard 비주얼 디자인 (DraftNotification 스타일 참고 가능)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DashboardHero`: greeting, manner score, walk grass grid — type rewire 필요 (UserType → MemberResponse, number[] → WalkStatsResponse)
- `AIBanner`: hotspot coaching banner — rewire to ThreadHotspotResponse
- `RecentFriends`: chat-based friend cards — already wired to @/api/chat
- `LocalFeedPreview`: thread cards — rewire ThreadType → ThreadResponse
- `DraftNotification`: 제거 대상 (isDraft 개념 백엔드에 없음)
- `Card`, `Typography`, `Badge`, `Button`: UI primitives
- `MannerScoreGauge`: manner score display

### Established Patterns
- `Promise.allSettled` already used in current page.tsx
- Centralized API modules: `@/api/members`, `@/api/threads`, `@/api/chat`
- Placeholder avatar: `/AINIINU_ROGO_B.png`
- `useUserStore` for current user profile access
- `createReview()` in `@/api/chat.ts` — Phase 8에서 구현 완료

### Integration Points
- `@/api/members.ts`: `getMe()`, `getWalkStats()` — WalkStatsResponse { windowDays, startDate, endDate, timezone, totalWalks, points: { date, count }[] }
- `@/api/threads.ts`: `getHotspots()`, `getThreads()` — ThreadHotspotResponse[], ThreadResponse
- `@/api/chat.ts`: `getRooms()`, `getRoom()`, `createReview()`, `getMyReviews()`
- Old services to remove: `threadService`, `memberService` imports in dashboard

</code_context>

<specifics>
## Specific Ideas

- React best practices: async-parallel for 4+ API calls, per-section independent error handling
- PendingReviewCard는 DraftNotification의 다크 배경 스타일 참고 — 같은 위치, 같은 행동 유도 역할
- Hotspot 추천: count 최대인 hotspot의 region을 코칭 메시지에 삽입
- WalkStatsResponse.points → heatmap grid 변환 유틸 함수로 분리

</specifics>

<deferred>
## Deferred Ideas

- 산책 완료 + 일기 미작성 감지 알림 — 백엔드에 해당 엔드포인트 없음. 백엔드 API 추가 후 구현 가능.

</deferred>

---

*Phase: 11-dashboard*
*Context gathered: 2026-03-08*

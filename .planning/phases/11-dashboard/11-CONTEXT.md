# Phase 11: Dashboard - Context

**Gathered:** 2026-03-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Cross-domain composition dashboard: greeting/manner score/walk stats, walk recommendations from hotspots, neighborhood thread summary, pending review detection with modal submit. Each section handles its own loading/error state independently (partial-failure fallback). Frontend-only changes — no backend or common-docs modifications.

</domain>

<decisions>
## Implementation Decisions

### Section composition
- Keep existing 5 components but rewire all to `@/api/*` modules (remove old `threadService`, `memberService` imports)
- Sections in order: (1) Pending Review Modal (conditional), (2) AI Coaching Banner (hotspot), (3) Hero (greeting + manner score + walk stats heatmap), (4) Recent Friends (chat history), (5) Local Feed Preview (latest threads)
- All section data types rewired from legacy `ThreadType`/`UserType` to proper API response types (`ThreadResponse`, `MemberResponse`, `WalkStatsResponse`, `ThreadHotspotResponse`)

### Data fetching strategy
- 4 parallel API calls via `Promise.allSettled`: `getMe()` + `getWalkStats()` + `getHotspots()` + `getThreads()` + `getRooms()`
- Each section wrapped in independent error boundary / Suspense-like pattern for isolated loading states
- Per-section 5-state guarantee: default / loading / empty / error / success

### FR-DASH-001: Greeting + stats
- API: `GET /api/v1/members/me` (manner score, nickname) + `GET /api/v1/members/me/stats/walk` (walk activity)
- Display: greeting with pet name, manner score gauge, walk activity heatmap (grass grid)

### FR-DASH-002: Walk recommendations
- API: `GET /api/v1/threads/hotspot`
- Display: AI coaching banner with highest-count hotspot region and recommendation message

### FR-DASH-003: Neighborhood threads
- API: `GET /api/v1/threads` (latest, small page size)
- Display: up to 3 thread cards with author, location, time, title, description

### FR-DASH-004: Pending review modal
- API: `GET /api/v1/chat-rooms` to get rooms, then check `reviews/me` per room to find unwritten reviews
- Modal appears when unwritten reviews detected
- Submit via `POST .../reviews` with retry on failure
- User can dismiss modal without writing review

### FR-DASH-005: Partial failure fallback
- Each section independently handles API failure — failed section shows error fallback with retry button
- Other sections remain fully functional
- No full-page error for individual section failures

### Claude's Discretion
- Exact Suspense boundary implementation vs custom loading state per component
- Error fallback card visual design
- Whether to use React ErrorBoundary class or custom try/catch pattern per section
- Loading skeleton vs spinner choice per section

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DashboardHero`: Existing component with greeting, manner score, walk grass grid — needs type rewire from `UserType` to `MemberResponse`
- `AIBanner`: Hotspot coaching banner — needs rewire from hardcoded data to `ThreadHotspotResponse`
- `RecentFriends`: Chat-based friend cards — already wired to `@/api/chat` (`getRooms`, `getRoom`)
- `LocalFeedPreview`: Thread cards — needs rewire from `ThreadType` to `ThreadResponse`
- `DraftNotification`: Draft diary notification — needs evaluation against pending review requirement
- `Card`, `Typography`, `Badge`, `Button`: UI primitives used across all dashboard components
- `MannerScoreGauge`: Manner score display component already used in LocalFeedPreview

### Established Patterns
- `Promise.allSettled` already used in current `page.tsx` for parallel data fetching
- Centralized API modules: `@/api/members`, `@/api/threads`, `@/api/chat`
- Placeholder avatar: `/AINIINU_ROGO_B.png` for missing images
- Zustand store: `useUserStore` for current user profile access

### Integration Points
- `@/api/members.ts`: `getMe()`, `getWalkStats()` — already implemented
- `@/api/threads.ts`: `getHotspots()`, `getThreads()` — already implemented
- `@/api/chat.ts`: `getRooms()`, `getRoom()` — already implemented
- Review API: `createReview()` in `@/api/chat.ts` — already implemented from Phase 8
- Old services to remove: `threadService`, `memberService` imports in dashboard page

</code_context>

<specifics>
## Specific Ideas

- React best practices: `async-parallel` pattern for 4+ API calls, consider `async-suspense-boundaries` for per-section independence
- Walk stats heatmap (grass grid) should use `WalkStatsResponse` shape from `getWalkStats()` instead of arbitrary `number[]`
- Hotspot recommendation: show the hotspot with highest count as the AI coaching message
- Pending review detection: iterate chat rooms and check review status per room — surface rooms where current user hasn't written a review

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 11-dashboard*
*Context gathered: 2026-03-08*

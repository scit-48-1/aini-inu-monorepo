# Phase 2: Common Infrastructure - Context

**Gathered:** 2026-03-06
**Status:** Ready for planning

<domain>
## Phase Boundary

A shared API layer that all domain screens consume, with consistent envelope parsing, error handling, pagination, image upload, auth tokens, and UI state patterns. This phase builds the foundation that Phases 3-12 depend on. No domain-specific screens are built here — only reusable infrastructure.

</domain>

<decisions>
## Implementation Decisions

### Error message presentation
- Toast-only for all API errors (using sonner, already in project)
- Display backend's `ApiResponse.message` as-is (Korean messages from backend) — no frontend error code mapping table
- If `message` is null, show generic fallback message
- 3-second auto-dismiss for all toasts
- Network/timeout errors get a "다시 시도" (retry) action button in the toast; validation/business errors do not
- Success feedback is toast-only (green success toast) — no animations or visual effects

### 5-state UI components (INFRA-07)
- **Loading:** Skeleton screens where layout is predictable (lists, cards, profiles), spinner fallback for dynamic/unknown content
- **Empty:** Lucide icon + descriptive Korean message + CTA action button (e.g., "첫 게시글을 작성해보세요")
- **Error:** Inline error card within the section area (red-tinted) with "다시 시도" retry button — section-level, doesn't affect rest of page
- **Success:** Toast notification only (same pattern as errors but green)
- **Default:** Normal rendered state — no special handling needed
- Build as reusable components that domain phases (3-12) can drop in

### Auth token management (INFRA-06)
- Access token stored in memory (Zustand store), refresh token in localStorage
- On 401 response: silent refresh — call `/auth/refresh` automatically in background
- If refresh succeeds: retry the failed request transparently (user never sees interruption)
- Concurrent 401 handling: queue all pending requests, first 401 triggers single refresh, after refresh replay all queued requests with new token (no duplicate refresh calls)
- If refresh token expired: clear all auth state, show "세션이 만료되었습니다" toast, redirect to /login
- JWT Bearer token auto-attached to all authenticated requests via interceptor in apiClient

### Image upload utility (INFRA-05)
- Click button + native file picker (no drag-and-drop)
- Thumbnail preview of selected image(s) with X button to remove before submitting
- Indeterminate spinner during upload (no progress percentage — standard fetch, not XMLHttpRequest)
- File validation errors shown inline below the file picker (red text: "지원하지 않는 파일 형식입니다", "파일 크기가 초과되었습니다")
- 3-step presigned URL flow: get URL → upload to storage → confirm

### Claude's Discretion
- Skeleton component variants per content type (how many, which shapes)
- Exact pagination hook API design (useInfiniteScroll, usePagination naming and params)
- apiClient refactoring approach (how to restructure existing code)
- Error code categorization logic (which codes are "network" vs "business")
- Token storage key names and Zustand store structure
- File size limits and allowed formats (follow backend validation constraints)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `apiClient.ts` (`src/services/api/apiClient.ts`): Already unwraps `ApiResponse.data`, has 8s timeout — needs auth interceptor, error handling enhancement, PATCH method addition
- Domain services exist (`memberService.ts`, `chatService.ts`, `postService.ts`, `threadService.ts`, `locationService.ts`) — need alignment with centralized patterns
- `useUserStore.ts` (`src/store/useUserStore.ts`): Zustand store — needs JWT token state management added
- `useConfigStore.ts` (`src/store/useConfigStore.ts`): Zustand + persist — reference pattern for localStorage persistence
- `ErrorBoundary.tsx` (`src/components/common/ErrorBoundary.tsx`): Added in Phase 1 — wraps page sections
- `cn()` utility (`src/lib/utils.ts`): clsx + tailwind-merge — use for component styling
- sonner toast library: Already installed and used for notifications

### Established Patterns
- All pages use `'use client'` — no SSR/RSC (constraint: maintain this)
- Service modules use object literal pattern: `export const xService = { method: () => apiClient.get<T>(...) }`
- Custom hooks return `{ data, isLoading, fetchData }` pattern
- Zustand with persist middleware for localStorage-backed state
- No schema validation library (no Zod/Yup) — manual validation in hooks

### Integration Points
- `layout.tsx`: Root layout with auth guard — auth interceptor connects here
- `next.config.ts`: API proxy via rewrites() to localhost:8080 (configured in Phase 1)
- Backend pagination types: `SliceResponse<T>` (infinite scroll), `CursorResponse<T>` (chat cursor), `PageResponse<T>` (traditional) — frontend must type-match all three
- Backend error codes: `ErrorCode` interface with domain enums (M001, C101, WD400_*, L409_*) — frontend receives these in `ApiResponse.errorCode`
- Presigned URL endpoints: `POST /api/v1/images/presigned-url` → `PUT {presigned_url}` → `GET /api/v1/images/{key}`

</code_context>

<specifics>
## Specific Ideas

- Phase 1 produced an API mismatch catalog — use it as the fix list for aligning service modules
- Error toasts should feel lightweight and non-intrusive (3s auto-dismiss default)
- Empty states should be actionable — always suggest what the user can do next
- Auth refresh should be completely invisible to the user — if it works, they never know it happened
- Image upload preview gives users confidence before submitting (especially for pet photos and lost pet reports)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-common-infrastructure*
*Context gathered: 2026-03-06*

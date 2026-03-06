---
phase: 02-common-infrastructure
plan: 01
subsystem: api
tags: [fetch, jwt, zustand, sonner, typescript, api-client]

# Dependency graph
requires:
  - phase: 01-critical-bugs
    provides: Next.js rewrites proxy for /api/v1 forwarding
provides:
  - apiClient with get/post/put/patch/delete and envelope unwrap
  - ApiResponse, ApiError, SliceResponse, CursorResponse types
  - useAuthStore with accessToken (memory) and refreshToken (persisted)
  - 401 refresh queue with concurrent request replay
  - Automatic error toast with Korean backend messages
affects: [02-02-domain-api-modules, 03-auth, all-domain-phases]

# Tech tracking
tech-stack:
  added: [sonner (toast)]
  patterns: [envelope-unwrap, bearer-interceptor, refresh-queue, typed-api-error]

key-files:
  created:
    - aini-inu-frontend/src/api/client.ts
    - aini-inu-frontend/src/api/types.ts
    - aini-inu-frontend/src/store/useAuthStore.ts
  modified: []

key-decisions:
  - "Relative imports in client.ts (not @/ alias) for standalone tsc compatibility"
  - "accessToken in Zustand memory only, refreshToken persisted via partialize"
  - "refresh call uses raw fetch to avoid 401 interceptor loop"

patterns-established:
  - "Envelope unwrap: all API calls return T directly, never ApiResponse<T>"
  - "Error toast: automatic Korean message display with 3s dismiss, suppressToast opt-out"
  - "Auth interceptor: Bearer token auto-attached, 401 triggers refresh queue"

requirements-completed: [INFRA-01, INFRA-02, INFRA-03, INFRA-06]

# Metrics
duration: 2min
completed: 2026-03-06
---

# Phase 2 Plan 1: API Client Foundation Summary

**Fetch-based API client with envelope unwrap, JWT Bearer interceptor, concurrent 401 refresh queue, and Korean error toasts via sonner**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-05T22:19:42Z
- **Completed:** 2026-03-05T22:21:42Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- API types matching backend ApiResponse envelope, pagination patterns, and typed ApiError class
- Auth store with memory-only accessToken and persisted refreshToken via Zustand partialize
- Full API client with get/post/put/patch/delete, auto Bearer injection, 401 refresh queue, and sonner toast

## Task Commits

Each task was committed atomically:

1. **Task 1: Create shared API types and auth store** - `3363972` (feat)
2. **Task 2: Create API client with envelope unwrap, auth interceptor, refresh queue, and error toast** - `0c84254` (feat)

## Files Created/Modified
- `aini-inu-frontend/src/api/types.ts` - ApiResponse, ApiError, SliceResponse, CursorResponse, PaginationParams, CursorPaginationParams
- `aini-inu-frontend/src/api/client.ts` - HTTP client with envelope unwrap, auth interceptor, refresh queue, error toast
- `aini-inu-frontend/src/store/useAuthStore.ts` - Zustand auth store with token management

## Decisions Made
- Used relative imports (`../store/useAuthStore`) instead of path alias (`@/store/useAuthStore`) to ensure `tsc --noEmit` works on individual files
- accessToken kept in Zustand memory only (not persisted) for security; refreshToken persisted via `partialize`
- Refresh endpoint called with raw `fetch` (not through the interceptor) to avoid infinite 401 loop
- `ApiRequestOptions` extends `Omit<RequestInit, 'body' | 'method'>` with `suppressToast` and `skipAuth` flags

## Deviations from Plan

None - plan executed exactly as written. The `types.ts` file already existed from prior work and matched the plan specification exactly.

## Issues Encountered
- `tsc --noEmit` on individual files fails due to sonner's own type declarations referencing React with `esModuleInterop` mismatch; full project `tsc --noEmit` passes cleanly

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- API client ready for all 9 domain API modules (Plan 02) to import
- useAuthStore ready for auth flow integration (Phase 3)
- No blockers

---
*Phase: 02-common-infrastructure*
*Completed: 2026-03-06*

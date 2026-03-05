---
phase: 01-critical-bugs
plan: 01
subsystem: ui
tags: [next.js, error-boundary, react, msw, proxy, tailwind]

# Dependency graph
requires: []
provides:
  - Next.js API proxy rewrites routing /api/v1/* to Spring Boot backend at localhost:8080
  - MSW toggle via NEXT_PUBLIC_ENABLE_MSW=false env var
  - global-error.tsx with own html/body for root layout crash recovery
  - error.tsx in all 10 route directories (root, dashboard, around-me, feed, chat, chat/[id], profile/[memberId], settings, login, signup)
  - Reusable React class ErrorBoundary component in components/common/
affects:
  - 02-api-layer
  - all-frontend-phases

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Next.js rewrites for API proxying to backend"
    - "Next.js error.tsx per-route error boundary convention"
    - "React class component for section-level error catching (only class components support componentDidCatch)"
    - "NEXT_PUBLIC_ENABLE_MSW env var guard in MSWProvider for backend testing"

key-files:
  created:
    - aini-inu-frontend/src/app/global-error.tsx
    - aini-inu-frontend/src/app/error.tsx
    - aini-inu-frontend/src/components/common/ErrorBoundary.tsx
    - aini-inu-frontend/src/app/dashboard/error.tsx
    - aini-inu-frontend/src/app/around-me/error.tsx
    - aini-inu-frontend/src/app/feed/error.tsx
    - aini-inu-frontend/src/app/chat/error.tsx
    - aini-inu-frontend/src/app/chat/[id]/error.tsx
    - aini-inu-frontend/src/app/profile/[memberId]/error.tsx
    - aini-inu-frontend/src/app/settings/error.tsx
    - aini-inu-frontend/src/app/login/error.tsx
    - aini-inu-frontend/src/app/signup/error.tsx
  modified:
    - aini-inu-frontend/next.config.ts
    - aini-inu-frontend/src/mocks/MSWProvider.tsx

key-decisions:
  - "Used Next.js async rewrites() to proxy /api/v1/* to backend, keeping MSW bypass via onUnhandledRequest: bypass pattern intact"
  - "global-error.tsx uses inline Tailwind without component library imports since root layout is unavailable on crash"
  - "ErrorBoundary implemented as React class component (not functional) because getDerivedStateFromError/componentDidCatch require class component lifecycle"

patterns-established:
  - "Error boundary pattern: global-error.tsx + per-route error.tsx + reusable ErrorBoundary class component"
  - "API proxy pattern: NEXT_PUBLIC_API_PROXY_TARGET env var with localhost:8080 default"

requirements-completed: [BUG-01, BUG-02]

# Metrics
duration: 2min
completed: 2026-03-06
---

# Phase 1 Plan 01: Critical Bugs - Frontend Crash Prevention Summary

**Next.js API proxy to Spring Boot backend, MSW toggle via env var, and complete error boundary infrastructure covering global crash recovery and all 10 route directories**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-05T16:39:13Z
- **Completed:** 2026-03-05T16:41:23Z
- **Tasks:** 2
- **Files modified:** 14 (12 created, 2 modified)

## Accomplishments
- Added `rewrites()` to next.config.ts proxying all `/api/v1/*` requests to the Spring Boot backend via configurable NEXT_PUBLIC_API_PROXY_TARGET env var
- Fixed MSWProvider to respect NEXT_PUBLIC_ENABLE_MSW=false, allowing real backend integration testing without MSW interception
- Created global-error.tsx with its own html/body tags for root layout crash recovery (Next.js requirement)
- Created error.tsx boundaries for all 10 app route directories to prevent full-page crashes from unhandled exceptions
- Created reusable ErrorBoundary class component in components/common/ for section-level error wrapping in future phases

## Task Commits

Each task was committed atomically:

1. **Task 1: API proxy rewrites + MSW toggle fix** - `2a275a0` (feat)
2. **Task 2: Error boundaries -- global, per-route, and reusable component** - `06eff53` (feat)

**Plan metadata:** (pending docs commit)

## Files Created/Modified
- `aini-inu-frontend/next.config.ts` - Added async rewrites() routing /api/v1/* to localhost:8080
- `aini-inu-frontend/src/mocks/MSWProvider.tsx` - Added NEXT_PUBLIC_ENABLE_MSW !== 'false' guard
- `aini-inu-frontend/src/app/global-error.tsx` - Root layout error catch with own html/body tags and recovery UI
- `aini-inu-frontend/src/app/error.tsx` - Root route error boundary
- `aini-inu-frontend/src/components/common/ErrorBoundary.tsx` - Reusable React class error boundary with fallback prop and onError callback
- `aini-inu-frontend/src/app/dashboard/error.tsx` - Dashboard route error boundary
- `aini-inu-frontend/src/app/around-me/error.tsx` - Around-me route error boundary
- `aini-inu-frontend/src/app/feed/error.tsx` - Feed route error boundary
- `aini-inu-frontend/src/app/chat/error.tsx` - Chat list route error boundary
- `aini-inu-frontend/src/app/chat/[id]/error.tsx` - Chat room route error boundary
- `aini-inu-frontend/src/app/profile/[memberId]/error.tsx` - Profile route error boundary
- `aini-inu-frontend/src/app/settings/error.tsx` - Settings route error boundary
- `aini-inu-frontend/src/app/login/error.tsx` - Login route error boundary
- `aini-inu-frontend/src/app/signup/error.tsx` - Signup route error boundary

## Decisions Made
- Used Next.js async rewrites() for API proxying rather than a custom server, keeping zero-config deployment compatibility
- global-error.tsx avoids component library imports since the root layout (which provides ThemeProvider etc.) is unavailable during a root crash
- ErrorBoundary implemented as a React class component since React 19 functional components cannot implement getDerivedStateFromError / componentDidCatch

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Pre-existing lint errors (157 errors) exist across the codebase in unrelated files. None of the new files introduced additional lint errors. These are out of scope for this plan.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- API proxy is live: frontend can now call `/api/v1/*` and reach the Spring Boot backend at localhost:8080
- MSW can be disabled via `NEXT_PUBLIC_ENABLE_MSW=false` in .env.local for backend integration testing
- All routes have error boundaries preventing white-screen crashes
- ErrorBoundary component ready for Plan 02 to wrap intra-page sections
- No blockers for Phase 1 Plan 02 (type safety and runtime error fixes)

---
*Phase: 01-critical-bugs*
*Completed: 2026-03-06*

---
phase: 03-authentication
plan: 03
subsystem: auth
tags: [zustand, persist, hydration, interceptor, skipAuth, logout]

# Dependency graph
requires:
  - phase: 03-authentication
    provides: AuthProvider, useAuthStore, login/logout API, Sidebar component
provides:
  - Session persistence across browser refresh via deferred Zustand rehydration
  - Inline login error display (wrong credentials no longer cause full-page reload)
  - Functional logout button in desktop Sidebar
affects: [all authenticated pages, session management]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - skipHydration in Zustand persist to defer localStorage hydration to client-side useEffect
    - skipAuth: true on auth endpoints to bypass 401 interceptor for expected auth errors

key-files:
  created: []
  modified:
    - aini-inu-frontend/src/store/useAuthStore.ts
    - aini-inu-frontend/src/providers/AuthProvider.tsx
    - aini-inu-frontend/src/api/auth.ts
    - aini-inu-frontend/src/components/common/Sidebar.tsx

key-decisions:
  - "skipHydration: true in Zustand persist config defers hydration to explicit rehydrate() call in AuthProvider useEffect, avoiding SSR window.localStorage unavailability"
  - "skipAuth: true on login() prevents 401 interceptor from redirecting on wrong credentials, letting ApiError bubble to LoginForm catch block for inline display"
  - "skipAuth: true on logout() prevents refresh cycle when revoking the refresh token itself"
  - "Sidebar LogOut replaced Link with button calling useAuth().logout() — full token revocation + state clear + redirect"

patterns-established:
  - "Auth API calls that expect 401 responses (login, logout) use skipAuth: true to bypass the generic 401 handler"
  - "Zustand persist stores with SSR should use skipHydration: true and call rehydrate() inside useEffect"

requirements-completed: [AUTH-01, AUTH-03, AUTH-04]

# Metrics
duration: 8min
completed: 2026-03-06
---

# Phase 3 Plan 03: UAT Gap Closure Summary

**Three targeted fixes closing session-persistence, wrong-credential error display, and functional logout gaps from Phase 3 UAT.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-06T00:00:00Z
- **Completed:** 2026-03-06T00:08:00Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- Session now persists across browser refresh: Zustand store defers hydration with `skipHydration: true`, AuthProvider calls `rehydrate()` before reading tokens
- Wrong login credentials now show inline error: `login()` passes `skipAuth: true` so 401 bubbles to LoginForm catch block instead of triggering redirect
- Desktop Sidebar logout button now calls `useAuth().logout()` — revokes token server-side, clears Zustand state, redirects to /login

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix session lost on refresh** - `ae3b708` (fix)
2. **Task 2: Fix login error display** - `4b29000` (fix)
3. **Task 3: Wire logout button in Sidebar** - `9613c02` (fix)

## Files Created/Modified
- `aini-inu-frontend/src/store/useAuthStore.ts` - Added `skipHydration: true` to persist config
- `aini-inu-frontend/src/providers/AuthProvider.tsx` - Added `await useAuthStore.persist.rehydrate()` at start of bootstrap
- `aini-inu-frontend/src/api/auth.ts` - Added `skipAuth: true` to `login()` and `logout()` calls
- `aini-inu-frontend/src/components/common/Sidebar.tsx` - Replaced non-functional Link with button calling `useAuth().logout()`

## Decisions Made
- `skipHydration: true` pattern chosen over alternative (lazy init) because it's the canonical Zustand approach for Next.js SSR
- `skipAuth: true` on logout() as well as login() — logout is revoking the refresh token, so intercepting a 401 during logout would cause infinite refresh loop
- Sidebar uses `useAuth()` hook rather than calling auth store directly — keeps logout logic in one place (AuthProvider)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
Pre-existing lint errors in Sidebar.tsx (unused `Settings` import, `setMounted` in useEffect, `<img>` element) were present before our changes and are out of scope per deviation rules. All modified files pass TypeScript compilation and build succeeds.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All three UAT failures from Phase 3 are now closed
- Authentication flow is complete end-to-end: login, session persist, error display, logout
- Ready to proceed to next phase (domain features depend on auth being stable)

---
*Phase: 03-authentication*
*Completed: 2026-03-06*

## Self-Check: PASSED

All files exist and all task commits verified:
- ae3b708 (Task 1: skipHydration + rehydrate)
- 4b29000 (Task 2: skipAuth on login/logout)
- 9613c02 (Task 3: Sidebar logout button)

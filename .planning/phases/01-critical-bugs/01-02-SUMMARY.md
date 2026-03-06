---
phase: 01-critical-bugs
plan: 02
subsystem: ui
tags: [react, nextjs, polling, defensive-coding, api-audit]

# Dependency graph
requires:
  - phase: 01-critical-bugs/01-01
    provides: Error boundary infrastructure (global-error.tsx, ErrorBoundary component, Next.js rewrite proxy)
provides:
  - Infinite polling stopped by consecutive failure counting (useRadarLogic, chat page)
  - Null/undefined crash guards on all API-sourced property accesses
  - Loading fallback UI replacing blank null returns
  - Comprehensive API mismatch catalog for Phase 2 planning input
affects:
  - 02-common-infrastructure (uses catalog for fix planning)
  - Any phase touching chat, around-me, dashboard, feed, profile pages

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Consecutive failure counting with useRef for polling auto-stop (failCountRef >= 3 clears interval)
    - Polling interval stored in ref (pollingIntervalRef) for external clearability
    - typeof guards before Object.values/Object.entries on API data
    - Loading fallback over blank null returns

key-files:
  created:
    - .planning/phases/01-critical-bugs/01-API-MISMATCH-CATALOG.md
  modified:
    - aini-inu-frontend/src/hooks/useRadarLogic.ts
    - aini-inu-frontend/src/app/chat/[id]/page.tsx
    - aini-inu-frontend/src/app/layout.tsx
    - aini-inu-frontend/src/app/dashboard/page.tsx
    - aini-inu-frontend/src/app/around-me/page.tsx
    - aini-inu-frontend/src/app/feed/page.tsx
    - aini-inu-frontend/src/components/profile/ProfileView.tsx

key-decisions:
  - "Polling interval stored in ref (not closure) so failure counter in catch block can clear it reliably"
  - "Loading fallback used instead of null to prevent blank white screens during data loading"
  - "API mismatch catalog scoped to src/services/api/ only -- no code changes, documentation artifact only"
  - "Profile page defensive guards applied in ProfileView.tsx (actual logic) not profile/[memberId]/page.tsx (thin wrapper)"

patterns-established:
  - "Failure counting pattern: useRef(0), increment on catch, clearInterval + warn when >= 3, reset on success"
  - "Loading fallback pattern: <div className='flex items-center justify-center h-full'><p className='text-zinc-400'>Loading...</p></div>"

requirements-completed: [BUG-02, BUG-03]

# Metrics
duration: 15min
completed: 2026-03-06
---

# Phase 1 Plan 02: Defensive Patches and API Mismatch Catalog Summary

**Infinite polling stopped by consecutive failure counting (3-strike auto-stop) across useRadarLogic and chat page, null-access crash vectors patched with optional chaining and type guards, blank null returns replaced with loading UI, and 14-mismatch API catalog produced for Phase 2**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-03-05T16:30:00Z
- **Completed:** 2026-03-05T16:52:15Z
- **Tasks:** 2
- **Files modified:** 7 source files + 1 planning artifact created

## Accomplishments

- useRadarLogic.ts and chat/[id]/page.tsx stop polling after 3 consecutive failures instead of flooding the console indefinitely
- layout.tsx auth guard now rejects non-object localStorage data shapes (previously only caught JSON.parse exceptions)
- dashboard/page.tsx and around-me/page.tsx show loading fallback instead of returning null/blank screens
- feed/page.tsx and ProfileView.tsx have typeof guards before Object.values/Object.entries on API data
- chat/[id]/page.tsx has optional chaining on all room.partner property accesses
- API mismatch catalog documents 29 API calls with 14 mismatches across 5 service files, ready for Phase 2 planning

## Task Commits

1. **Task 1: Fix infinite polling and null-access crash vectors across all pages** - `53ca26f` (fix)
2. **Task 2: Produce API mismatch catalog for Phase 2** - `d5eb2e1` (feat)

## Files Created/Modified

- `aini-inu-frontend/src/hooks/useRadarLogic.ts` - Added failCountRef + pollingIntervalRef; polling auto-stops after 3 consecutive failures
- `aini-inu-frontend/src/app/chat/[id]/page.tsx` - Added pollFailCountRef + pollIntervalRef; message polling stops after 3 failures; optional chaining on room?.partner?.id and room?.partner?.nickname
- `aini-inu-frontend/src/app/layout.tsx` - Auth guard now also checks typeof db === 'object' && db !== null before accessing db.currentUserId
- `aini-inu-frontend/src/app/dashboard/page.tsx` - null return replaced with loading div; typeof guard on diaryRes.value before Object.values
- `aini-inu-frontend/src/app/around-me/page.tsx` - Both null returns replaced with loading divs
- `aini-inu-frontend/src/app/feed/page.tsx` - typeof guard before setFollowingDiaries
- `aini-inu-frontend/src/components/profile/ProfileView.tsx` - allDiaries type guard before Object.values; optional chaining on diary properties
- `.planning/phases/01-critical-bugs/01-API-MISMATCH-CATALOG.md` - 150-line catalog documenting all 29 API calls and 14 mismatches

## Decisions Made

- Polling interval ref stored separately from failure count ref so that the catch block in fetchData can access the interval ID even though the interval was created in a different effect callback scope.
- `profile/[memberId]/page.tsx` itself is a thin wrapper component -- the actual API call logic lives in `ProfileView.tsx`, so defensive guards were applied there instead.
- The API mismatch catalog is documentation-only (no code changes to service files) per the CONTEXT.md locked decision: Phase 1 adds only defensive wrapping, Phase 2 fixes the actual mismatches.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Pre-existing lint errors (245 total: `no-explicit-any`, `prefer-const`) exist across many files but are out-of-scope for this plan (they predate these changes and are not caused by them).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 2 has a comprehensive mismatch catalog with 14 categorized mismatches ready for fix planning
- The 5 most critical fixes for Phase 2 are: (1) add JWT auth headers to apiClient, (2) add patch() method, (3) fix /chat/rooms → /chat-rooms URL, (4) fix /members/me/dogs → /pets URL, (5) fix follow endpoint path
- All crash vectors from the Phase 1 research audit are now guarded

---
*Phase: 01-critical-bugs*
*Completed: 2026-03-06*

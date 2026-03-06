---
phase: 06-walk-threads
plan: "01"
subsystem: frontend/around-me
tags: [walk-threads, hook, gps, api-wiring, radar]
dependency_graph:
  requires: []
  provides: [useRadarLogic-v2, AroundMeHeader-refresh]
  affects: [around-me-page, radar-components]
tech_stack:
  added: []
  patterns: [GPS-with-fallback, manual-only-refresh, temporary-adapter]
key_files:
  created: []
  modified:
    - aini-inu-frontend/src/hooks/useRadarLogic.ts
    - aini-inu-frontend/src/app/around-me/page.tsx
    - aini-inu-frontend/src/components/around-me/AroundMeHeader.tsx
decisions:
  - "[06-01]: useRadarLogic rewritten to import exclusively from @/api/threads and @/api/pets -- all old service imports removed"
  - "[06-01]: isExpired() exported as standalone utility function using ISO 8601 Date.parse() not HH:mm split"
  - "[06-01]: Temporary adapters (ThreadMapResponse/ThreadSummaryResponse to legacy ThreadType) in page.tsx preserve existing RadarMapSection/RadarSidebar compatibility until Plan 03 rewire"
  - "[06-01]: EMERGENCY tab disabled with opacity-50 styling on tab button + overlay in content area rather than removing EMERGENCY components entirely"
metrics:
  duration: "3 min"
  completed_date: "2026-03-06"
  tasks_completed: 2
  files_modified: 3
---

# Phase 6 Plan 01: useRadarLogic Rewrite + Around-Me Rewire Summary

**One-liner:** GPS-first useRadarLogic with dual data fetch (threads+map+hotspots) from api/threads.ts, manual-only refresh, and disabled EMERGENCY overlay.

## What Was Built

Rewrote the `useRadarLogic` hook from scratch to use Phase 2 API modules. The old hook pulled data from `threadService`, `memberService`, and `locationService` (legacy service layer) with 10-second polling. The new hook uses `getThreads`, `getThreadMap`, `getHotspots`, `getThread`, and `deleteThread` from `@/api/threads` plus `getMyPets` from `@/api/pets`.

Key behavioral changes:
- **GPS acquisition** on mount using `navigator.geolocation.getCurrentPosition()` with 10s timeout; falls back to Seoul City Hall `[37.566295, 126.977945]` on error
- **No polling** — data fetches only when GPS ready (once on mount) and on manual `handleRefresh()` call
- **60s expiry timer** using `setInterval` for display clock only — not a data refetch trigger
- **Paginated sidebar** with `loadMore()` appending to `threadList`
- **Thread selection** via `selectThread(threadId)` calling `getThread(threadId)` for full detail

`AroundMeHeader` received a `재탐색` re-search button (RefreshCw icon, spins during refresh) and EMERGENCY tab visual muting.

`page.tsx` was rewired to consume the new hook shape with temporary adapters converting `ThreadMapResponse`/`ThreadSummaryResponse` to the legacy `ThreadType` shape expected by `RadarMapSection` and `RadarSidebar` — these adapters will be removed in Plan 03.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Rewrite useRadarLogic hook | f3cdf3d | src/hooks/useRadarLogic.ts |
| 2 | Rewire around-me page and disable EMERGENCY tab | cb3a851 | src/app/around-me/page.tsx, src/components/around-me/AroundMeHeader.tsx |

## Verification Results

- Build: PASSED (zero TypeScript errors, zero warnings)
- No old service imports in useRadarLogic.ts or page.tsx: CONFIRMED
- Only one `setInterval` in useRadarLogic.ts (60s expiry clock): CONFIRMED
- `navigator.geolocation.getCurrentPosition` present: CONFIRMED
- Seoul City Hall fallback `37.566295` / `126.977945` present: CONFIRMED
- Re-search button in AroundMeHeader: CONFIRMED
- EMERGENCY disabled overlay with "준비 중" message: CONFIRMED

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

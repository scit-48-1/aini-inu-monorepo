---
phase: 11-dashboard
plan: 01
subsystem: ui
tags: [react, typescript, dashboard, walk-stats, threads, heatmap]

requires:
  - phase: 02-common-infrastructure
    provides: API client and type modules (members.ts, threads.ts)
  - phase: 06-walk-threads
    provides: ThreadSummaryResponse, ThreadHotspotResponse types
  - phase: 04-member-profile-relations
    provides: WalkStatsResponse, MemberResponse types
provides:
  - DashboardHero accepting WalkStatsResponse for dynamic heatmap grid
  - AIBanner accepting ThreadHotspotResponse[] with max-count selection
  - LocalFeedPreview accepting ThreadSummaryResponse[] with thread cards
  - pointsToGridCounts utility for walk stats grid transformation
affects: [11-dashboard-plan-02]

tech-stack:
  added: []
  patterns: [duck-typed-props, internal-data-derivation, empty-state-handling]

key-files:
  created:
    - aini-inu-frontend/src/utils/walkStatsGrid.ts
  modified:
    - aini-inu-frontend/src/components/dashboard/DashboardHero.tsx
    - aini-inu-frontend/src/components/dashboard/AIBanner.tsx
    - aini-inu-frontend/src/components/dashboard/LocalFeedPreview.tsx
  deleted:
    - aini-inu-frontend/src/components/dashboard/DraftNotification.tsx

key-decisions:
  - "DashboardHero duck-typed userProfile prop works with both UserType and MemberResponse without importing either"
  - "Badge 'blue' variant replaced with 'indigo' to match Badge component's supported variants"
  - "LocalFeedPreview cards link to /around-me since no standalone thread detail page exists"

patterns-established:
  - "Internal data derivation: components accept raw API responses and compute display values internally (grassData, totalWalks, topHotspot)"
  - "Empty state with error fallback: optional error/onRetry props for per-section error handling"

requirements-completed: [DASH-01, DASH-02, DASH-03, DASH-05]

duration: 3min
completed: 2026-03-08
---

# Phase 11 Plan 01: Dashboard Section Components Summary

**Dashboard leaf components rewired from legacy types to canonical API response types with walk stats grid utility**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-07T18:12:07Z
- **Completed:** 2026-03-07T18:14:45Z
- **Tasks:** 2
- **Files modified:** 5 (3 modified, 1 created, 1 deleted)

## Accomplishments
- Created pointsToGridCounts utility that transforms WalkStatsResponse.points into grid-compatible number[] using date-based lookup
- Rewired DashboardHero to accept WalkStatsResponse and duck-typed userProfile, computing grassData/totalWalks internally
- Rewired AIBanner to accept ThreadHotspotResponse[] and select max-count hotspot internally with empty fallback
- Rewired LocalFeedPreview to ThreadSummaryResponse[] with proper thread cards (title, description, placeName, time range, participants)
- Deleted dead DraftNotification.tsx component

## Task Commits

Each task was committed atomically:

1. **Task 1: Create walkStatsGrid utility and rewire DashboardHero + AIBanner** - `2e59a1c` (feat)
2. **Task 2: Rewire LocalFeedPreview to ThreadSummaryResponse and delete DraftNotification** - `8271cc9` (feat)

## Files Created/Modified
- `src/utils/walkStatsGrid.ts` - Transforms WalkStatsResponse.points to flat number[] for CSS grid heatmap
- `src/components/dashboard/DashboardHero.tsx` - Accepts walkStats: WalkStatsResponse | null, duck-typed userProfile
- `src/components/dashboard/AIBanner.tsx` - Accepts hotspots: ThreadHotspotResponse[], selects max-count internally
- `src/components/dashboard/LocalFeedPreview.tsx` - Accepts ThreadSummaryResponse[] with status/chatType badges, empty state, error fallback
- `src/components/dashboard/DraftNotification.tsx` - Deleted (dead code)

## Decisions Made
- DashboardHero duck-typed userProfile prop works with both UserType and MemberResponse without importing either type
- Badge 'blue' variant replaced with 'indigo' to match Badge component's supported variants
- LocalFeedPreview cards link to /around-me since no standalone thread detail page exists
- formatTimeRange uses toLocaleTimeString with ko-KR locale for consistent Korean time display

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Badge variant 'blue' not supported by Badge component**
- **Found during:** Task 2 (LocalFeedPreview rewire)
- **Issue:** statusLabel() returned 'blue' variant for FULL status, but Badge only supports 'default' | 'amber' | 'emerald' | 'red' | 'indigo'
- **Fix:** Changed 'blue' to 'indigo' for FULL status badge
- **Files modified:** LocalFeedPreview.tsx
- **Verification:** TypeScript compiles cleanly (no errors in modified components)
- **Committed in:** 8271cc9 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Minor type correction for Badge component compatibility. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 3 dashboard section components accept canonical API response types
- page.tsx has expected TypeScript errors (old prop types) -- Plan 02 will rewire the page orchestrator
- walkStatsGrid utility ready for import by page.tsx

---
*Phase: 11-dashboard*
*Completed: 2026-03-08*

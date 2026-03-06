---
phase: 06-walk-threads
plan: 10
subsystem: ui
tags: [leaflet, reverse-geocoding, nominatim, map-drag, react]

requires:
  - phase: 06-walk-threads
    provides: DynamicMap component, RadarMapSection, around-me page
provides:
  - Free-drag map without snap-back bounds lock
  - onMoveEnd callback from DynamicMap to parent components
  - Reverse geocoding via Nominatim on map drag end
affects: [06-walk-threads]

tech-stack:
  added: [nominatim-reverse-geocoding]
  patterns: [moveend-callback-prop-drilling, nominatim-free-reverse-geocode]

key-files:
  modified:
    - aini-inu-frontend/src/components/common/DynamicMap.tsx
    - aini-inu-frontend/src/components/around-me/RadarMapSection.tsx
    - aini-inu-frontend/src/app/around-me/page.tsx

key-decisions:
  - "Nominatim for reverse geocoding: free, no API key, Korean addresses with accept-language=ko"
  - "onMoveEnd fires on every moveend event (including programmatic setView), acceptable for UX"

patterns-established:
  - "Map moveend callback: DynamicMap -> RadarMapSection -> page.tsx prop chain"

requirements-completed: [WALK-02]

duration: 2min
completed: 2026-03-07
---

# Phase 06 Plan 10: Free Map Drag + Reverse Geocoding Summary

**Free map dragging without snap-back via bounds lock removal, with Nominatim reverse geocoding updating location name on drag end**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-06T15:19:48Z
- **Completed:** 2026-03-06T15:21:39Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Removed setMaxBounds and maxBoundsViscosity from DynamicMap so users can freely drag the map
- Added onMoveEnd callback prop through DynamicMap -> RadarMapSection -> page.tsx
- Reverse geocoding via Nominatim updates location button in header after drag
- Search coordinates update on drag end so "재탐색" uses new location

## Task Commits

Each task was committed atomically:

1. **Task 1: Remove map bounds lock and add moveend callback** - `55bec36` (feat)
2. **Task 2: Wire moveend to reverse geocoding and location update** - `8e56910` (feat)

## Files Created/Modified
- `aini-inu-frontend/src/components/common/DynamicMap.tsx` - Removed bounds lock, added onMoveEnd prop and moveend event listener
- `aini-inu-frontend/src/components/around-me/RadarMapSection.tsx` - Added onMoveEnd prop passthrough to DynamicMap
- `aini-inu-frontend/src/app/around-me/page.tsx` - Added handleMapMoveEnd with Nominatim reverse geocoding

## Decisions Made
- Used Nominatim for reverse geocoding (free, no API key, 1 req/sec rate limit fine for drag-end)
- onMoveEnd callback fires lat/lng rounded to 6 decimal places
- Address extraction priority: borough > suburb > city_district > city > town > village

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed pre-existing TS2352 Address type cast**
- **Found during:** Task 2 (page.tsx compilation)
- **Issue:** DaumPostcode Address type cast to Record<string, string> failed strict type check
- **Fix:** Added double assertion via `as unknown as Record<string, string>`
- **Files modified:** aini-inu-frontend/src/app/around-me/page.tsx
- **Verification:** `npx tsc --noEmit` passes with 0 errors
- **Committed in:** 8e56910 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Pre-existing type error fixed to enable clean compilation. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Map drag and reverse geocoding complete
- All 10 plans in phase 06 are now complete

---
*Phase: 06-walk-threads*
*Completed: 2026-03-07*

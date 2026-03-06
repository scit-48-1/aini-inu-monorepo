---
status: diagnosed
trigger: "UAT Test 11: date range filter - date input triggers immediate server request + backend param type error + various improvement requests"
created: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:00:00Z
---

## Current Focus

hypothesis: Most reported issues were fixed in dc17eaf and 951e7d4; checking for remaining gaps
test: Code review of all relevant files
expecting: Identify what was fixed vs what remains
next_action: Return diagnosis

## Symptoms

expected: Header has date range UI. Start/end date + neighborhood + radius -> refetch button only. Radius 1~100km. Search centered on neighborhood setting. Map + sidebar show same results.
actual: (reported) Date input triggers immediate server request + backend param type error. Requested improvements: (1) filter only on refetch button, (2) radius 1-100km, (3) search from neighborhood center, (4) map+sidebar same results.
errors: Backend parameter type error on date params
reproduction: Change date inputs in header
started: Phase 06 UAT retest

## Evidence

- timestamp: 2026-03-06
  checked: WalkThreadController.java - date parameter types
  found: startDate/endDate are now `LocalDate` with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`. This resolves the original backend param type error.
  implication: FIXED - backend no longer chokes on date strings

- timestamp: 2026-03-06
  checked: useRadarLogic.ts - date change behavior
  found: `dateFrom`/`dateTo` are state vars with setters exposed. No useEffect watches them. Data fetch only happens via `handleRefresh()` (manual refetch) or initial GPS-ready load. Comment on line 183 says "DEC-029: only way to refetch".
  implication: FIXED - date input no longer triggers immediate server request

- timestamp: 2026-03-06
  checked: AroundMeHeader.tsx - radius UI
  found: Radius dropdown offers [1, 2, 3, 5, 10, 20, 50, 100] km options.
  implication: FIXED - radius range 1-100km is available

- timestamp: 2026-03-06
  checked: useRadarLogic.ts - searchCoordinates + handleRefresh
  found: `searchCoordinates` state (line 79) is set from DaumPostcode district lookup (page.tsx line 199). `handleRefresh` uses `searchCoordinates ?? coordinates` (line 187). `fetchThreadData` passes coords to both `getThreads` and `getThreadMap`.
  implication: FIXED - refetch uses neighborhood-set coordinates

- timestamp: 2026-03-06
  checked: page.tsx - effectiveCoordinates usage
  found: `effectiveCoordinates = searchCoordinates ?? coordinates` (line 53). Passed to `RadarMapSection` as `coordinates` prop (line 86) and to `RadarSidebar` as `coordinates` prop (line 106). Map centers on effectiveCoordinates.
  implication: FIXED - map centers on neighborhood setting location

- timestamp: 2026-03-06
  checked: fetchThreadData in useRadarLogic.ts - sidebar vs map data source
  found: Both `getThreads` (sidebar) and `getThreadMap` (map markers) are called in the same `fetchThreadData` with the same coords and radius. Both use location filter.
  implication: FIXED - map and sidebar show same location-filtered results

- timestamp: 2026-03-06
  checked: getThreads API call in fetchThreadData
  found: `getThreads` passes `startDate`/`endDate` params for date filtering. `getThreadMap` does NOT pass date params.
  implication: GAP - map markers are NOT date-filtered while sidebar list IS date-filtered

- timestamp: 2026-03-06
  checked: WalkThreadController.java getMapThreads endpoint
  found: `/threads/map` only accepts latitude, longitude, radius. No startDate/endDate parameters.
  implication: GAP - backend map endpoint has no date filter support

- timestamp: 2026-03-06
  checked: WalkThreadService.getThreads - location filtering
  found: Location filter is applied in-memory after DB query (lines 118-127). DB query (`findByStatusAndWalkDateRange`) does NOT filter by location - it only filters by date. Then Java-side stream filters by distance.
  implication: Potential performance concern but functionally correct. Date+location filtering works for sidebar.

- timestamp: 2026-03-06
  checked: loadMore function in useRadarLogic.ts (line 169-180)
  found: `loadMore` calls `getThreads` with `dateFrom`/`dateTo` but does NOT pass `latitude`, `longitude`, or `radius` params.
  implication: GAP - pagination (load more) does not apply location filter, so subsequent pages may include threads outside the radius

## Resolution

root_cause: See diagnosis below - most issues fixed, two gaps remain
fix: N/A (research only)
verification: N/A
files_changed: []

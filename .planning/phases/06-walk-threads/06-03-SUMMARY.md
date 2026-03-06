---
phase: 06-walk-threads
plan: "03"
subsystem: frontend/around-me
tags: [walk-threads, map, markers, hotspot, apply, sidebar, pagination]
dependency_graph:
  requires: [06-01, 06-02]
  provides: [around-me FIND tab fully functional, thread apply/cancel flow, hotspot markers]
  affects: [around-me page, RadarMapSection, RadarSidebar]
tech_stack:
  added: []
  patterns:
    - Seoul district coordinate lookup table for hotspot map markers
    - Inline pet selection flow within map popup
    - Haversine distance calculation in sidebar sort
key_files:
  created: []
  modified:
    - aini-inu-frontend/src/components/around-me/RadarMapSection.tsx
    - aini-inu-frontend/src/components/around-me/RadarSidebar.tsx
    - aini-inu-frontend/src/app/around-me/page.tsx
decisions:
  - "useUserStore profile.id is string; converted to Number(profile.id) for authorId comparison"
  - "Hotspot markers use DynamicMap; unmapped regions (non-Seoul) fall back to overlay panel"
  - "Sidebar sort state owned by RadarSidebar (not page.tsx) for encapsulation"
  - "Owner actions (edit/delete) only in map popup which has full ThreadResponse with authorId; removed from sidebar cards"
  - "loadMore wrapped in async handler in sidebar to track loading state"
metrics:
  duration_seconds: 186
  completed_date: "2026-03-06"
  tasks_completed: 2
  files_modified: 3
---

# Phase 06 Plan 03: FIND Tab Map + Sidebar Rewire Summary

**One-liner:** RadarMapSection and RadarSidebar fully rewired with ThreadMapResponse/ThreadSummaryResponse types, Seoul district hotspot markers, thread detail popup with apply/cancel/edit/delete, and paginated sidebar with distance and expiry badges.

## Tasks Completed

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| 1 | Rewrite RadarMapSection with new types, SEOUL_DISTRICT_COORDS, thread popup | 2b12bae | RadarMapSection.tsx |
| 2 | Rewrite RadarSidebar with ThreadSummaryResponse + wire page.tsx | 590db40 | RadarSidebar.tsx, page.tsx |

## What Was Built

### RadarMapSection.tsx (468 lines)

- **Props interface:** `coordinates`, `mapMarkers: ThreadMapResponse[]`, `hotspots: ThreadHotspotResponse[]`, `selectedThread: ThreadResponse | null`, `myPets`, `currentUserId`, `isExpired`, `onMarkerClick`, `onClearSelection`, `onDeleteThread`, `onEditThread`, `onRefreshDetail`
- **Thread markers:** `ThreadMapResponse[]` converted to `MapMarker[]` for DynamicMap
- **Hotspot markers:** `SEOUL_DISTRICT_COORDS` lookup table with all 25 Seoul districts; hotspots placed on map at district coordinates; unmapped regions shown in overlay panel with Flame icon
- **Hotspot click:** Shows popup with region name and count; does not trigger thread fetch
- **Thread detail popup:** Title, chatType badge, expiry badge, placeName+address, start-end time, remaining minutes, participant count
- **Owner actions:** Edit + Delete with confirmation dialog (isConfirmingDelete state)
- **Apply flow:** "산책 신청하기" button → pet multi-select cards → confirm → `applyToThread` → toast with chat navigation action → `onRefreshDetail()`
- **Cancel flow:** "신청 취소" button → `cancelApplication` → toast → `onRefreshDetail()`
- **Idempotent replay:** Treated as success
- **Chat navigation:** `useRouter` → `/chat/{chatRoomId}`

### RadarSidebar.tsx (230 lines)

- **Props interface:** `threads: ThreadSummaryResponse[]`, `hasNext`, `coordinates`, `currentTime`, `isExpired`, `currentUserId`, `onCardClick`, `onLoadMore`, `onDeleteThread`, `onEditThread`, `isLoading`
- **Thread cards:** title, description (truncated), placeName, distance via `calculateDistance`, remaining time badge (만료됨/XX분 남음), participant count, chatType badge, "참여 중" green badge when `applied || isApplied`
- **Sort toggle:** DISTANCE (Haversine sort) / TIME (ISO string lexicographic) — state owned by sidebar
- **Load-more button:** Shown when `hasNext` is true; disabled while loading; calls `onLoadMore`
- **Empty state:** Card with message "주변에 산책 스레드가 없어요" when no threads
- **Expired threads:** Shown with muted styling (opacity-60) and "만료됨" red badge

### page.tsx (152 lines)

- Removed all temporary adapters (`mapMarkersAsLegacy`, `threadListAsLegacy`)
- Added `useUserStore` import; `currentUserId = Number(profile.id)`
- Both components receive clean new prop interfaces
- Zero legacy `ThreadType` or `services/api` imports

## Decisions Made

1. **useUserStore profile.id is string:** The store maps `MemberResponse.id` to `String(member.id)` in `UserType`. Converting to `Number(profile.id)` for the `authorId === currentUserId` comparison in the popup.

2. **Hotspot markers on DynamicMap:** Per user decision (Phase 06 CONTEXT.md), hotspots are displayed as map markers. Seoul districts (25 wards) covered; any non-Seoul or unrecognized region names fall back to the unmapped overlay panel.

3. **Owner actions only in popup:** `ThreadSummaryResponse` lacks `authorId`, so ownership cannot be determined from list data. Per plan guidance, edit/delete removed from sidebar cards; they exist only in the detail popup which has full `ThreadResponse` with `authorId`.

4. **Sort state in sidebar:** The plan implied sort could be in sidebar or page; keeping it internal to `RadarSidebar` keeps coupling minimal.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] useUserStore uses `profile`, not `user`**
- **Found during:** Task 1 research
- **Issue:** Plan referenced `useUserStore((s) => s.user)` but the store exposes `s.profile` (mapped via `mapMemberToUser`); `profile.id` is a string, not a number
- **Fix:** Used `s.profile` and `Number(profile.id)` for numeric comparison with `authorId`
- **Files modified:** RadarMapSection.tsx, page.tsx

## Self-Check

Files exist:
- `/Users/keonhongkoo/Desktop/github/aini-inu/aini-inu-frontend/src/components/around-me/RadarMapSection.tsx` (468 lines)
- `/Users/keonhongkoo/Desktop/github/aini-inu/aini-inu-frontend/src/components/around-me/RadarSidebar.tsx` (230 lines)
- `/Users/keonhongkoo/Desktop/github/aini-inu/aini-inu-frontend/src/app/around-me/page.tsx` (152 lines)

Commits:
- `2b12bae` feat(06-03): rewrite RadarMapSection with ThreadMapResponse/ThreadResponse types
- `590db40` feat(06-03): rewrite RadarSidebar + wire page.tsx with new component interfaces

Build: `npm run build` passes with zero errors.

## Self-Check: PASSED

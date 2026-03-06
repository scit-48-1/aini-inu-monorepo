---
phase: 04-member-profile-relations
plan: 06
subsystem: ui
tags: [react, typescript, follow, members, profile]

# Dependency graph
requires:
  - phase: 04-member-profile-relations
    provides: OtherProfileView, NeighborsModal, useFollowToggle hook, members API
provides:
  - getFollowStatus(targetId) using GET /members/me/follows/{targetId}
  - getFollowers/getFollowing with optional memberId routing to /members/{memberId}/... or /members/me/...
  - OtherProfileView fetchFollowState using dedicated follow-status endpoint (survives refresh)
  - OtherProfileView followerCount from real API data (not hardcoded 0)
  - NeighborsModal memberId prop for viewing other member's followers/following lists
affects: [profile, follow, members]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "getFollowStatus(targetId): dedicated endpoint for follow state check instead of list scan"
    - "memberId-routed API functions: optional memberId param routes to /members/{id}/... or /members/me/..."

key-files:
  created: []
  modified:
    - aini-inu-frontend/src/api/members.ts
    - aini-inu-frontend/src/components/profile/OtherProfileView.tsx
    - aini-inu-frontend/src/components/profile/NeighborsModal.tsx

key-decisions:
  - "getFollowStatus uses dedicated GET /members/me/follows/{targetId} endpoint for O(1) exact follow state check, replacing O(n) list scan with size:100 cap"
  - "getFollowers/getFollowing accept optional memberId param; routes to /members/{memberId}/... when set, /members/me/... when absent (backward compatible)"
  - "followerCount fetched via getFollowers({ memberId, size:1000 }) asynchronously after profile load to avoid blocking main render"
  - "NeighborsModal memberId prop optional — MyProfileView passes none (routes to /me/...), OtherProfileView passes memberId (routes to /{id}/...)"

patterns-established:
  - "Follow state on OtherProfileView checked via getFollowStatus on mount — not list scan"
  - "NeighborsModal memberId prop controls whose follower/following list is shown"

requirements-completed: [MEM-03, MEM-04]

# Metrics
duration: 3min
completed: 2026-03-06
---

# Phase 04 Plan 06: Fix Follow State Persistence and NeighborsModal Member Routing Summary

**Follow state now uses dedicated GET /members/me/follows/{targetId} endpoint (survives refresh), followerCount loaded from real API, and NeighborsModal routes to correct member's followers/following list via optional memberId prop**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-06T02:27:29Z
- **Completed:** 2026-03-06T02:30:00Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Fixed follow state not persisting on refresh: replaced list-scan approach (getFollowing size:100) with dedicated getFollowStatus(targetId) endpoint
- Fixed followerCount hardcoded to 0: now fetched asynchronously from getFollowers({ memberId, size:1000 })
- Fixed NeighborsModal showing logged-in user's list when viewing others: added optional memberId prop that routes API calls to /members/{memberId}/... instead of /members/me/...

## Task Commits

Each task was committed atomically:

1. **Task 1: Add getFollowStatus + memberId param to getFollowers/getFollowing in members.ts** - `ef96432` (feat)
2. **Task 2: Fix OtherProfileView fetchFollowState + followerCount + NeighborsModal memberId** - `9349474` (feat)

**Plan metadata:** (docs: complete plan - follows this summary)

## Files Created/Modified
- `aini-inu-frontend/src/api/members.ts` - Added getFollowStatus(targetId); updated getFollowers/getFollowing to accept optional memberId routing parameter
- `aini-inu-frontend/src/components/profile/OtherProfileView.tsx` - fetchFollowState now uses getFollowStatus; followerCount loaded from getFollowers; NeighborsModal receives memberId prop
- `aini-inu-frontend/src/components/profile/NeighborsModal.tsx` - Added optional memberId prop; passed to getFollowers/getFollowing calls for correct member routing

## Decisions Made
- Used `res.following || res.isFollowing` to read FollowStatusResponse since backend uses both field names for the same semantic
- followerCount fetched asynchronously (fire-and-forget .then/.catch) to avoid blocking main profile render
- getFollowers/getFollowing backward-compatible: callers without memberId continue routing to /members/me/... unchanged

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None - pre-existing lint errors in unrelated files (any type, img element warnings) were not introduced by these changes and are out of scope.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Gap 2 (follow state persistence) and Gap 3 (NeighborsModal member routing) from UAT are resolved
- UAT items MEM-03 and MEM-04 requirements complete
- All three UAT gaps addressed across plans 04-05 and 04-06

---
*Phase: 04-member-profile-relations*
*Completed: 2026-03-06*

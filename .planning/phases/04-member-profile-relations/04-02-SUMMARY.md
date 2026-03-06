---
phase: 04-member-profile-relations
plan: 02
subsystem: ui
tags: [react, typescript, nextjs, follow, profile, pagination, optimistic-ui]

# Dependency graph
requires:
  - phase: 04-01
    provides: ProfileHeader with followerCount/followingCount props, OtherProfileView placeholder, api/members.ts module

provides:
  - OtherProfileView component with full other-member profile experience
  - useFollowToggle hook rewritten with optimistic update pattern and rollback on failure
  - NeighborsModal rewired to api/members.ts with SliceResponse pagination and load-more

affects:
  - Any future phase using follow/unfollow functionality
  - Phase 8 (chat) which will activate the MessageSquare button in NeighborsModal

# Tech tracking
tech-stack:
  added: []
  patterns:
    - optimistic-update-with-rollback: toggle state immediately, call callbacks, revert on failure + toast error
    - slice-response-pagination: fetch page 0 on open, append on load-more when hasNext is true

key-files:
  created:
    - aini-inu-frontend/src/components/profile/OtherProfileView.tsx
  modified:
    - aini-inu-frontend/src/hooks/useFollowToggle.ts
    - aini-inu-frontend/src/components/profile/NeighborsModal.tsx
    - aini-inu-frontend/src/components/profile/ProfileView.tsx

key-decisions:
  - "useFollowToggle targetId changed from string to number to match backend ID type"
  - "Optimistic follow toggle: state toggled immediately, callbacks fired immediately, rollback + error toast on API failure"
  - "Follow state for OtherProfileView determined by scanning getFollowing() list (getFollowers/Following only returns current user's list)"
  - "followingCount on OtherProfileView stays 0 (no API endpoint returns target member's following count)"
  - "NeighborsModal: SliceResponse.content accessed directly; load-more appends pages when hasNext is true"
  - "ProfileView.tsx adapted to Number(profile.id) to satisfy useFollowToggle's number parameter"

patterns-established:
  - "Optimistic UI: toggle → immediate callback → API call → revert on failure (no success toast, error-only toast)"
  - "SliceResponse pagination: page state, reset on tab change, append on load-more"

requirements-completed: [MEM-03, MEM-04, MEM-05, MEM-06, MEM-07, MEM-08, MEM-13]

# Metrics
duration: 4min
completed: 2026-03-06
---

# Phase 04 Plan 02: Other-Member Profile + Follow Toggle + NeighborsModal Summary

**OtherProfileView with optimistic follow/unfollow toggle, getMemberPets() pet list display, and NeighborsModal rewired to api/members.ts with SliceResponse load-more pagination**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-03-06T01:36:49Z
- **Completed:** 2026-03-06T01:41:00Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- OtherProfileView: fetches member profile via getMember(), pets via getMemberPets(), and follow state via getFollowing(); renders ProfileHeader (isMe=false), 3 tabs (DOGS populated, FEED/HISTORY empty states), and NeighborsModal integration
- useFollowToggle: rewritten with optimistic update pattern — state toggles immediately before API call, callbacks fire immediately for parent count updates, reverts on failure with error toast only
- NeighborsModal: replaced memberService/UserType with getFollowers/getFollowing from api/members.ts; renders MemberFollowResponse fields (profileImageUrl, nickname, followedAt); load-more pagination when SliceResponse.hasNext is true

## Task Commits

1. **Task 1: Create OtherProfileView + rewire useFollowToggle** - `028afc4` (feat)
2. **Task 2: Rewire NeighborsModal to api/members.ts with SliceResponse pagination** - `d7274e2` (feat)

## Files Created/Modified
- `aini-inu-frontend/src/components/profile/OtherProfileView.tsx` - Full other-member profile view with follow, pets, tabs, NeighborsModal
- `aini-inu-frontend/src/hooks/useFollowToggle.ts` - Optimistic follow toggle hook with number targetId and rollback
- `aini-inu-frontend/src/components/profile/NeighborsModal.tsx` - Paginated follower/following modal using api/members.ts
- `aini-inu-frontend/src/components/profile/ProfileView.tsx` - Adapted useFollowToggle call from string to number id

## Decisions Made
- Optimistic update: toggle state BEFORE API call, callback immediately, revert on failure. Removes success toasts (assumed success) and only toasts on error.
- Follow state detection: scans getFollowing() list (size: 100) to find if memberId is present — this is the current user's following list, which correctly answers "am I following this person?"
- followingCount stays 0 for other members — the API does not expose the target member's following count in any available endpoint.
- Load-more pagination resets list on tab change; appends content on page increment when hasNext is true.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed ProfileView.tsx useFollowToggle call from string to number id**
- **Found during:** Task 1 (useFollowToggle rewrite)
- **Issue:** ProfileView.tsx passed `profile?.id ?? ''` (string) to useFollowToggle, but hook signature changed to `number`
- **Fix:** Changed call to `profile?.id ? Number(profile.id) : 0` to satisfy number type
- **Files modified:** aini-inu-frontend/src/components/profile/ProfileView.tsx
- **Verification:** `npx tsc --noEmit` passes with zero errors
- **Committed in:** 028afc4 (Task 1 commit)

**2. [Rule 1 - Bug] PetResponse shape adaptation**
- **Found during:** Task 1 (OtherProfileView implementation)
- **Issue:** Initial code used `pet.profileImageUrl` and `pet.breed` as string, but PetResponse has `pet.photoUrl` and `pet.breed: BreedResponse` (object)
- **Fix:** Updated to `pet.photoUrl` and `pet.breed?.name ?? ''` in OtherProfilePets sub-component
- **Files modified:** aini-inu-frontend/src/components/profile/OtherProfileView.tsx
- **Verification:** `npx tsc --noEmit` passes with zero errors; build passes
- **Committed in:** 028afc4 (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (both Rule 1 - Bug)
**Impact on plan:** Both auto-fixes required for TypeScript correctness. No scope creep.

## Issues Encountered
- PetResponse interface has `photoUrl` (not `profileImageUrl`) and `breed: BreedResponse` (object with `.name`, not string) — adapted in OtherProfilePets sub-component

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Other-member profile experience fully functional with follow/unfollow and neighbor lists
- MEM-03 through MEM-08 and MEM-13 requirements satisfied
- Phase 04 Plan 03 (if any) can build on this foundation

---
*Phase: 04-member-profile-relations*
*Completed: 2026-03-06*

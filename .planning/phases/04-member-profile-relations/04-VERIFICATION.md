---
phase: 04-member-profile-relations
verified: 2026-03-06T03:00:00Z
status: passed
score: 16/16 must-haves verified
re_verification:
  previous_status: passed
  previous_score: 13/13
  gaps_closed:
    - "Own profile page loads without error (postService.getPosts() removed from Promise.all — Plan 04-05)"
    - "Follow state persists after page refresh (getFollowStatus dedicated endpoint used instead of list scan — Plan 04-06)"
    - "NeighborsModal shows correct member's followers/following (memberId prop added — Plan 04-06)"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Own profile page load after gap closure"
    expected: "Navigate to /profile/me — profile header, follower count, following count, and pet list render without error state. FEED tab shows empty state (acceptable)."
    why_human: "Code fix is verified statically; browser confirmation with real auth session needed to confirm 401 is resolved."
  - test: "Follow state persists after page refresh"
    expected: "After following a member and refreshing, the Follow button shows Unfollow state"
    why_human: "Requires live backend session to verify GET /members/me/follows/{targetId} returns correct state"
  - test: "NeighborsModal shows correct member's followers/following"
    expected: "Opening neighbors modal on another member's profile lists that member's followers, not the logged-in user's"
    why_human: "Requires multiple accounts and live backend to verify API routing is correct"
  - test: "Walk stats heatmap display"
    expected: "GitHub-style grid with color-coded cells renders when backend returns WalkStatsResponse data"
    why_human: "Cannot verify color rendering or date-indexed cell population without a running backend session"
  - test: "ProfileEditModal personality type multi-select"
    expected: "After opening modal, chips appear and toggling them updates personalityTypeIds in the PATCH payload"
    why_human: "Requires backend to return personality type master data; chip interaction needs browser verification"
  - test: "Follow/unfollow optimistic rollback"
    expected: "Clicking follow immediately shows following state; if API fails the state reverts and an error toast appears"
    why_human: "Requires simulating an API failure condition in browser"
---

# Phase 4: Member Profile & Relations Verification Report

**Phase Goal:** Users can view and edit their profile, browse other members, follow/unfollow, search members, and see walk activity stats
**Verified:** 2026-03-06T03:00:00Z
**Status:** passed
**Re-verification:** Yes — after UAT-driven gap closure (Plans 04-05 and 04-06)

## Context

The previous automated verification (score 13/13) declared the phase passed. UAT then exposed 3 runtime failures not caught by static analysis. Two gap-closure plans (04-05 and 04-06) were executed to fix them. This re-verification confirms all three fixes are present in the codebase and all 13 requirements remain satisfied.

---

## Gap Closure Verification

### Gap 1 (UAT Test 1): Own Profile Page Fails to Load

**Root cause:** `postService.getPosts(undefined)` was included in `Promise.all` inside `MyProfileView.fetchData`. The legacy `postService` uses `src/services/api/apiClient.ts` (no auth headers), causing GET /posts to return 401. The entire `Promise.all` rejected, showing the error state for all users viewing their own profile. `OtherProfileView` did not use `postService`, so other profiles loaded fine.

**Fix verified:**
- `MyProfileView.tsx` line 219 — `Promise.all` now contains only `[getMe(), getMyPets(), getFollowers({ size: 1000 }), getFollowing({ size: 1000 })]`
- The `postService` import is absent from the file (grep returns no results)
- The `posts` state defaults to `[]`; `setPosts` remains declared for future Phase 9 wiring

### Gap 2 (UAT Test 5): Follow State Not Persisting After Refresh

**Root cause:** `OtherProfileView.fetchFollowState` called `getFollowing({ size: 100 })` and scanned the result list for the target member ID. Users following more than 100 people would see `isFollowing` initialized to `false` regardless of actual follow state. The backend has `GET /members/me/follows/{targetId}` (returning `FollowStatusResponse`) which was unused.

**Fix verified:**
- `src/api/members.ts` line 158 — `getFollowStatus(targetId: number)` function exported, calling `GET /members/me/follows/${targetId}`
- `OtherProfileView.tsx` line 5 — imports `getFollowStatus` (and `getFollowers`) instead of `getFollowing`
- `OtherProfileView.tsx` lines 59-69 — `fetchFollowState` now calls `getFollowStatus(memberId)` and reads `res.following || res.isFollowing`
- No occurrence of `getFollowing.*size.*100` remains in `OtherProfileView.tsx`

### Gap 3 (UAT Test 6): NeighborsModal Shows Wrong Member's Lists

**Root cause:** `getFollowers()` and `getFollowing()` always called `/members/me/followers` and `/members/me/following`. `NeighborsModal` had no `memberId` prop, so opening it from `OtherProfileView` always showed the logged-in user's lists. Additionally, `followerCount` in `OtherProfileView` was hardcoded to `0`.

**Fix verified:**
- `src/api/members.ts` lines 136-148 — `getFollowers` and `getFollowing` accept optional `memberId?: number`. When provided, routes to `/members/${memberId}/followers`; when omitted, routes to `/members/me/followers`. Existing callers without `memberId` are unaffected.
- `NeighborsModal.tsx` line 19 — `memberId?: number` prop added to interface
- `NeighborsModal.tsx` lines 49-50 — `memberId` passed to `getFollowers`/`getFollowing` calls in `fetchUsers`
- `OtherProfileView.tsx` lines 155-160 — `<NeighborsModal memberId={memberId} .../>` passes the profile owner's ID
- `OtherProfileView.tsx` lines 48-50 — `getFollowers({ memberId, size: 1000 }).then(res => setFollowerCount(res.content.length))` fires asynchronously inside `fetchData` to initialize `followerCount` from real data

---

## Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | Own profile page loads without error when navigating to /profile/me or /profile/[own-id] | VERIFIED | `postService.getPosts()` absent from `Promise.all`; only `getMe`, `getMyPets`, `getFollowers`, `getFollowing` remain (MyProfileView.tsx:219) |
| 2  | Profile header, follower count, following count, and pet list render correctly | VERIFIED | All 4 data sources in `Promise.all` are authenticated via `@/api/client`; state set from response data |
| 3  | Posts tab shows empty state but does not cause the entire page to fail | VERIFIED | `posts` defaults to `[]`; `ProfileFeed` renders empty state when passed empty array |
| 4  | User can edit their profile via PATCH /members/me | VERIFIED | `ProfileEditModal` calls `updateMe()` with all `MemberProfilePatchRequest` fields |
| 5  | Walk activity stats render as a GitHub-style heatmap | VERIFIED | `transformWalkStats()` + `WalkHeatmap` component with `grid-flow-col grid-rows-7` and amber color mapping |
| 6  | Personality types are selectable multi-select chips in edit form | VERIFIED | `getPersonalityTypes()` called on modal open; chip multi-select with `personalityTypeIds` state |
| 7  | User can view another member's profile | VERIFIED | `getMember(memberId)` in `OtherProfileView`; `ProfileHeader` renders `MemberResponse` data |
| 8  | User can see another member's pet list | VERIFIED | `getMemberPets(memberId)` called with correct numeric ID |
| 9  | Follow state is determined by dedicated GET /members/me/follows/{targetId}, not list scan | VERIFIED | `getFollowStatus(memberId)` called in `fetchFollowState` (OtherProfileView.tsx:61) |
| 10 | Follow state persists after page refresh | VERIFIED | Dedicated endpoint gives authoritative answer on each mount, not bounded by list scan size |
| 11 | User can follow/unfollow with optimistic UI update and failure rollback | VERIFIED | `useFollowToggle` toggles state before API call, reverts + error toast on failure |
| 12 | NeighborsModal opened from OtherProfileView shows the target member's followers/following | VERIFIED | `memberId={memberId}` passed to `NeighborsModal`; routes to `/members/${memberId}/followers|following` |
| 13 | followerCount in OtherProfileView reflects actual backend count, not 0 | VERIFIED | `getFollowers({ memberId, size: 1000 }).then(res => setFollowerCount(res.content.length))` in fetchData |
| 14 | User can browse follower and following lists with pagination | VERIFIED | `NeighborsModal` fetches `SliceResponse`, appends pages on load-more when `hasNext` is true |
| 15 | User can search members with search-as-you-type (300ms debounce) | VERIFIED | `MemberSearchModal` with setTimeout/clearTimeout debounce; `searchMembers()` from api/members |
| 16 | Clicking a search result navigates to that member's profile | VERIFIED | `router.push('/profile/${member.id}')` + `onClose()` on result click |

**Score:** 16/16 truths verified

---

## Required Artifacts

| Artifact | Lines | Status | Details |
|----------|-------|--------|---------|
| `aini-inu-frontend/src/api/members.ts` | 174 | VERIFIED | `getFollowStatus` exported (line 158); `getFollowers`/`getFollowing` accept optional `memberId` (lines 136-148) |
| `aini-inu-frontend/src/components/profile/MyProfileView.tsx` | 421 | VERIFIED | `postService` absent; `Promise.all` has 4 authenticated calls; `posts` state defaults to `[]` |
| `aini-inu-frontend/src/components/profile/OtherProfileView.tsx` | 226 | VERIFIED | `getFollowStatus` used in `fetchFollowState`; `followerCount` from `getFollowers({ memberId })`; `memberId` passed to `NeighborsModal` |
| `aini-inu-frontend/src/components/profile/NeighborsModal.tsx` | 223 | VERIFIED | `memberId?: number` prop; `memberId` passed to `getFollowers`/`getFollowing` calls |
| `aini-inu-frontend/src/components/profile/ProfileHeader.tsx` | 158 | VERIFIED | Accepts `MemberResponse` directly; renders all profile fields |
| `aini-inu-frontend/src/components/profile/ProfileEditModal.tsx` | 369 | VERIFIED | All `MemberProfilePatchRequest` fields present; personality type multi-select |
| `aini-inu-frontend/src/hooks/useFollowToggle.ts` | 64 | VERIFIED | Optimistic update with rollback via `onFollow`/`onUnfollow` callbacks |
| `aini-inu-frontend/src/components/search/MemberSearchModal.tsx` | 192 | VERIFIED | `searchMembers()` with 300ms debounce |
| `aini-inu-frontend/src/app/profile/[memberId]/page.tsx` | 23 | VERIFIED | `isMe` routing; React 19 `use(params)` pattern |

---

## Key Link Verification

| From | To | Via | Status | Evidence |
|------|----|-----|--------|---------|
| MyProfileView.fetchData | GET /members/me | `getMe()` | VERIFIED | Import line 8; called in Promise.all |
| MyProfileView.fetchData | GET /pets | `getMyPets()` | VERIFIED | Import line 10; called in Promise.all |
| MyProfileView.fetchData | GET /members/me/followers | `getFollowers({ size: 1000 })` | VERIFIED | Called in Promise.all (line 222) |
| MyProfileView.fetchData | GET /members/me/stats/walk | `getWalkStats()` | VERIFIED | Called non-blocking after Promise.all |
| ProfileEditModal | PATCH /members/me | `updateMe()` | VERIFIED | Called in handleSave |
| ProfileEditModal | GET /member-personality-types | `getPersonalityTypes()` | VERIFIED | Called on modal open |
| OtherProfileView.fetchFollowState | GET /members/me/follows/{targetId} | `getFollowStatus(memberId)` | VERIFIED | Line 61 — dedicated endpoint, not list scan |
| OtherProfileView.fetchData | GET /members/{memberId}/followers | `getFollowers({ memberId, size: 1000 })` | VERIFIED | Lines 48-50 — async count fetch |
| OtherProfileView | GET /members/{memberId} | `getMember()` | VERIFIED | Import line 5; called in Promise.all |
| OtherProfileView | GET /members/{memberId}/pets | `getMemberPets()` | VERIFIED | Called with numeric memberId |
| useFollowToggle | POST/DELETE /members/me/follows/{targetId} | `follow()/unfollow()` | VERIFIED | Import in useFollowToggle.ts |
| NeighborsModal (from OtherProfileView) | GET /members/{memberId}/followers | `getFollowers({ memberId })` | VERIFIED | memberId prop passed through (lines 49-50) |
| NeighborsModal (from MyProfileView) | GET /members/me/followers | `getFollowers()` without memberId | VERIFIED | MyProfileView passes no memberId; routes to /me/ path |
| MemberSearchModal | GET /members/search | `searchMembers()` | VERIFIED | Import line 9; 300ms debounce |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|---------|
| MEM-01 | 04-01, 04-04, 04-05 | Own profile view (FR-MEMBER-001, MEM-ME-GET) | SATISFIED | `getMe()` + `getMyPets()` authenticated; `postService` removed so page loads |
| MEM-02 | 04-01 | Own profile edit (FR-MEMBER-001, MEM-ME-PATCH) | SATISFIED | `updateMe()` in ProfileEditModal |
| MEM-03 | 04-02, 04-06 | Other member profile view (FR-MEMBER-002, MEM-ID-GET) | SATISFIED | `getMember(memberId)` in OtherProfileView |
| MEM-04 | 04-02 | Other member pet list (FR-MEMBER-002, MEM-ID-PETS-GET) | SATISFIED | `getMemberPets(memberId)` in OtherProfileView |
| MEM-05 | 04-02, 04-06 | Follower list (FR-MEMBER-003, MEM-FOLLOWERS-GET) | SATISFIED | `getFollowers({ memberId })` in NeighborsModal — routes to correct member |
| MEM-06 | 04-02, 04-06 | Following list (FR-MEMBER-003, MEM-FOLLOWING-GET) | SATISFIED | `getFollowing({ memberId })` in NeighborsModal — routes to correct member |
| MEM-07 | 04-02 | Follow (FR-MEMBER-004, MEM-FOLLOWS-POST) | SATISFIED | `follow(targetId)` in useFollowToggle |
| MEM-08 | 04-02 | Unfollow (FR-MEMBER-004, MEM-FOLLOWS-DELETE) | SATISFIED | `unfollow(targetId)` in useFollowToggle |
| MEM-09 | 04-01 | Walk activity stats (FR-MEMBER-005, MEM-WALK-STATS-GET) | SATISFIED | `getWalkStats()` + WalkHeatmap component |
| MEM-10 | 04-03 | Member search (FR-MEMBER-006, MEM-SEARCH-GET) | SATISFIED | `searchMembers()` with 300ms debounce |
| MEM-11 | 04-01 | Personality types master (FR-MEMBER-007, MEM-PERSONALITY-TYPES-GET) | SATISFIED | `getPersonalityTypes()` in ProfileEditModal |
| MEM-12 | 04-01, 04-06 | Profile UI permissions, follow toggle failure recovery, pet empty state (PRD 8.3) | SATISFIED | `isMe` gate; optimistic rollback in useFollowToggle; empty dog state renders |
| MEM-13 | 04-02, 04-06 | Follow count + list public (DEC-010) | SATISFIED | followerCount from real API data; NeighborsModal shows correct member's list |

**Orphaned requirements:** None. All 13 requirements (MEM-01 through MEM-13) satisfied.

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| MyProfileView.tsx | 169 | `setPosts` assigned but never called | Info | Lint warning only; `posts` defaults to `[]`; deferred to Phase 9 intentionally |

No blockers or warnings that affect goal achievement. The `setPosts` warning is intentional technical debt documented in Plan 04-05.

---

## Human Verification Required

### 1. Own Profile Page Load (after gap closure)

**Test:** Log in, navigate to `/profile/me` or `/profile/[your-numeric-id]`.
**Expected:** Profile header with name, avatar, follower count, following count renders. No "프로필을 불러오는데 실패했습니다." error state. FEED tab shows empty state (not an error).
**Why human:** UAT confirmed this was broken; code fix is verified statically but browser confirmation with real auth session is needed.

### 2. Follow State Persistence After Refresh

**Test:** Navigate to another member's profile. Click Follow. Refresh the page.
**Expected:** The Follow button still shows "Unfollow" after refresh.
**Why human:** Requires live backend returning `GET /members/me/follows/{targetId}` with correct `following: true`.

### 3. NeighborsModal Shows Correct Member's Lists

**Test:** On another member's profile (not your own), click the follower count. Verify the modal lists people who follow THAT member, not the logged-in user.
**Expected:** Followers tab shows the profile owner's followers. Following tab shows who the profile owner follows.
**Why human:** Requires multiple accounts and live backend to verify API routing is correct in practice.

### 4. Walk Stats Heatmap Display

**Test:** Load `/profile/me` with a backend session that has walk diary entries within the last 90 days.
**Expected:** Heatmap grid renders with amber-shaded cells proportional to walk count per day; streak and success rate show non-zero values.
**Why human:** Cannot verify pixel-level rendering or correct date-indexed cell population without a live backend session.

### 5. ProfileEditModal Personality Type Chips

**Test:** Open the profile edit modal. Verify chips appear from `GET /member-personality-types`. Toggle chips. Save. Verify `personalityTypeIds` in PATCH request body (DevTools).
**Why human:** Requires live backend returning master data; chip interaction and network payload need browser DevTools verification.

### 6. Follow/Unfollow Optimistic Rollback

**Test:** In DevTools, block the follow API endpoint (Network tab — Block request URL). Click Follow on another member's profile.
**Expected:** Button immediately shows "following" state, then reverts to "follow" when the API fails, and an error toast appears.
**Why human:** Requires simulated API failure in browser DevTools.

---

## Summary

Three UAT-discovered runtime failures have been fixed in Plans 04-05 and 04-06:

**Gap 1 — MyProfileView 401 crash:** `postService.getPosts()` removed from `Promise.all`. Own profile now loads from 4 authenticated API calls. FEED tab shows empty state intentionally (posts API wiring deferred to Phase 9).

**Gap 2 — Follow state not persisting:** `OtherProfileView.fetchFollowState` now calls `getFollowStatus(memberId)` — a dedicated endpoint that gives authoritative follow status regardless of following list size. The old `getFollowing({ size: 100 })` list scan is gone.

**Gap 3 — NeighborsModal wrong member's lists:** `getFollowers`/`getFollowing` now accept optional `memberId` parameter. `NeighborsModal` receives `memberId` prop from `OtherProfileView` and passes it through, correctly fetching `/members/{memberId}/followers|following`. When viewing own profile, no `memberId` is passed, routing to `/members/me/...` as before (no regression).

All 13 requirements (MEM-01 through MEM-13) are implemented and wired. Six items require human browser verification with a live backend session.

---

_Verified: 2026-03-06T03:00:00Z_
_Verifier: Claude (gsd-verifier)_

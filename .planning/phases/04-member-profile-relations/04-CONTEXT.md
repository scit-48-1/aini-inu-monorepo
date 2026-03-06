# Phase 4: Member Profile/Relations - Context

**Gathered:** 2026-03-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewire all member profile and relations screens to the Phase 2 `api/members.ts` infrastructure. Users can view/edit their own profile, browse other members' profiles and pets, follow/unfollow, search members, and view walk activity stats. All 13 member endpoints aligned to Swagger spec.

**Modification scope:** aini-inu-frontend/ only. Backend and common-docs are read-only.

**API endpoints in scope:**
- `GET /api/v1/members/me` (FR-MEMBER-001, MEM-ME-GET)
- `PATCH /api/v1/members/me` (FR-MEMBER-001, MEM-ME-PATCH)
- `GET /api/v1/members/{id}` (FR-MEMBER-002, MEM-ID-GET)
- `GET /api/v1/members/{id}/pets` (FR-MEMBER-002, MEM-ID-PETS-GET)
- `GET /api/v1/members/me/followers` (FR-MEMBER-003, MEM-FOLLOWERS-GET)
- `GET /api/v1/members/me/following` (FR-MEMBER-003, MEM-FOLLOWING-GET)
- `POST /api/v1/members/me/follows/{targetId}` (FR-MEMBER-004, MEM-FOLLOWS-POST)
- `DELETE /api/v1/members/me/follows/{targetId}` (FR-MEMBER-004, MEM-FOLLOWS-DELETE)
- `GET /api/v1/members/me/stats/walk` (FR-MEMBER-005, MEM-WALK-STATS-GET)
- `GET /api/v1/members/search` (FR-MEMBER-006, MEM-SEARCH-GET)
- `GET /api/v1/member-personality-types` (FR-MEMBER-007, MEM-PERSONALITY-TYPES-GET)

**Applicable DEC:** DEC-010 (follow count + list public)

</domain>

<decisions>
## Implementation Decisions

### Component architecture (composition-patterns)
- Split current `ProfileView` into explicit variants: `MyProfileView` and `OtherProfileView`
- Each variant handles its own data fetching and permission logic (edit button vs follow button)
- Profile page orchestrator determines which variant to render based on `memberId` param
- Follow button state lifted to parent with optimistic update + failure rollback

### Profile data wiring
- Replace all `memberService` (old MSW-era) calls with `api/members.ts` functions
- Replace all `postService` / `threadService` calls with corresponding `api/*.ts` functions
- Map `MemberResponse` fields to UI: backend fields are the source of truth
- MSW-era fields not in backend response (e.g., `user.avatar`, `user.handle`, `user.about`, `user.location`) must be mapped from actual backend fields (`profileImageUrl`, `nickname`, `selfIntroduction`, etc.) or dropped
- `useFollowToggle` hook rewired from `memberService.follow/unfollow` to `api/members.ts follow/unfollow`

### Profile edit (FR-MEMBER-001)
- Only available on own profile (isMe check)
- Backend `PATCH /members/me` supports: nickname, profileImageUrl, linkedNickname, phone, age, gender, mbti, personality, selfIntroduction, personalityTypeIds
- Show all editable fields in ProfileEditModal
- Personality types fetched from `GET /member-personality-types` (FR-MEMBER-007) for selection UI

### Follow/unfollow (FR-MEMBER-004)
- Optimistic UI update: toggle immediately, increment/decrement count locally
- On API failure: revert toggle state + revert count + toast error
- Follow state detection: current approach fetches following list to check — acceptable since no direct "am I following?" endpoint exists
- DEC-010: follower/following counts and lists are publicly visible

### Follower/following lists (FR-MEMBER-003)
- NeighborsModal rewired to use `api/members.ts getFollowers/getFollowing`
- Paginated with SliceResponse
- Each item shows avatar, nickname, manner temperature, followedAt

### Walk activity stats (FR-MEMBER-005)
- Displayed on own profile
- Uses `GET /members/me/stats/walk` returning WalkStatsResponse with daily walk counts
- Visual representation: activity heatmap or simple stat cards

### Member search (FR-MEMBER-006)
- Search UI needed (no existing search page/component)
- Search via `GET /members/search?q={query}` with pagination
- Results show member avatar, nickname, manner temperature

### PRD SS8.3 UI/UX 5-state coverage
- Default: profile loaded with all sections visible
- Loading: spinner/skeleton while fetching profile data
- Empty: pet list empty state ("Register your first pet"), no posts state
- Error: profile fetch failed state with retry
- Success: toast on profile edit save, follow/unfollow actions

### Claude's Discretion
- Exact field layout in ProfileEditModal (form grouping, order)
- Walk stats visualization (heatmap vs cards vs chart)
- Member search placement (dedicated page vs modal vs sidebar section)
- Loading skeleton design
- Exact type mapping strategy (adapt UserType or create new MemberProfile type)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/members.ts`: All 13 endpoint functions already typed and ready (Phase 2)
- `ProfileView.tsx` (`src/components/profile/ProfileView.tsx`): Main profile component — needs split into MyProfileView/OtherProfileView
- `ProfileHeader.tsx`: Displays avatar, nickname, stats, follow button — reusable with props
- `ProfileEditModal.tsx`: Edit form modal — needs field expansion and API rewire
- `NeighborsModal.tsx`: Follower/following list modal — needs API rewire
- `useFollowToggle.ts`: Follow toggle hook — needs rewire to `api/members.ts` + optimistic rollback
- `useMemberProfile.ts`: Other member data hook — needs rewire to `api/members.ts`
- `useProfile.ts`: Own profile wrapper around useUserStore
- `ProfileDogs.tsx`, `ProfileFeed.tsx`, `ProfileHistory.tsx`: Tab content components
- `ProfileTabs.tsx`: Tab navigation (FEED/DOGS/HISTORY)
- `MannerScoreGauge.tsx`: Manner temperature display component
- `UserAvatar.tsx`: Avatar component with recent diary indicator

### Established Patterns
- `'use client'` on all pages and components
- Zustand useUserStore for own profile state
- Toast-only errors via sonner (Korean messages from backend)
- `cn()` utility for conditional classNames
- Composition pattern: explicit variant components (established in Phase 3 signup)

### Integration Points
- `app/profile/page.tsx`: Redirects to `/profile/me` — needs to render MyProfileView
- `app/profile/[memberId]/page.tsx`: Dynamic route — renders ProfileView with memberId
- `useUserStore.fetchProfile()`: Refreshes own profile after edit
- `api/members.ts`: Source of truth for all member API calls
- `api/pets.ts`: PetResponse type re-exported from members.ts

</code_context>

<specifics>
## Specific Ideas

- User specified explicit variant split: MyProfileView vs OtherProfileView (not boolean prop switching)
- Follow button must have optimistic update WITH failure rollback (not just optimistic)
- Main pet change mentioned as in-scope for PRD SS8.3 coverage — but PET-04 (main pet PATCH) is Phase 5; this phase handles displaying which is main
- 5-state coverage mandatory for all profile views (loading/empty/error/default/success)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 04-member-profile-relations*
*Context gathered: 2026-03-06*

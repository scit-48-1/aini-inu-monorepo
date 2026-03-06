# Phase 4: Member Profile/Relations - Research

**Researched:** 2026-03-06
**Domain:** Frontend profile/relations UI rewiring to backend API (Next.js 16 / React 19 / TypeScript)
**Confidence:** HIGH

## Summary

Phase 4 rewires all member profile and relations screens from the old MSW-era `memberService` (in `services/api/memberService.ts`) to the Phase 2 `api/members.ts` infrastructure. The scope is frontend-only: 11 distinct API endpoints across 13 requirements covering own profile view/edit, other member profiles, follow/unfollow, follower/following lists, member search, walk stats, and personality types.

The existing code has significant type mismatches: components use `UserType` (with `avatar`, `handle`, `about`, `location`, `mannerScore`, `followerCount`, `followingCount`) while the backend returns `MemberResponse` (with `profileImageUrl`, `selfIntroduction`, `mannerTemperature`, no `followerCount`/`followingCount`/`location`/`handle`). The core challenge is transitioning components to consume `MemberResponse` directly (as decided in CONTEXT.md) rather than maintaining the intermediate `UserType` mapping layer.

The `useUserStore` already has a `mapMemberToUser` function that bridges `MemberResponse` to `UserType`, but the CONTEXT.md decision mandates components use `MemberResponse` directly without adapter layers. This means ProfileHeader, ProfileView, ProfileEditModal, NeighborsModal, and related components must be updated to accept `MemberResponse` props.

**Primary recommendation:** Split ProfileView into MyProfileView and OtherProfileView as explicit variants, rewire all API calls from `memberService` to `api/members.ts`, and transition component props from `UserType` to `MemberResponse` with field mapping handled at the component level.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Split current `ProfileView` into explicit variants: `MyProfileView` and `OtherProfileView`
- Each variant handles its own data fetching and permission logic (edit button vs follow button)
- Profile page orchestrator determines which variant to render based on `memberId` param
- Follow button state lifted to parent with optimistic update + failure rollback
- Replace all `memberService` calls with `api/members.ts` functions
- Replace all `postService` / `threadService` calls with corresponding `api/*.ts` functions
- Map `MemberResponse` fields to UI: backend fields are the source of truth
- MSW-era fields not in backend response must be mapped from actual backend fields or dropped
- `useFollowToggle` hook rewired from `memberService.follow/unfollow` to `api/members.ts follow/unfollow`
- Profile edit only available on own profile (isMe check)
- Backend `PATCH /members/me` supports: nickname, profileImageUrl, linkedNickname, phone, age, gender, mbti, personality, selfIntroduction, personalityTypeIds
- Personality types fetched from `GET /member-personality-types` for selection UI
- Optimistic UI update for follow: toggle immediately, increment/decrement count locally; on failure revert + toast
- Follow state detection: fetch following list to check (no direct "am I following?" endpoint)
- DEC-010: follower/following counts and lists are publicly visible
- NeighborsModal rewired to use `api/members.ts getFollowers/getFollowing` with SliceResponse pagination
- Walk activity stats displayed on own profile using GitHub-style activity heatmap
- Member search: sidebar search button, click opens search modal with search-as-you-type + debounce
- Profile edit form: all editable fields in a single form (no section/tab separation)
- Type mapping: `UserType` MSW fields cleaned up to match `MemberResponse` backend fields
- Components use `MemberResponse` directly (no adapter layer)
- 5-state coverage mandatory for all profile views (loading/empty/error/default/success)

### Claude's Discretion
- Loading skeleton design
- Search modal internal layout and debounce interval
- ProfileEditModal field order and spacing
- Sidebar search button position (nav area vs bottom action area)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| MEM-01 | Own profile view (MEM-ME-GET) | `api/members.ts getMe()` already typed, returns `MemberResponse`. MyProfileView fetches via this. |
| MEM-02 | Own profile edit (MEM-ME-PATCH) | `api/members.ts updateMe()` accepts `MemberProfilePatchRequest`. ProfileEditModal needs field expansion + rewire. |
| MEM-03 | Other member profile view (MEM-ID-GET) | `api/members.ts getMember(id)` returns `MemberResponse`. OtherProfileView fetches via this. |
| MEM-04 | Other member pet list (MEM-ID-PETS-GET) | `api/members.ts getMemberPets(id)` returns `PetResponse[]`. Displayed in ProfileDogs tab. |
| MEM-05 | Follower list (MEM-FOLLOWERS-GET) | `api/members.ts getFollowers()` returns `SliceResponse<MemberFollowResponse>`. NeighborsModal rewire. |
| MEM-06 | Following list (MEM-FOLLOWING-GET) | `api/members.ts getFollowing()` returns `SliceResponse<MemberFollowResponse>`. NeighborsModal rewire. |
| MEM-07 | Follow (MEM-FOLLOWS-POST) | `api/members.ts follow(targetId: number)` returns `FollowStatusResponse`. useFollowToggle rewire. |
| MEM-08 | Unfollow (MEM-FOLLOWS-DELETE) | `api/members.ts unfollow(targetId: number)` returns `FollowStatusResponse`. useFollowToggle rewire. |
| MEM-09 | Walk stats (MEM-WALK-STATS-GET) | `api/members.ts getWalkStats()` returns `WalkStatsResponse` with `points[]` (date+count). Heatmap component exists in DashboardHero. |
| MEM-10 | Member search (MEM-SEARCH-GET) | `api/members.ts searchMembers(q, params)` returns `SliceResponse<MemberResponse>`. New search modal needed. |
| MEM-11 | Personality types master (MEM-PERSONALITY-TYPES-GET) | `api/members.ts getPersonalityTypes()` returns `MemberPersonalityTypeResponse[]`. Used in ProfileEditModal. |
| MEM-12 | Profile UI states (PRD SS8.3) | 5-state coverage: loading/empty/error/default/success per `AsyncState` type from `api/types.ts`. |
| MEM-13 | Follow count + list public (DEC-010) | No access control needed for follower/following counts/lists -- always visible. |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Next.js | 16 | App Router, routing | Project standard |
| React | 19 | UI framework | Project standard, `use()` hook for params |
| TypeScript | (project ver) | Type safety | Project standard |
| Tailwind CSS | 4 | Styling | Project standard |
| Zustand | (project ver) | State management (useUserStore) | Project standard |
| sonner | (project ver) | Toast notifications | Project standard |
| lucide-react | (project ver) | Icons | Project standard |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `api/members.ts` | Phase 2 | All member API calls | Every member endpoint call |
| `api/types.ts` | Phase 2 | SliceResponse, AsyncState | Pagination, 5-state UI |
| `api/client.ts` | Phase 2 | apiClient with auth interceptor | Underlying HTTP layer |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Direct MemberResponse in components | Keep UserType adapter in useUserStore | Decision: adapter layer removed per CONTEXT.md |
| Custom search page | Search modal in sidebar | Decision: modal approach per CONTEXT.md |

## Architecture Patterns

### Component Structure After Refactor
```
src/
├── app/profile/
│   ├── page.tsx                    # Redirects to /profile/me (existing)
│   └── [memberId]/
│       └── page.tsx                # Orchestrator: renders MyProfileView or OtherProfileView
├── components/profile/
│   ├── MyProfileView.tsx           # NEW: own profile variant (edit, walk stats)
│   ├── OtherProfileView.tsx        # NEW: other member variant (follow, pet list)
│   ├── ProfileHeader.tsx           # MODIFIED: accepts MemberResponse instead of UserType
│   ├── ProfileEditModal.tsx        # MODIFIED: expanded fields, api/members.ts rewire
│   ├── NeighborsModal.tsx          # MODIFIED: api/members.ts rewire, SliceResponse pagination
│   ├── ProfileDogs.tsx             # EXISTING: pet list display
│   ├── ProfileTabs.tsx             # EXISTING: tab navigation
│   ├── ProfileFeed.tsx             # EXISTING: feed tab (uses postService -- may be out of scope)
│   ├── ProfileHistory.tsx          # EXISTING: walk history tab
│   └── ProfileView.tsx             # DEPRECATED: replaced by My/OtherProfileView
├── components/search/
│   └── MemberSearchModal.tsx       # NEW: member search modal
├── hooks/
│   ├── useFollowToggle.ts          # MODIFIED: rewire to api/members.ts, optimistic rollback
│   ├── useMemberProfile.ts         # DEPRECATED or MODIFIED: replaced by direct API calls in OtherProfileView
│   └── useProfile.ts               # EXISTING: wraps useUserStore for own profile
└── store/
    └── useUserStore.ts             # MODIFIED: mapMemberToUser may be retained for backward compat
```

### Pattern 1: Explicit Variant Components (from vercel-composition-patterns skill)
**What:** Instead of `ProfileView` with `isMe` boolean prop switching behavior, create `MyProfileView` and `OtherProfileView` as explicit variants.
**When to use:** When a component's behavior diverges significantly based on a mode flag.
**Example:**
```typescript
// app/profile/[memberId]/page.tsx - Orchestrator
'use client';
import React, { use } from 'react';
import { useUserStore } from '@/store/useUserStore';
import { MyProfileView } from '@/components/profile/MyProfileView';
import { OtherProfileView } from '@/components/profile/OtherProfileView';

export default function ProfilePage({ params }: { params: Promise<{ memberId: string }> }) {
  const { memberId } = use(params);
  const myId = useUserStore((s) => s.profile?.id);
  const isMe = memberId === 'me' || memberId === String(myId);

  return isMe ? <MyProfileView /> : <OtherProfileView memberId={Number(memberId)} />;
}
```

### Pattern 2: Optimistic Follow Toggle with Rollback
**What:** Update UI immediately on follow/unfollow, then call API; on failure, revert state and show error toast.
**When to use:** Follow/unfollow button interactions.
**Example:**
```typescript
// hooks/useFollowToggle.ts - rewired version
import { useState, useEffect, useCallback } from 'react';
import { follow, unfollow } from '@/api/members';
import { toast } from 'sonner';

export function useFollowToggle(
  targetId: number,
  initialIsFollowing: boolean,
  options?: { onFollow?: () => void; onUnfollow?: () => void; onError?: () => void }
) {
  const [isFollowing, setIsFollowing] = useState(initialIsFollowing);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => { setIsFollowing(initialIsFollowing); }, [initialIsFollowing]);

  const toggle = useCallback(async () => {
    if (isLoading) return;
    const prev = isFollowing;
    // Optimistic update
    setIsFollowing(!prev);
    if (prev) options?.onUnfollow?.(); else options?.onFollow?.();
    setIsLoading(true);
    try {
      if (prev) await unfollow(targetId);
      else await follow(targetId);
    } catch {
      // Rollback
      setIsFollowing(prev);
      if (!prev) options?.onUnfollow?.(); else options?.onFollow?.();
      options?.onError?.();
      toast.error('팔로우 처리에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [isFollowing, isLoading, targetId, options]);

  return { isFollowing, isLoading, toggle };
}
```

### Pattern 3: Walk Stats Heatmap Data Transformation
**What:** Transform `WalkStatsResponse` (with `points: {date, count}[]`) into a flat array for the GitHub-style heatmap grid.
**When to use:** Profile walk stats section.
**Example:**
```typescript
// Transform WalkStatsResponse points to grassData array
function transformWalkStats(stats: WalkStatsResponse): number[] {
  const { startDate, points, windowDays } = stats;
  const start = new Date(startDate);
  const data = new Array(windowDays).fill(0);
  const dateMap = new Map(points.map(p => [p.date, p.count]));
  for (let i = 0; i < windowDays; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    const key = d.toISOString().slice(0, 10);
    data[i] = dateMap.get(key) ?? 0;
  }
  return data;
}
```

### Anti-Patterns to Avoid
- **Boolean prop switching in ProfileView:** Don't use `isMe` prop to change entire component behavior; use explicit variants (MyProfileView/OtherProfileView).
- **Intermediate UserType mapping everywhere:** Don't create adapter functions that convert MemberResponse to UserType for every component; let components consume MemberResponse directly.
- **Fetching entire following list for "am I following?" check:** While currently necessary (no dedicated endpoint), cache the result and avoid refetching on every render.
- **Non-optimistic follow toggle:** Current `useFollowToggle` waits for API response before updating UI; must change to optimistic + rollback pattern.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| API calls to member endpoints | Custom fetch calls | `api/members.ts` functions | Already typed, handles auth, envelope unwrapping |
| Pagination for follower/following lists | Custom page tracking | `SliceResponse<T>` from `api/types.ts` + `PaginationParams` | Consistent with all paginated endpoints |
| Toast notifications | Custom alert system | `sonner` toast | Project standard, Korean messages from backend |
| Debounced search | Custom timer management | `setTimeout` + cleanup in `useEffect` | Simple enough; no library needed for single use |
| Activity heatmap rendering | New heatmap library | Existing DashboardHero grass grid pattern | Already implemented with correct CSS grid layout |

**Key insight:** The `api/members.ts` module already has all 13 endpoint functions fully typed. The work is purely about rewiring existing UI components to these functions and fixing the type mismatches.

## Common Pitfalls

### Pitfall 1: Type ID Mismatch (string vs number)
**What goes wrong:** Existing code uses `string` IDs everywhere (`UserType.id: string`, `useFollowToggle(targetId: string)`), but backend uses `number` (`MemberResponse.id: number`, `follow(targetId: number)`).
**Why it happens:** Original MSW mock data used string IDs.
**How to avoid:** When transitioning to MemberResponse, ensure all ID comparisons use consistent types. The orchestrator page receives `memberId` as string from URL params -- convert with `Number(memberId)` when calling API functions.
**Warning signs:** Follow/unfollow 404 errors, profile not loading for numeric IDs.

### Pitfall 2: Gender Enum Mismatch
**What goes wrong:** Frontend `UserType.gender` uses `'M' | 'F'`, but backend `MemberResponse.gender` uses `'MALE' | 'FEMALE' | 'UNKNOWN'`.
**Why it happens:** MSW mock data used short codes.
**How to avoid:** Map backend values to display strings in the UI. Update ProfileEditModal gender selector to use `'MALE'`/`'FEMALE'` values.
**Warning signs:** Gender display showing raw enum values, edit form not pre-selecting correct gender.

### Pitfall 3: Missing followerCount/followingCount in MemberResponse
**What goes wrong:** `ProfileHeader` reads `user.followerCount` and `user.followingCount`, but `MemberResponse` from OpenAPI schema does NOT include these fields.
**Why it happens:** These counts may be computed server-side but aren't in the documented schema.
**How to avoid:** Check actual API response at runtime. If missing, consider: (a) fetching follower/following lists and using `.content.length` as an approximation (but paginated), or (b) tracking counts locally based on follow/unfollow actions. The current `UserType` has these fields hardcoded to 0 in `mapMemberToUser`.
**Warning signs:** Follower/following counts always showing 0.

### Pitfall 4: Old memberService Still Imported
**What goes wrong:** After rewiring, some components may still import from `services/api/memberService.ts`, causing duplicate API calls or using wrong URL patterns.
**Why it happens:** memberService uses different URLs (e.g., `/members/me/follow/{id}` vs correct `/members/me/follows/{id}`).
**How to avoid:** After rewiring, search codebase for all `memberService` imports and verify none remain in phase-4 touched files.
**Warning signs:** 404 errors on follow endpoints, PUT instead of PATCH on profile update.

### Pitfall 5: NeighborsModal Expects UserType[] but API Returns SliceResponse<MemberFollowResponse>
**What goes wrong:** NeighborsModal currently sets `users: UserType[]` from `memberService.getFollowers()` which returned `UserType[]`. The new `api/members.ts getFollowers()` returns `SliceResponse<MemberFollowResponse>`.
**Why it happens:** API response shape changed from flat array to paginated slice.
**How to avoid:** Access `.content` from the SliceResponse and map `MemberFollowResponse` fields (id, nickname, profileImageUrl, mannerTemperature, followedAt) to the list item display.
**Warning signs:** Empty follower/following list, TypeScript errors about missing properties.

### Pitfall 6: ProfileEditModal Field Mismatches
**What goes wrong:** Current ProfileEditModal uses MSW-era fields (`handle`, `avatar`, `about`, `location`) but backend accepts `profileImageUrl`, `selfIntroduction`, etc. Also missing fields: `linkedNickname`, `personality`, `personalityTypeIds`, `age`.
**Why it happens:** Original form was designed for mock data schema.
**How to avoid:** Map form state to `MemberProfilePatchRequest` shape. Add missing fields. Remove fields not in backend (handle, location via DaumPostcode -- `location` is not a backend field).
**Warning signs:** Edit form submitting wrong field names, 400 validation errors.

### Pitfall 7: Dashboard walkStats Data Shape Change
**What goes wrong:** Dashboard currently expects `memberService.getWalkStats()` to return `number[]` (flat array), but `api/members.ts getWalkStats()` returns `WalkStatsResponse` with `points: {date, count}[]`.
**Why it happens:** Different API implementations between MSW mock and real backend.
**How to avoid:** When reusing the heatmap in profile, transform `WalkStatsResponse.points` into the flat `number[]` format the grid expects, or update the grid to accept the structured data. Note: Dashboard page will also need updating but may be out of Phase 4 scope (it's Phase 11).
**Warning signs:** Heatmap showing all zeros or crashing on non-array data.

## Code Examples

### MemberResponse to Component Props Mapping
```typescript
// Field mapping reference: MemberResponse -> UI display
// Source: OpenAPI schema + api/members.ts types
{
  // Direct use
  id: member.id,                          // number (was string)
  email: member.email,
  nickname: member.nickname,
  memberType: member.memberType,          // 'PET_OWNER' | 'NON_PET_OWNER' | 'ADMIN'
  mbti: member.mbti,
  phone: member.phone,
  age: member.age,

  // Renamed fields
  profileImageUrl: member.profileImageUrl, // was: avatar
  selfIntroduction: member.selfIntroduction, // was: about
  mannerTemperature: member.mannerTemperature, // was: mannerScore

  // Enum mapping
  gender: member.gender,                   // 'MALE'|'FEMALE'|'UNKNOWN' (was: 'M'|'F')

  // New fields (not in old UserType)
  linkedNickname: member.linkedNickname,
  personality: member.personality,
  personalityTypes: member.personalityTypes, // MemberPersonalityTypeResponse[]
  status: member.status,                   // 'ACTIVE'|'INACTIVE'|'BANNED'
  nicknameChangedAt: member.nicknameChangedAt,
  verified: member.verified,
  isVerified: member.isVerified,

  // Dropped fields (not in backend)
  // handle -> dropped (use nickname)
  // location -> dropped (not in MemberResponse)
  // about -> use selfIntroduction
  // avatar -> use profileImageUrl
  // mannerScore -> use mannerTemperature
  // followerCount -> NOT in MemberResponse schema
  // followingCount -> NOT in MemberResponse schema
  // dogs -> NOT in MemberResponse (separate endpoint)
  // isOwner -> derive from memberType === 'PET_OWNER'
}
```

### ProfileEditModal Field Mapping to MemberProfilePatchRequest
```typescript
// Source: api/members.ts MemberProfilePatchRequest
const payload: MemberProfilePatchRequest = {
  nickname: form.nickname,           // optional, 30-day cooldown enforced by backend
  profileImageUrl: form.profileImageUrl,
  linkedNickname: form.linkedNickname,
  phone: form.phone,
  age: form.age,
  gender: form.gender,               // 'MALE' | 'FEMALE' | 'UNKNOWN'
  mbti: form.mbti,
  personality: form.personality,
  selfIntroduction: form.selfIntroduction,
  personalityTypeIds: form.personalityTypeIds, // number[] from personality type selector
};
```

### Search Modal with Debounce
```typescript
// Pattern for search-as-you-type with debounce
const [query, setQuery] = useState('');
const [results, setResults] = useState<MemberResponse[]>([]);
const [isSearching, setIsSearching] = useState(false);

useEffect(() => {
  if (!query.trim()) { setResults([]); return; }
  const timer = setTimeout(async () => {
    setIsSearching(true);
    try {
      const res = await searchMembers(query, { page: 0, size: 20 });
      setResults(res.content);
    } catch { /* toast error */ }
    finally { setIsSearching(false); }
  }, 300); // 300ms debounce
  return () => clearTimeout(timer);
}, [query]);
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `memberService` (services/api/) | `api/members.ts` (Phase 2) | Phase 2 | All member calls must use new module |
| `UserType` throughout | `MemberResponse` directly | Phase 4 (this phase) | Component props change |
| `memberService.getWalkStats() -> number[]` | `getWalkStats() -> WalkStatsResponse` | Phase 2 | Heatmap data transformation needed |
| `memberService.getFollowers() -> UserType[]` | `getFollowers() -> SliceResponse<MemberFollowResponse>` | Phase 2 | Pagination support, different response shape |
| `memberService.follow(string)` | `follow(number)` | Phase 2 | ID type change |
| `memberService.updateMe(PUT)` | `updateMe(PATCH)` | Phase 2 | HTTP method change, partial update semantics |

**Deprecated/outdated:**
- `services/api/memberService.ts`: Replaced by `api/members.ts`. Uses wrong URLs, wrong HTTP methods, wrong types.
- `UserType` for profile data: Being replaced by `MemberResponse` in component consumption. May remain in `useUserStore` for backward compatibility during transition.
- `react-daum-postcode` in ProfileEditModal: `location` field is not in `MemberResponse` or `MemberProfilePatchRequest`. The DaumPostcode integration should be removed from ProfileEditModal.

## Open Questions

1. **followerCount / followingCount missing from MemberResponse**
   - What we know: OpenAPI `MemberResponse` schema does not include `followerCount` or `followingCount` fields. Current `mapMemberToUser` hardcodes these to 0.
   - What's unclear: Does the actual API response include these fields despite not being in the schema? Or must they be fetched separately?
   - Recommendation: Test actual API response. If missing, consider fetching follower/following lists on profile load and using the response metadata (if total count is available in SliceResponse) or maintaining local state. The counts displayed in ProfileHeader may need to be managed locally with optimistic updates on follow/unfollow.

2. **ProfileFeed and ProfileHistory tabs -- which API?**
   - What we know: ProfileView currently uses `postService.getPosts(targetId)` for feed and `threadService.getWalkDiaries(memberId)` for history. These are not in the Phase 4 member endpoint scope.
   - What's unclear: Should these tabs be rewired in this phase or deferred?
   - Recommendation: Feed is Phase 9 (FR-COMMUNITY), Walk diary is Phase 7 (FR-WALK). For Phase 4, keep these tabs but use existing service calls or stub them with empty state. Focus rewiring on the member-specific endpoints only.

3. **useUserStore mapMemberToUser -- keep or remove?**
   - What we know: Components are transitioning to MemberResponse directly. But useUserStore is used broadly (Sidebar, AuthProvider, dashboard).
   - What's unclear: Can we fully remove UserType dependency in Phase 4?
   - Recommendation: Keep `useUserStore` with `mapMemberToUser` for now (it powers sidebar, dashboard, auth). Only Phase 4 profile components switch to MemberResponse directly. Full UserType deprecation is a cross-cutting concern for later phases.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | No test runner configured (per CLAUDE.md) |
| Config file | none |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| MEM-01 | Own profile loads via getMe() | manual-only | N/A -- no test runner | N/A |
| MEM-02 | Profile edit saves via updateMe() | manual-only | N/A | N/A |
| MEM-03 | Other profile loads via getMember() | manual-only | N/A | N/A |
| MEM-04 | Other member pets display | manual-only | N/A | N/A |
| MEM-05 | Follower list loads paginated | manual-only | N/A | N/A |
| MEM-06 | Following list loads paginated | manual-only | N/A | N/A |
| MEM-07 | Follow action with optimistic UI | manual-only | N/A | N/A |
| MEM-08 | Unfollow action with rollback | manual-only | N/A | N/A |
| MEM-09 | Walk stats heatmap renders | manual-only | N/A | N/A |
| MEM-10 | Member search returns results | manual-only | N/A | N/A |
| MEM-11 | Personality types load in edit | manual-only | N/A | N/A |
| MEM-12 | 5-state UI coverage | manual-only | N/A | N/A |
| MEM-13 | Follow counts publicly visible | manual-only | N/A | N/A |

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint`
- **Per wave merge:** `cd aini-inu-frontend && npm run build`
- **Phase gate:** Build green + manual UAT before `/gsd:verify-work`

### Wave 0 Gaps
None -- no test infrastructure to set up (frontend has no test runner per CLAUDE.md). Validation is via lint + build + manual UAT.

## Sources

### Primary (HIGH confidence)
- OpenAPI spec: `common-docs/openapi/openapi.v1.json` -- verified all 11 endpoint schemas, request/response types
- `api/members.ts` -- verified all 13 function signatures match OpenAPI spec
- `api/types.ts` -- verified SliceResponse, PaginationParams, AsyncState types
- Existing components: ProfileView.tsx, ProfileHeader.tsx, ProfileEditModal.tsx, NeighborsModal.tsx, useFollowToggle.ts, useMemberProfile.ts, useProfile.ts, useUserStore.ts -- all read and analyzed

### Secondary (MEDIUM confidence)
- `types/index.ts` UserType definition -- verified field mapping discrepancies with MemberResponse
- `services/api/memberService.ts` -- verified as deprecated (wrong URLs, wrong types)
- DashboardHero.tsx heatmap implementation -- verified grassData grid pattern for reuse

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries are already in use, no new dependencies
- Architecture: HIGH -- explicit variant pattern decided by user, code structure clear from existing files
- Pitfalls: HIGH -- all identified from direct code comparison between old (memberService/UserType) and new (api/members.ts/MemberResponse)
- Type mapping: HIGH -- verified against OpenAPI schema field-by-field

**Research date:** 2026-03-06
**Valid until:** 2026-04-06 (stable -- frontend-only refactoring, no external dependency changes)

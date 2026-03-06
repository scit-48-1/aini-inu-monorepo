# Phase 7: Walk Diary + Story - Research

**Researched:** 2026-03-07
**Domain:** Frontend rewire -- walk diary CRUD, following feed, story viewer (Next.js 16 / React 19 / TypeScript)
**Confidence:** HIGH

## Summary

Phase 7 is a frontend-only rewire phase. All 7 API endpoint functions and their TypeScript types already exist in `src/api/diaries.ts` and `src/api/community.ts` (built in Phase 2). The existing UI components (`ProfileHistory`, `StoryArea`, `DiaryBookModal`, `useWalkDiaries`, `useDiaryForm`) currently import from the OLD `threadService`/`postService` modules with legacy `WalkDiaryType` shapes. The work is: (1) rewire these components to use Phase 2 API modules with proper `WalkDiaryResponse`/`StoryGroupResponse` types, (2) add diary create/delete functionality, and (3) adapt the story viewer for the StoryGroupResponse shape with member-grouped auto-advance.

The data shape mismatch between `WalkDiaryType` (legacy: string id, `photos`, `place`, `partner`, `tags`, `isDraft`) and `WalkDiaryResponse` (API: number id, `photoUrls`, no place/partner/tags/isDraft) is the central challenge. Components must be rewritten to consume `WalkDiaryResponse` directly. Similarly, `StoryArea` currently expects a flat `Story[]` but must accept `StoryGroupResponse[]` from the API.

**Primary recommendation:** Rewire in dependency order -- hooks/data layer first (`useWalkDiaries`, `useDiaryForm`), then profile components (`ProfileHistory`, diary create modal), then feed components (`StoryArea`, story viewer in `DiaryBookModal`).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **CRUD main access**: Profile HISTORY tab -- diary list, detail view, create/edit/delete all accessible from here; '+' button in HISTORY tab opens create modal
- **Following feed**: Accessed via story viewer only (no separate card listing in feed)
- **Story row**: Top of feed page (Instagram pattern) -- icon row, click opens viewer
- **Diary detail viewer**: Same DiaryBookModal flipbook used everywhere (profile + feed story)
- **Story viewer UX**: Existing flipbook DiaryBookModal, auto-advance to next member, header with avatar + nickname + time, X button + background click to dismiss
- **Diary create/edit form**: Modal with fields: title, content (max 300 chars), photos (max 5, presigned URL), walkDate, isPublic toggle (default ON), threadId dropdown (optional)
- **Feed page composition**: Phase 7 rewires story area only; post area kept as-is for Phase 9
- **Visibility toggle (DEC-011)**: isPublic toggle default true; private hides from following feed and stories
- **5-state coverage (PRD SS8.3)**: Default/loading/empty/error/success for diary list and stories
- **Modification scope**: aini-inu-frontend/ only. Backend and common-docs are read-only.

### Claude's Discretion
- Exact skeleton/loading design
- Diary card design in HISTORY tab list view
- Empty state illustration and CTA copy
- Modal sizing and responsive breakpoints
- Thread dropdown label and empty state text

### Deferred Ideas (OUT OF SCOPE)
- Community feed posts rewire (Phase 9)
- Story creation UI (stories are auto-derived from diaries per DEC-022)
- Notification on new story (deferred to v2 NOTF-01/02)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DIARY-01 | Create diary -- content max 300 chars, default public (DEC-011) | `api/diaries.ts` createDiary() ready; WalkDiaryCreateRequest has title (required), content (required, max 300), walkDate (required), photoUrls (optional, max 5), isPublic (optional, default true), threadId (optional) |
| DIARY-02 | Diary list query | `api/diaries.ts` getDiaries() ready with SliceResponse pagination; rewire useWalkDiaries from threadService |
| DIARY-03 | Diary detail query | `api/diaries.ts` getDiary() ready; DiaryBookModal needs WalkDiaryResponse shape adaptation |
| DIARY-04 | Diary edit -- content max 300 chars | `api/diaries.ts` updateDiary() ready with PATCH semantics (null = no change); rewire useDiaryForm from threadService |
| DIARY-05 | Diary delete | `api/diaries.ts` deleteDiary() ready; add delete confirmation dialog in DiaryBookModal |
| DIARY-06 | Following diary feed | `api/diaries.ts` getFollowingDiaries() ready with SliceResponse; feeds into story viewer |
| DIARY-07 | Story query -- follower-scoped, member-grouped icons, 24h expiry | `api/community.ts` getStories() ready with SliceResponse<StoryGroupResponse>; StoryArea needs rewire from flat Story[] to StoryGroupResponse[] |
</phase_requirements>

## Standard Stack

### Core (Already Installed -- No New Dependencies)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Next.js | 16 | App Router framework | Project foundation |
| React | 19 | UI library | `use()` hook, `'use client'` pattern |
| TypeScript | 5.x | Type safety | Project standard |
| Tailwind CSS | 4 | Styling | Project standard |
| Zustand | Latest | State management | Project stores pattern |
| sonner | Latest | Toast notifications | Korean error/success messages |
| lucide-react | Latest | Icons | Project standard |

### Supporting (Already Available)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@/api/diaries.ts` | Phase 2 | All 6 diary API functions | All diary CRUD operations |
| `@/api/community.ts` | Phase 2 | getStories() | Story icon row data |
| `@/api/upload.ts` | Phase 2 | uploadImageFlow() | Presigned URL photo upload in diary create/edit |
| `@/api/types.ts` | Phase 2 | SliceResponse, PaginationParams, AsyncState | Pagination and state types |

### No Alternatives Needed
This phase uses exclusively existing project infrastructure. No new libraries required.

## Architecture Patterns

### Recommended Modification Structure
```
src/
├── api/
│   ├── diaries.ts           # READ ONLY -- already complete
│   └── community.ts         # READ ONLY -- already complete
├── hooks/
│   ├── useWalkDiaries.ts    # REWIRE -- from threadService to api/diaries
│   └── forms/
│       └── useDiaryForm.ts  # REWIRE -- from threadService to api/diaries
├── components/
│   ├── feed/
│   │   └── StoryArea.tsx    # REWIRE -- from flat Story[] to StoryGroupResponse[]
│   └── profile/
│       ├── ProfileHistory.tsx   # REWIRE -- from WalkDiaryType to WalkDiaryResponse
│       ├── DiaryBookModal.tsx   # REWIRE -- adapt to WalkDiaryResponse + StoryGroupResponse
│       ├── DiaryCreateModal.tsx # NEW -- diary create/edit modal
│       └── DiaryModal/
│           └── DiaryPageRenderer.tsx # REWIRE -- from WalkDiaryType to WalkDiaryResponse
├── app/
│   └── feed/
│       └── page.tsx         # REWIRE -- story area data flow only
└── ...MyProfileView.tsx     # REWIRE -- diary data flow + create button
```

### Pattern 1: Hook Rewire (useWalkDiaries)
**What:** Replace old `threadService.getWalkDiaries()` (returns `Record<string, WalkDiaryType>`) with `getDiaries()` from `@/api/diaries` (returns `SliceResponse<WalkDiaryResponse>`)
**When to use:** Diary list in profile HISTORY tab
**Key change:** Data goes from `Record<string, WalkDiaryType>` to `WalkDiaryResponse[]` (via `SliceResponse.content`)
```typescript
// Source: Existing project pattern from Phase 6
import { getDiaries } from '@/api/diaries';
import type { WalkDiaryResponse } from '@/api/diaries';
import type { SliceResponse } from '@/api/types';

export function useWalkDiaries() {
  const [diaries, setDiaries] = useState<WalkDiaryResponse[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  const fetchDiaries = useCallback(async (pageNum = 0) => {
    setIsLoading(true);
    try {
      const res = await getDiaries({ page: pageNum, size: 20 });
      if (pageNum === 0) setDiaries(res.content);
      else setDiaries(prev => [...prev, ...res.content]);
      setHasNext(res.hasNext);
      setPage(pageNum);
    } catch (e) {
      // error handled by apiClient toast
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { diaries, isLoading, hasNext, fetchDiaries, loadMore: () => fetchDiaries(page + 1) };
}
```

### Pattern 2: StoryArea Rewire
**What:** Replace flat `Story[]` interface with `StoryGroupResponse[]` from `@/api/community`
**When to use:** Feed page story icon row
**Key change:** Props change from `Story { id, user: { nickname, avatar }, image }` to `StoryGroupResponse { memberId, nickname, profileImageUrl, coverImageUrl, diaries[] }`
```typescript
// StoryArea props change
interface StoryAreaProps {
  storyGroups: StoryGroupResponse[];
  onStoryClick: (group: StoryGroupResponse) => void;
  isLoading?: boolean;
  isEmpty?: boolean;
}
```

### Pattern 3: DiaryBookModal for Story Viewer
**What:** Extend DiaryBookModal to handle StoryGroupResponse[] (multiple members, auto-advance)
**When to use:** When clicking a story icon in feed
**Key change:** Currently takes `Record<string, WalkDiaryType> | WalkDiaryType[]`; needs to accept `StoryGroupResponse[]` for grouped member navigation, or flatten `StoryGroupResponse[].diaries` into a sequential list with member transition markers

### Pattern 4: Diary Create Modal
**What:** New modal for diary creation/editing
**When to use:** '+' button in HISTORY tab, or edit action in DiaryBookModal
**Key fields:** title, content (max 300 with char counter), photos (max 5 via presigned URL), walkDate (date picker), isPublic toggle (default true), threadId dropdown (optional)
**Reference modals:** CreatePostModal, ProfileEditModal patterns

### Anti-Patterns to Avoid
- **Keeping WalkDiaryType dependency:** Components MUST consume WalkDiaryResponse directly. Do NOT create adapter layers mapping WalkDiaryResponse back to WalkDiaryType.
- **Mixing old and new API modules:** Do NOT import from both `threadService` and `api/diaries` in the same component. Each component must be fully migrated.
- **Client-side 24h expiry filtering:** Stories are already filtered server-side by the `/stories` endpoint (DEC-024). Do NOT add client-side expiry logic.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Image upload | Custom multipart upload | `uploadImageFlow()` from `@/api/upload.ts` | Presigned URL flow already handles token extraction, upload, and returns imageUrl |
| Pagination | Custom offset tracking | `SliceResponse` + `hasNext` pattern from Phase 6 | Already established in threads, consistent UX |
| Error toasts | Per-component error handling | `apiClient` auto-toast from Phase 2 | Korean messages already mapped, consistent behavior |
| 24h story expiry | Client-side Date.now() filtering | Server-side `/stories` endpoint | Backend handles expiry per DEC-024; client just renders what API returns |
| Content max length validation | Custom regex/length check | `maxLength` attribute on textarea + char counter | Backend validates at 300 chars; frontend provides UX feedback |

**Key insight:** The Phase 2 infrastructure (api modules, types, apiClient) handles nearly all data concerns. This phase is primarily about rewiring existing components to consume the correct data shapes.

## Common Pitfalls

### Pitfall 1: WalkDiaryResponse field naming differences
**What goes wrong:** WalkDiaryResponse uses `photoUrls` but legacy WalkDiaryType uses `photos`. Components using `data.photos` will get `undefined`.
**Why it happens:** Direct property access without adapting field names.
**How to avoid:** Find-and-replace all `data.photos` references to `data.photoUrls` in rewired components. Same for: `data.place` (doesn't exist in WalkDiaryResponse), `data.partner` (doesn't exist), `data.tags` (doesn't exist), `data.isDraft` (doesn't exist).
**Warning signs:** `undefined` rendering in photos grid, missing location text.

### Pitfall 2: ID type mismatch (string vs number)
**What goes wrong:** WalkDiaryType uses `id: string`, WalkDiaryResponse uses `id: number`. Components doing `diary.id === selectedDiaryId` will fail if types differ.
**Why it happens:** Legacy type used string IDs throughout.
**How to avoid:** Use `WalkDiaryResponse.id` (number) consistently. Update all state variables holding diary IDs to `number | null`.
**Warning signs:** Selected diary not highlighting, modal not opening for correct diary.

### Pitfall 3: WalkDiaryResponse has both `public` and `isPublic` fields
**What goes wrong:** The OpenAPI schema shows both `public` (boolean) and `isPublic` (boolean) on WalkDiaryResponse. The `api/diaries.ts` type reflects this correctly.
**Why it happens:** Java boolean getter naming (`isPublic` field produces both `getPublic()` and `isPublic()` in JSON serialization).
**How to avoid:** Use `isPublic` consistently as the semantic field. Ignore `public`.
**Warning signs:** Visibility toggle showing wrong state.

### Pitfall 4: DiaryBookModal data shape incompatibility
**What goes wrong:** DiaryBookModal currently accepts `Record<string, WalkDiaryType> | WalkDiaryType[]`. After rewire, profile passes `WalkDiaryResponse[]` and feed story passes `StoryGroupResponse[]` (different shapes).
**Why it happens:** DiaryBookModal serves two contexts with different data sources.
**How to avoid:** Either (a) normalize both inputs to `WalkDiaryResponse[]` before passing (flatten StoryGroupResponse.diaries with member info attached), or (b) create a separate StoryViewerModal for the feed story context.
**Warning signs:** Story viewer not navigating between members correctly.

### Pitfall 5: isPublic default must be true (DEC-011)
**What goes wrong:** `useDiaryForm` currently initializes `isPublic: false`. DEC-011 requires default public.
**Why it happens:** Legacy code had private default.
**How to avoid:** Set initial state `isPublic: true` in the form hook. The `WalkDiaryCreateRequest.isPublic` field is optional; backend defaults to true if omitted.
**Warning signs:** New diaries being private by default.

### Pitfall 6: Feed page story area must NOT touch post area
**What goes wrong:** Accidentally modifying the post feed section while rewiring the story area.
**Why it happens:** `feed/page.tsx` contains both story and post logic interleaved.
**How to avoid:** Only modify: story fetch logic (replace `postService.getStories()` with `getStories()` from `api/community`), StoryArea props, and DiaryBookModal usage. Leave `fetchPosts`, `FeedItem`, `CreatePostModal` completely untouched.
**Warning signs:** Post feed breaking or showing different data.

### Pitfall 7: Missing fields in DiaryPageRenderer after rewire
**What goes wrong:** DiaryPageRenderer references `data.lat`, `data.lng`, `data.place`, `data.participatingDogs`, `data.tags` -- none of which exist in `WalkDiaryResponse`.
**Why it happens:** WalkDiaryResponse is a simpler DTO than the legacy WalkDiaryType.
**How to avoid:** Remove or conditionally render map section (no lat/lng available), remove participatingDogs section, remove tags/neighbors section. Focus on: title, content, photoUrls, walkDate, isPublic.
**Warning signs:** Map rendering at default Seoul coordinates for every diary, empty dog/neighbor sections.

## Code Examples

### Diary Create Modal Form (verified against OpenAPI schema)
```typescript
// Source: OpenAPI WalkDiaryCreateRequest schema
interface DiaryFormState {
  title: string;       // required, 1-120 chars
  content: string;     // required, 1-300 chars
  photoUrls: string[]; // optional, 0-5 items
  walkDate: string;    // required, YYYY-MM-DD format
  isPublic: boolean;   // optional, default true (DEC-011)
  threadId?: number;   // optional
}

// Create request
const request: WalkDiaryCreateRequest = {
  title: form.title,
  content: form.content,
  photoUrls: form.photoUrls.length > 0 ? form.photoUrls : undefined,
  walkDate: form.walkDate,
  isPublic: form.isPublic,
  threadId: form.threadId || undefined,
};
const created = await createDiary(request);
```

### StoryArea Rewire (from flat to grouped)
```typescript
// Source: api/community.ts StoryGroupResponse type
import { getStories } from '@/api/community';
import type { StoryGroupResponse } from '@/api/community';

// In feed page:
const [storyGroups, setStoryGroups] = useState<StoryGroupResponse[]>([]);

const fetchStories = async () => {
  try {
    const res = await getStories({ page: 0, size: 20 });
    setStoryGroups(res.content);
  } catch (e) { /* apiClient handles toast */ }
};

// StoryArea maps each StoryGroupResponse to an icon:
// group.profileImageUrl -> avatar
// group.nickname -> label
// group.diaries.length -> badge count (optional)
```

### Presigned URL Photo Upload in Diary Form
```typescript
// Source: api/upload.ts uploadImageFlow
import { uploadImageFlow } from '@/api/upload';

const handlePhotoUpload = async (file: File) => {
  if (form.photoUrls.length >= 5) {
    toast.error('사진은 최대 5장까지 첨부할 수 있습니다.');
    return;
  }
  try {
    const imageUrl = await uploadImageFlow(file, 'WALK_DIARY');
    setForm(prev => ({ ...prev, photoUrls: [...prev.photoUrls, imageUrl] }));
  } catch (e) {
    toast.error('사진 업로드에 실패했습니다.');
  }
};
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `threadService.getWalkDiaries()` returns `Record<string, WalkDiaryType>` | `getDiaries()` returns `SliceResponse<WalkDiaryResponse>` | Phase 2 | Array-based with pagination support |
| `threadService.saveWalkDiary()` single save method | Separate `createDiary()` / `updateDiary()` / `deleteDiary()` | Phase 2 | Proper REST CRUD |
| `postService.getStories()` returns `any[]` | `getStories()` returns `SliceResponse<StoryGroupResponse>` | Phase 2 | Typed, paginated, member-grouped |
| `WalkDiaryType` with string ID, `photos`, `place` | `WalkDiaryResponse` with number ID, `photoUrls`, no place | Phase 2 | Must adapt all component references |

**Deprecated/outdated:**
- `services/api/threadService.ts` diary methods: replaced by `api/diaries.ts`
- `services/api/postService.ts` getStories: replaced by `api/community.ts` getStories
- `types/index.ts` WalkDiaryType: replaced by `WalkDiaryResponse` from `api/diaries.ts`
- `types/index.ts` DiaryFormValues: replaced by inline form state matching `WalkDiaryCreateRequest`

## Open Questions

1. **DiaryBookModal dual-purpose (profile vs story viewer)**
   - What we know: Currently serves both profile diary detail and feed story viewing. Profile passes `WalkDiaryResponse[]`, feed story needs `StoryGroupResponse[]` with member transitions.
   - What's unclear: Whether to extend existing modal or create a separate StoryViewerModal.
   - Recommendation: Extend DiaryBookModal with a `mode` prop (`'profile' | 'story'`). In story mode, accept `StoryGroupResponse[]` and handle member auto-advance. This minimizes code duplication while keeping behavior clear.

2. **Thread dropdown data source for diary create**
   - What we know: Diary create has optional threadId linking to a walk thread. User decided it's a dropdown showing "user's participated threads."
   - What's unclear: Which API provides the user's participated threads list.
   - Recommendation: Use `getThreads()` from `api/threads.ts` with appropriate filtering, or simply allow threadId text input. The field is optional, so a minimal approach works.

3. **DiaryPageRenderer simplification**
   - What we know: Current renderer shows map, participating dogs, neighbors -- none available in `WalkDiaryResponse`.
   - What's unclear: How much of the existing layout to preserve vs simplify.
   - Recommendation: Remove map/dogs/neighbors sections. Focus on title, content, photos, walkDate, isPublic. Keep the flipbook aesthetic but with simpler content.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | No frontend test runner configured (per CLAUDE.md) |
| Config file | none |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DIARY-01 | Diary create with content max 300 | manual-only | `npm run build` (type check) | N/A |
| DIARY-02 | Diary list in profile HISTORY tab | manual-only | `npm run build` (type check) | N/A |
| DIARY-03 | Diary detail view | manual-only | `npm run build` (type check) | N/A |
| DIARY-04 | Diary edit | manual-only | `npm run build` (type check) | N/A |
| DIARY-05 | Diary delete with confirmation | manual-only | `npm run build` (type check) | N/A |
| DIARY-06 | Following diary feed via story viewer | manual-only | `npm run build` (type check) | N/A |
| DIARY-07 | Story icon row, member-grouped, 24h | manual-only | `npm run build` (type check) | N/A |

**Justification for manual-only:** Frontend has no test runner (CLAUDE.md confirms). Validation is via lint + build (type safety) + browser UAT.

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** Same (no test suite)
- **Phase gate:** Full build green + browser verification

### Wave 0 Gaps
None -- no test infrastructure to set up (frontend validation is lint + build only).

## Sources

### Primary (HIGH confidence)
- `common-docs/openapi/openapi.v1.json` -- WalkDiaryCreateRequest (line 9066), WalkDiaryResponse (line 9152), WalkDiaryPatchRequest (line 11934), StoryDiaryItemResponse (line 12843), StoryGroupResponse (line 12886), all endpoint specs verified
- `src/api/diaries.ts` -- All 6 diary functions verified against OpenAPI schema
- `src/api/community.ts` -- getStories() verified against OpenAPI schema
- `src/api/upload.ts` -- uploadImageFlow() verified

### Secondary (MEDIUM confidence)
- Existing component source code (`StoryArea.tsx`, `ProfileHistory.tsx`, `DiaryBookModal.tsx`, `DiaryPageRenderer.tsx`, `useWalkDiaries.ts`, `useDiaryForm.ts`, `feed/page.tsx`, `MyProfileView.tsx`) -- current data shapes and import patterns analyzed
- CONTEXT.md decisions -- all locked decisions from user discussion

### Tertiary (LOW confidence)
- Thread dropdown for diary create -- exact API for "user's participated threads" needs validation during implementation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries already installed, no new deps
- Architecture: HIGH -- all existing components and API modules examined directly
- Pitfalls: HIGH -- verified by comparing WalkDiaryType vs WalkDiaryResponse field-by-field against OpenAPI schema

**Research date:** 2026-03-07
**Valid until:** 2026-04-07 (stable -- no external dependencies, all project-internal)

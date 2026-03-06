# Phase 7: Walk Diary + Story - Context

**Gathered:** 2026-03-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewire all walk diary and story screens to Phase 2 `api/diaries.ts` and `api/community.ts` infrastructure. Users can create, browse, edit, and delete walk diaries with image upload and public/private visibility; browse a following feed of diaries; and view time-limited stories grouped by member. Frontend-only modifications (aini-inu-frontend/).

**API endpoints in scope (7 endpoints):**
- `POST /api/v1/walk-diaries` (FR-WALK-004, diary create)
- `GET /api/v1/walk-diaries` (FR-WALK-004, diary list -- SliceResponse)
- `GET /api/v1/walk-diaries/{diaryId}` (FR-WALK-004, diary detail)
- `PATCH /api/v1/walk-diaries/{diaryId}` (FR-WALK-004, diary update)
- `DELETE /api/v1/walk-diaries/{diaryId}` (FR-WALK-004, diary delete)
- `GET /api/v1/walk-diaries/following` (FR-WALK-004, following diary feed -- SliceResponse)
- `GET /api/v1/stories` (FR-COMMUNITY-004, story groups -- SliceResponse)

**Applicable DEC policies:**
- DEC-011: Default public visibility, private selection available
- DEC-022: Stories are derived temporary views of walk diaries (no independent CRUD)
- DEC-023: Story audience = author's followers only
- DEC-024: Ordered by diary.createdAt newest first; expires at createdAt + 24h
- DEC-025: One icon per member in story row; group contains multiple diaries shown sequentially

**PRD constraints:**
- Walk diary content max 300 chars (PRD SS8.1)
- 5-state UI coverage: default/loading/empty/error/success (PRD SS8.3)

**Domain terms (PRD SS4.2):**
- Walk Diary (산책일기): Permanent record, full CRUD
- Story (스토리): Diary-derived temporary view, read-only query

**Modification scope:** aini-inu-frontend/ only. Backend and common-docs are read-only.

</domain>

<decisions>
## Implementation Decisions

### Diary CRUD
- Create diary with: title, content (max 300 chars), photoUrls (image upload), walkDate, isPublic toggle, optional threadId link
- Default visibility: public (DEC-011); user can toggle to private
- Edit: same fields as create, content max 300 chars enforced
- Delete: confirmation dialog, then DELETE endpoint, refresh list
- Image upload: use Phase 2 presigned URL flow (`api/upload.ts`)

### Following feed
- `GET /api/v1/walk-diaries/following` with SliceResponse pagination
- Shows diary entries from members the user follows
- Load-more pagination (existing SliceResponse pattern from Phase 6)

### Story display (DEC-022 through DEC-025)
- Stories are read-only derived views -- no create/edit/delete UI for stories
- `GET /api/v1/stories` returns `StoryGroupResponse[]` grouped by member
- One icon per member in horizontal scroll row (StoryArea pattern)
- Tap icon opens sequential diary viewer for that member's stories
- 24h expiry: backend filters by createdAt + 24h; frontend trusts the response (no client-side expiry check needed)
- Private diary or deleted diary = story disappears (backend handles filtering)

### Visibility toggle
- Diary create/edit form includes isPublic toggle (default: true / public)
- When toggled to private: diary hidden from following feed and stories
- UI reflects change immediately after successful API response

### 5-state coverage (PRD SS8.3)
- Default: diary list loaded, stories visible in row
- Loading: skeleton/spinner while fetching diaries and stories
- Empty: friendly message when no diaries exist or no following feed entries
- Error: fetch failed with retry button
- Success: toast on create/edit/delete success

### Claude's Discretion
- Diary create/edit form layout (modal vs page)
- Story viewer UI design (flipbook, carousel, or fullscreen)
- Exact skeleton/loading design
- Diary card design in list view
- Following feed page location (tab within existing page vs standalone)
- Empty state illustration and CTA copy

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/diaries.ts`: All 6 diary endpoint functions fully typed (WalkDiaryCreateRequest, WalkDiaryPatchRequest, WalkDiaryResponse, getDiaries, createDiary, getDiary, updateDiary, deleteDiary, getFollowingDiaries)
- `api/community.ts`: getStories with StoryGroupResponse/StoryDiaryItemResponse types
- `api/upload.ts`: Presigned URL image upload utility
- `components/feed/StoryArea.tsx`: Horizontal story icon row component (needs rewire from old data shape to StoryGroupResponse)
- `components/profile/DiaryBookModal.tsx` + DiaryModal engines (Desktop/Mobile): Flipbook-style diary viewer (needs rewire from old data to WalkDiaryResponse)
- `components/profile/ProfileTabs.tsx`: "산책일기" (HISTORY) tab already in profile
- `hooks/useWalkDiaries.ts`: Diary fetching hook (needs rewire from threadService to api/diaries.ts)
- `hooks/forms/useDiaryForm.ts`: Diary form state hook (needs rewire from threadService to api/diaries.ts)
- `components/common/UserAvatar.tsx`: Avatar with hasRecentDiary ring indicator

### Established Patterns
- `'use client'` on all pages and components
- Toast-only errors via sonner (Korean messages)
- `cn()` for conditional classNames
- Optimistic UI with failure rollback (Phase 4/5 pattern)
- Phase 2 API modules: inline types, `apiClient` envelope unwrap
- SliceResponse load-more pagination (Phase 6 pattern)

### Integration Points
- `src/app/feed/page.tsx`: Feed page already imports StoryArea + DiaryBookModal; currently uses old postService/threadService -- needs rewire
- `src/components/profile/MyProfileView.tsx`: Profile page with HISTORY tab for diary list
- `src/components/profile/ProfileHistory.tsx`: Diary list component in profile
- `services/api/threadService.ts`: OLD module with diary functions -- all calls should migrate to `api/diaries.ts`
- `services/api/postService.ts`: OLD module with story functions -- story calls should migrate to `api/community.ts`
- `types/index.ts` WalkDiaryType: Legacy type -- components should use WalkDiaryResponse from `api/diaries.ts`

</code_context>

<specifics>
## Specific Ideas

- User provided full FR/DEC/DoD specification as acceptance criteria
- DEC-022: Stories have NO independent CRUD -- purely derived from public walk diaries within 24h window
- DEC-025: Member icon grouping -- one avatar per member, tap to cycle through their diaries
- Image upload must use Phase 2 presigned URL flow (not direct upload)
- Private/deleted diary must immediately disappear from stories and following feed

</specifics>

<deferred>
## Deferred Ideas

- Community feed posts (Phase 9) -- feed page also shows posts, but post CRUD is Phase 9 scope
- Story creation UI -- stories are auto-derived from diaries (DEC-022), no separate creation flow needed
- Notification on new story -- deferred to v2 (NOTF-01/02)

</deferred>

---

*Phase: 07-walk-diary-story*
*Context gathered: 2026-03-07*

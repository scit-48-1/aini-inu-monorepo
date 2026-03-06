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

### Diary access & navigation
- **CRUD main access**: Profile HISTORY tab (프로필 산책일기 탭)
  - Diary list, detail view, create/edit/delete all accessible from here
  - '+' button in HISTORY tab opens create modal
- **Following feed**: Accessed via story viewer only (no separate card listing in feed)
- **Story row**: Top of feed page (인스타 패턴) — icon row → click opens viewer
- **Diary detail viewer**: Same DiaryBookModal flipbook used everywhere (profile + feed story)

### Story viewer UX
- **Style**: Existing flipbook DiaryBookModal (Desktop/Mobile engines already built)
- **Member transition**: Auto-advance to next member after finishing one member's diaries (인스타 스토리 패턴)
- **Header**: Top bar with avatar + nickname + created time, updates on member transition
- **Close**: X button + background click to dismiss
- **Navigation**: Page flip within member's diaries, then auto-advance to next member group

### Diary create/edit form
- **UI**: Modal (opened from '+' button in profile HISTORY tab)
- **Fields**: title, content (max 300 chars), photos (max 5장, presigned URL upload), walkDate, isPublic toggle, threadId dropdown
- **isPublic toggle**: Toggle switch + label at form bottom. Default ON (공개). "나만 보기" when toggled off (DEC-011)
- **threadId**: Optional dropdown showing user's participated threads. Not required — diary can exist without thread link
- **Edit**: Same modal pre-filled with existing data. Same field constraints
- **Delete**: Confirmation dialog from diary detail view

### Feed page composition
- **Layout**: Instagram pattern — top story icon row + below post feed (existing structure)
- **Phase 7 scope**: Rewire story area only (StoryArea → `api/community.ts` getStories)
- **Following diaries in feed**: Accessible only through story viewer (click story icon → flipbook). No separate diary card listing in feed
- **Post area (below stories)**: Keep current code as-is (old postService). Phase 9 will rewire post CRUD
- **Story click flow**: StoryArea icon → DiaryBookModal opens with that member's diaries → auto-advance through members

### Visibility toggle (DEC-011)
- Diary create/edit form includes isPublic toggle (default: true / public)
- When toggled to private: diary hidden from following feed and stories
- UI reflects change immediately after successful API response

### 5-state coverage (PRD SS8.3)
- Default: diary list loaded in HISTORY tab, stories visible in feed icon row
- Loading: skeleton/spinner while fetching diaries and stories
- Empty: friendly message when no diaries exist or no stories available
- Error: fetch failed with retry button
- Success: toast on create/edit/delete success

### Claude's Discretion
- Exact skeleton/loading design
- Diary card design in HISTORY tab list view
- Empty state illustration and CTA copy
- Modal sizing and responsive breakpoints
- Thread dropdown label and empty state text

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/diaries.ts`: All 6 diary endpoint functions fully typed (WalkDiaryCreateRequest, WalkDiaryPatchRequest, WalkDiaryResponse, getDiaries, createDiary, getDiary, updateDiary, deleteDiary, getFollowingDiaries)
- `api/community.ts`: getStories with StoryGroupResponse/StoryDiaryItemResponse types
- `api/upload.ts`: Presigned URL image upload utility
- `components/feed/StoryArea.tsx`: Horizontal story icon row component (needs rewire from old data shape to StoryGroupResponse)
- `components/profile/DiaryBookModal.tsx` + DiaryModal engines (Desktop/Mobile): Flipbook-style diary viewer (needs rewire from old data to WalkDiaryResponse + StoryGroupResponse)
- `components/profile/ProfileTabs.tsx`: "산책일기" (HISTORY) tab already in profile
- `components/profile/ProfileHistory.tsx`: Diary list component in profile (needs rewire)
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
- Modal pattern: CreatePostModal, ProfileEditModal for reference

### Integration Points
- `src/app/feed/page.tsx`: Feed page — rewire story area only; keep post area as-is for Phase 9
- `src/components/profile/MyProfileView.tsx`: Profile page with HISTORY tab for diary list + create button
- `src/components/profile/ProfileHistory.tsx`: Diary list component in profile
- `services/api/threadService.ts`: OLD module with diary functions — all diary calls migrate to `api/diaries.ts`
- `services/api/postService.ts`: OLD module with story functions — story calls migrate to `api/community.ts`
- `types/index.ts` WalkDiaryType: Legacy type — components should use WalkDiaryResponse from `api/diaries.ts`

</code_context>

<specifics>
## Specific Ideas

- User provided full FR/DEC/DoD specification as acceptance criteria
- Instagram pattern: story icon row at top of feed, posts below (existing layout preserved)
- Flipbook viewer reused for both profile diary detail and feed story viewing — unified UX
- Auto-advance between members in story viewer (인스타 스토리 패턴)
- Viewer header shows avatar + nickname + time, updates on member transition
- Thread link is optional dropdown in create form — diary can exist standalone
- Photo limit: max 5 images per diary entry
- Private/deleted diary must immediately disappear from stories and following feed

</specifics>

<deferred>
## Deferred Ideas

- Community feed posts rewire (Phase 9) — feed page post area kept as-is, Phase 9 will rewire
- Story creation UI — stories are auto-derived from diaries (DEC-022), no separate creation flow needed
- Notification on new story — deferred to v2 (NOTF-01/02)

</deferred>

---

*Phase: 07-walk-diary-story*
*Context gathered: 2026-03-07*

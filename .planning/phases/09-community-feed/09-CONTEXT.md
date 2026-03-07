# Phase 9: Community Feed - Context

**Gathered:** 2026-03-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewire the community feed frontend screens to use the centralized `api/community.ts` API layer with correct types (`PostResponse`, `PostDetailResponse`, `CommentResponse`), presigned URL image upload via `api/upload.ts`, optimistic like toggle with rollback, and comment delete permission branching. Story section is already wired from Phase 7 — verify integration only.

**Scope:** aini-inu-frontend/ only. No backend or common-docs modifications.

</domain>

<decisions>
## Implementation Decisions

### Feed pagination
- Infinite scroll using `SliceResponse.hasNext` from `getPosts()`
- Auto-load next page on scroll bottom
- Consistent with walk diary following feed pattern

### Image upload
- Multiple images supported per post (backend `imageUrls[]` accepts array)
- Presigned URL upload flow via `uploadImageFlow()` from `api/upload.ts`
- Purpose: `COMMUNITY_POST` for presigned URL request
- Replace current base64 approach in CreatePostModal/usePostForm

### Like behavior (DEC-018)
- Optimistic UI: toggle liked state and likeCount immediately on click
- Call `likePost()` API after UI update
- On failure: rollback to previous state + error toast
- Single endpoint toggles like/unlike (POST `/posts/{id}/like`)

### Post edit (DEC-004)
- Keep PostDetailModal for view + inline edit
- Rewire to `PostDetailResponse` / `PostResponse` types
- Content is required on edit (`PostUpdateRequest.content`)
- `imageUrls` optional on update (keep existing if not changed)

### Comment permissions (DEC-019)
- Comment delete visible to: comment author OR post author
- Compare `comment.author.id` and `post.author.id` against current user ID
- No comment edit — backend has no update endpoint (create + delete only)

### Old service cleanup
- Replace all `postService` (`services/api/postService.ts`) usage with `api/community.ts` functions
- Replace `FeedPostType` / `CommentType` from `@/types` with `PostResponse` / `CommentResponse` from `api/community.ts`
- Delete `services/api/postService.ts` after migration
- Rewire `usePostForm` to use `createPost()` from `api/community.ts` + `uploadImageFlow()`

### 5-state coverage (PRD SS8.3)
- Feed list: loading / empty / error / success states
- Comments: loading / empty / error states
- Like: optimistic success / rollback on error

### Claude's Discretion
- Loading skeleton design for feed items
- Exact scroll threshold for infinite scroll trigger
- Image carousel/gallery component choice for multi-image display
- Comment pagination strategy (load all vs paginated)
- Error state UI details

</decisions>

<specifics>
## Specific Ideas

- Story section already wired in Phase 7 — verify it still works on feed page, no re-implementation needed
- `postService.getPosts()` currently accepts `memberId` and `location` params that don't exist in backend API — drop these
- FeedItem uses `post.likes` / `post.comments` (number fields) but backend returns `likeCount` / `commentCount` — type mapping required
- FeedItem uses `post.images` (string[]) but backend returns `imageUrls` — field name mapping
- Current FeedItem uses string IDs (`currentUserId?: string`) but backend uses number IDs — fix to number
- ProfileFeed component also uses old `FeedPostType` — needs same type rewire

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/community.ts`: All 10 API functions typed and ready (getPosts, createPost, getPost, updatePost, deletePost, getComments, createComment, deleteComment, likePost, getStories)
- `api/upload.ts`: `uploadImageFlow(file, purpose)` handles full presigned URL flow (get URL -> extract token -> PUT upload -> return imageUrl)
- `api/types.ts`: `SliceResponse<T>`, `PaginationParams` already defined
- `components/ui/Card`: Reusable card component used by FeedItem
- `components/ui/Button`, `Typography`: Design system components
- `sonner` toast: Established pattern for success/error notifications

### Established Patterns
- Centralized API layer (`api/*.ts`) with `apiClient` from `api/client.ts` — all new code must use this, not old `services/api/`
- Optimistic update pattern: used in Phase 4 `useFollowToggle` (update UI -> call API -> rollback on failure)
- Image upload: `uploadImageFlow()` used in Phase 5 (pet profile) and Phase 7 (diary) — same pattern for community posts
- SliceResponse pagination: used in Phase 6 (threads), Phase 7 (diaries) — same pattern for post list
- Number IDs everywhere in new API layer (not string)

### Integration Points
- Feed page: `src/app/feed/page.tsx` — main entry, needs full rewire
- FeedItem: `src/components/feed/FeedItem.tsx` — card component, type + API rewire
- CreatePostModal: `src/components/common/CreatePostModal.tsx` — presigned URL upload rewire
- PostDetailModal: `src/components/profile/PostDetailModal.tsx` — type rewire for view/edit/delete
- ProfileFeed: `src/components/profile/ProfileFeed.tsx` — type rewire
- usePostForm: `src/hooks/forms/usePostForm.ts` — rewire to api/community.ts + uploadImageFlow
- MyProfileView: `src/components/profile/MyProfileView.tsx` — posts state uses old types

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 09-community-feed*
*Context gathered: 2026-03-07*

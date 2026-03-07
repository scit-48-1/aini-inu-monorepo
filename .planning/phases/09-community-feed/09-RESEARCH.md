# Phase 9: Community Feed - Research

**Researched:** 2026-03-07
**Domain:** Frontend API rewire — community feed screens to centralized API layer
**Confidence:** HIGH

## Summary

Phase 9 is a frontend-only rewire phase. All backend API functions are already implemented in `api/community.ts` (10 functions) and `api/upload.ts` (presigned URL flow). The work is replacing old `postService` imports and `FeedPostType`/`CommentType` types with the new centralized API types (`PostResponse`, `PostDetailResponse`, `CommentResponse`), implementing infinite scroll pagination, optimistic like toggle with rollback, presigned URL image upload, and comment delete permission branching.

The codebase has well-established patterns from Phases 4-8 that this phase replicates: `useFollowToggle` for optimistic updates, `SliceResponse.hasNext` for infinite scroll, and `uploadImageFlow()` for presigned URL uploads. The primary risk is the field name mismatches between old types and new types (e.g., `post.likes` vs `post.likeCount`, `post.images` vs `post.imageUrls`, string IDs vs number IDs).

**Primary recommendation:** Systematically replace all `postService` and old type imports across 5 consumer files, then layer on infinite scroll pagination and optimistic like toggle using established project patterns.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Infinite scroll using `SliceResponse.hasNext` from `getPosts()`, auto-load next page on scroll bottom
- Multiple images per post via `uploadImageFlow()` with purpose `COMMUNITY_POST`
- Optimistic like: toggle liked state + likeCount immediately, call `likePost()`, rollback on failure
- Post edit keeps PostDetailModal; rewire to `PostDetailResponse`/`PostResponse` types; content required; imageUrls optional on update
- Comment delete visible to comment author OR post author; no comment edit (backend has no update endpoint)
- Replace all `postService` usage with `api/community.ts`; replace `FeedPostType`/`CommentType` with `PostResponse`/`CommentResponse`; delete `postService.ts` after migration
- 5-state coverage: feed list (loading/empty/error/success), comments (loading/empty/error), like (optimistic success/rollback)
- Story section already wired from Phase 7 -- verify only, no re-implementation

### Claude's Discretion
- Loading skeleton design for feed items
- Exact scroll threshold for infinite scroll trigger
- Image carousel/gallery component choice for multi-image display
- Comment pagination strategy (load all vs paginated)
- Error state UI details

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| FEED-01 | Post creation with image + body text required | `createPost()` from `api/community.ts` + `uploadImageFlow()` from `api/upload.ts`; replace base64 approach in CreatePostModal/usePostForm |
| FEED-02 | Post list with pagination | `getPosts()` returns `SliceResponse<PostResponse>`; infinite scroll pattern from Phase 6/7 |
| FEED-03 | Post detail view | `getPost()` returns `PostDetailResponse` with embedded comments |
| FEED-04 | Post edit (content required) | `updatePost()` with `PostUpdateRequest`; content required, imageUrls optional |
| FEED-05 | Post delete | `deletePost()` already typed; rewire from `postService.deletePost` |
| FEED-06 | Comment CRUD with permission branching | `createComment()`, `deleteComment()` from `api/community.ts`; delete visible to comment.author.id or post.author.id matching current user |
| FEED-07 | Like with optimistic UI + rollback | `likePost()` returns `PostLikeResponse`; follow `useFollowToggle` optimistic pattern |
| FEED-08 | Presigned URL image upload | `uploadImageFlow(file, 'COMMUNITY_POST')` from `api/upload.ts`; replace FileReader base64 approach |
</phase_requirements>

## Standard Stack

### Core (Already in Project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19 | UI framework | Project standard |
| Next.js | 16 | App Router, `'use client'` pages | Project standard |
| TypeScript | - | Type safety | Project standard |
| Tailwind CSS | 4 | Styling | Project standard |
| Zustand | - | State management (user store) | Project standard |
| sonner | - | Toast notifications | Project standard |
| lucide-react | - | Icons | Project standard |

### Supporting (Already Available)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `api/community.ts` | - | All 10 community API functions | All post/comment/like/story calls |
| `api/upload.ts` | - | Presigned URL upload flow | Image upload in post creation |
| `api/types.ts` | - | `SliceResponse`, `PaginationParams` | Pagination for post list |

### No New Dependencies
This phase requires zero new npm packages. All needed infrastructure exists.

## Architecture Patterns

### Files to Modify (Complete List)

```
src/
├── app/feed/page.tsx                        # Full rewire: pagination, types, API
├── components/feed/FeedItem.tsx             # Type rewire + optimistic like + comment delete permission
├── components/common/CreatePostModal.tsx    # Presigned URL upload + type rewire
├── components/profile/PostDetailModal.tsx   # Type rewire to PostDetailResponse
├── components/profile/ProfileFeed.tsx       # Type rewire PostResponse
├── components/profile/ProfileView.tsx       # Remove postService import, drop posts fetch
├── components/profile/MyProfileView.tsx     # Remove FeedPostType import, fix posts state
├── components/shared/forms/PostFormFields.tsx # Remove PostFormData type, accept new form shape
├── hooks/forms/usePostForm.ts              # Rewire to api/community.ts + uploadImageFlow
├── hooks/useMemberProfile.ts              # Remove postService import
├── services/api/postService.ts            # DELETE after migration
└── types/index.ts                          # Remove FeedPostType, CommentType, PostFormData (if no other consumers)
```

### Pattern 1: Field Name Mapping (Old -> New)

**What:** Systematic type migration from legacy types to API response types
**When to use:** Every file that imports `FeedPostType` or `CommentType`

| Old Field (FeedPostType) | New Field (PostResponse) | Notes |
|--------------------------|--------------------------|-------|
| `id: string` | `id: number` | Type change |
| `author: UserType` | `author: Author` | Different shape (id/nickname/profileImageUrl only) |
| `author.avatar` | `author.profileImageUrl` | Field rename |
| `images: string[]` | `imageUrls: string[]` | Field rename |
| `caption: string` | `content: string` | Field rename |
| `content?: string` | `content: string` | Was alias, now primary |
| `likes: number` | `likeCount: number` | Field rename |
| `comments: number` | `commentCount: number` | Field rename |
| `isLiked: boolean` | `liked: boolean` | Field rename |
| `location: string` | (not in response) | Drop -- backend has no location field |
| `commentsList?: CommentType[]` | (not in list response) | Use `getPost()` for detail with comments |
| `tags?: string[]` | (not in response) | Drop -- backend has no tags |
| `time?: string` | `createdAt: string` | Compute relative time from ISO string |

| Old Field (CommentType) | New Field (CommentResponse) | Notes |
|--------------------------|--------------------------|-------|
| `id: string` | `id: number` | Type change |
| `author.nickname` | `author.nickname` | Same |
| `author.avatar` | `author.profileImageUrl` | Field rename |

### Pattern 2: Infinite Scroll Pagination

**What:** Load more posts when user scrolls to bottom
**When to use:** Feed page post list
**Example:**
```typescript
// Established pattern from Phase 6/7
const [posts, setPosts] = useState<PostResponse[]>([]);
const [page, setPage] = useState(0);
const [hasNext, setHasNext] = useState(false);
const [isLoading, setIsLoading] = useState(true);

const fetchPosts = async (pageNum: number) => {
  const res = await getPosts({ page: pageNum, size: 10 });
  if (pageNum === 0) {
    setPosts(res.content);
  } else {
    setPosts(prev => [...prev, ...res.content]);
  }
  setHasNext(res.hasNext);
  setPage(pageNum);
};

const loadMore = () => {
  if (hasNext) fetchPosts(page + 1);
};

// Scroll detection via IntersectionObserver or scroll event
```

### Pattern 3: Optimistic Like Toggle

**What:** Toggle like state immediately, call API, rollback on failure
**When to use:** Like button in FeedItem
**Example:**
```typescript
// Following useFollowToggle pattern from Phase 4
const handleLike = async (e: React.MouseEvent) => {
  e.stopPropagation();

  // Save previous state for rollback
  const prevLiked = post.liked;
  const prevCount = post.likeCount;

  // Optimistic update
  setPost(prev => ({
    ...prev,
    liked: !prev.liked,
    likeCount: prev.liked ? prev.likeCount - 1 : prev.likeCount + 1,
  }));

  try {
    await likePost(post.id);
  } catch {
    // Rollback
    setPost(prev => ({
      ...prev,
      liked: prevLiked,
      likeCount: prevCount,
    }));
    toast.error('좋아요 처리에 실패했습니다.');
  }
};
```

### Pattern 4: Presigned URL Upload (Replace Base64)

**What:** Use `uploadImageFlow()` instead of FileReader base64
**When to use:** CreatePostModal / usePostForm
**Example:**
```typescript
// Established pattern from Phase 5/7
import { uploadImageFlow } from '@/api/upload';
import { createPost } from '@/api/community';

const handleImageUpload = async (file: File) => {
  const imageUrl = await uploadImageFlow(file, 'COMMUNITY_POST');
  setImageUrls(prev => [...prev, imageUrl]);
};

const handleSubmit = async () => {
  await createPost({ content, imageUrls });
};
```

### Pattern 5: Comment Delete Permission

**What:** Show delete button for comment author OR post author
**When to use:** Comment rendering in FeedItem
**Example:**
```typescript
const currentUserId = Number(useUserStore.getState().profile?.id);
const canDeleteComment = (comment: CommentResponse, postAuthorId: number) => {
  return comment.author.id === currentUserId || postAuthorId === currentUserId;
};
```

### Anti-Patterns to Avoid
- **Keeping postService alongside api/community.ts:** All consumers must be migrated in one sweep, then postService deleted
- **Using string IDs with new API types:** All IDs are `number` in the new API layer
- **Passing `memberId` or `location` to getPosts():** Backend API doesn't accept these params
- **Using `put` for post update:** Backend uses PATCH, not PUT (already correct in `api/community.ts`)
- **Base64 image upload:** Must use presigned URL flow via `uploadImageFlow()`

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Image upload | FileReader + base64 encoding | `uploadImageFlow(file, 'COMMUNITY_POST')` | Presigned URL is the project standard; base64 fails for large files |
| Pagination | Custom offset tracking | `SliceResponse.hasNext` + page counter | Established pattern, handles edge cases |
| Optimistic updates | Ad-hoc state management | Follow `useFollowToggle` pattern (save prev -> update -> try/catch rollback) | Proven rollback logic |
| Toast notifications | Custom error UI | `sonner` toast (success/error) | Project standard |
| Relative time display | Manual date math | Simple helper from `createdAt` ISO string | Keep consistent across app |

## Common Pitfalls

### Pitfall 1: ID Type Mismatch (string vs number)
**What goes wrong:** Old code uses `currentUserId?: string` and `post.id: string`; new API uses `number` for all IDs
**Why it happens:** Legacy `FeedPostType` and `CommentType` used string IDs; new `PostResponse` uses number
**How to avoid:** Use `Number()` conversion at the boundary (e.g., `Number(profile.id)` from Zustand store which stores string)
**Warning signs:** `===` comparisons that always return false between string and number

### Pitfall 2: Field Name Confusion (likes vs likeCount)
**What goes wrong:** UI displays 0 or undefined for like/comment counts
**Why it happens:** Old type uses `post.likes` / `post.comments`; new type uses `post.likeCount` / `post.commentCount`
**How to avoid:** Search-and-replace all field references when changing types; check every property access
**Warning signs:** `undefined` values in rendered counts, NaN in arithmetic

### Pitfall 3: PostDetailResponse Embedded Comments
**What goes wrong:** Fetching comments separately when `getPost()` already includes them
**Why it happens:** `PostDetailResponse` has `comments: CommentResponse[]` embedded
**How to avoid:** Use embedded comments from `getPost()` response; only use `getComments()` for pagination if needed
**Warning signs:** Double API calls for same data

### Pitfall 4: CreatePostModal UserType Dependency
**What goes wrong:** CreatePostModal expects `UserType` for `userProfile` prop, but Phase 9 should use `MemberResponse`
**Why it happens:** PostFormFields imports `UserType` from `@/types` and accesses `userProfile.avatar`
**How to avoid:** Either pass adapted data or update the component to accept `MemberResponse` shape
**Warning signs:** Type errors when replacing userProfile source

### Pitfall 5: PostDetailModal Uses FeedPostType for Edit
**What goes wrong:** PostDetailModal's `onUpdate`/`onDelete` don't call the API -- they're empty callbacks delegating to parent
**Why it happens:** Current PostDetailModal has no API integration; parent (MyProfileView/ProfileView) passes stub callbacks
**How to avoid:** Wire `updatePost()` and `deletePost()` from `api/community.ts` either in PostDetailModal or in the parent callback
**Warning signs:** Edit/delete actions that appear to work but don't persist

### Pitfall 6: ProfileView.tsx Still Uses Old memberService + postService
**What goes wrong:** ProfileView fetches posts via `postService.getPosts(targetId)` which sends invalid `memberId` param
**Why it happens:** Backend `GET /posts` has no `memberId` query parameter
**How to avoid:** Remove posts fetch from ProfileView entirely (posts on other user's profile may not be a supported feature), or use `getPosts()` without memberId
**Warning signs:** 400 errors from backend on profile page load

## Code Examples

### Feed Page Rewire (Core Structure)
```typescript
// Source: Established patterns from Phase 6/7
import { useState, useEffect, useCallback, useRef } from 'react';
import { getPosts } from '@/api/community';
import type { PostResponse } from '@/api/community';

export default function FeedPage() {
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  const fetchPosts = useCallback(async (pageNum: number) => {
    if (pageNum === 0) setIsLoading(true);
    setHasError(false);
    try {
      const res = await getPosts({ page: pageNum, size: 10 });
      setPosts(prev => pageNum === 0 ? res.content : [...prev, ...res.content]);
      setHasNext(res.hasNext);
      setPage(pageNum);
    } catch {
      setHasError(true);
      toast.error('피드를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { fetchPosts(0); }, [fetchPosts]);
  // ...
}
```

### FeedItem Type Rewire
```typescript
// Old:
import { FeedPostType, CommentType } from '@/types';
interface FeedItemProps {
  post: FeedPostType;
  currentUserId?: string;
  onDelete?: (postId: string) => void;
}

// New:
import type { PostResponse, CommentResponse } from '@/api/community';
interface FeedItemProps {
  post: PostResponse;
  currentUserId?: number;
  onDelete?: (postId: number) => void;
  postAuthorId?: number; // for comment delete permission
}
```

### usePostForm Rewire
```typescript
// Old: uses postService.createPost with base64 images
// New:
import { createPost } from '@/api/community';
import { uploadImageFlow } from '@/api/upload';

export function usePostForm(onSuccess?: () => void) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [content, setContent] = useState('');
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);

  const handleSubmit = useCallback(async () => {
    if (imageFiles.length === 0) return toast.warning('사진을 최소 1장 이상 업로드해주세요.');
    if (!content.trim()) return toast.warning('내용을 입력해주세요.');

    setIsSubmitting(true);
    try {
      // Upload all images via presigned URL
      const imageUrls = await Promise.all(
        imageFiles.map(f => uploadImageFlow(f, 'COMMUNITY_POST'))
      );
      await createPost({ content, imageUrls });
      toast.success('게시글이 등록되었습니다!');
      // Reset form
      setContent('');
      setImageFiles([]);
      setPreviewUrls([]);
      onSuccess?.();
    } catch {
      toast.error('등록 중 오류가 발생했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  }, [content, imageFiles, onSuccess]);

  return { content, setContent, imageFiles, setImageFiles, previewUrls, setPreviewUrls, isSubmitting, handleSubmit };
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `postService` (services/api/) | `api/community.ts` (centralized) | Phase 2 | All post calls must use new layer |
| base64 image upload | Presigned URL via `uploadImageFlow()` | Phase 2 (INFRA-05) | Large file support, server-side storage |
| `FeedPostType` (types/index.ts) | `PostResponse` (api/community.ts) | Phase 2 | Field names match backend exactly |
| String IDs throughout | Number IDs | Phase 2 | Type-safe comparisons |

**Deprecated/outdated:**
- `services/api/postService.ts`: Must be deleted after this phase
- `FeedPostType`, `CommentType`, `PostFormData` in `types/index.ts`: Replace with API types
- base64 FileReader approach in PostFormFields: Replace with File object + presigned URL

## Open Questions

1. **Post list on profile pages (ProfileView/MyProfileView)**
   - What we know: Backend `GET /posts` has no `memberId` filter param. MyProfileView keeps `posts` state but never fetches (removed in Phase 4). ProfileView still calls `postService.getPosts(targetId)` which would fail.
   - What's unclear: Should profile pages show user's posts at all? Backend may not support filtered post listing.
   - Recommendation: Remove posts fetch from ProfileView (it was already removed from MyProfileView). Keep ProfileFeed component but with empty state until backend adds member-filtered endpoint. Or leave as-is with empty array.

2. **PostFormFields Component Coupling**
   - What we know: PostFormFields imports `UserType` and `PostFormData` from `@/types`, uses `postForm.caption` and `postForm.location`
   - What's unclear: Whether to fully rewrite PostFormFields or adapt it minimally
   - Recommendation: Rewrite PostFormFields to accept File-based image upload callback instead of base64, rename `caption` to `content`, drop `location` field (not in API)

3. **Comment Pagination Strategy (Claude's Discretion)**
   - What we know: `PostDetailResponse` embeds all comments; separate `getComments()` supports pagination
   - Recommendation: Use embedded comments from `getPost()` on initial load. If comment count is large, add "load more" via `getComments()` with page param. For MVP, embedded comments are sufficient.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None (frontend has no test runner) |
| Config file | None |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FEED-01 | Post creation with image + body | manual-only | `npm run build` (type check) | N/A |
| FEED-02 | Post list pagination | manual-only | `npm run build` (type check) | N/A |
| FEED-03 | Post detail view | manual-only | `npm run build` (type check) | N/A |
| FEED-04 | Post edit | manual-only | `npm run build` (type check) | N/A |
| FEED-05 | Post delete | manual-only | `npm run build` (type check) | N/A |
| FEED-06 | Comment CRUD + permissions | manual-only | `npm run build` (type check) | N/A |
| FEED-07 | Like optimistic toggle | manual-only | `npm run build` (type check) | N/A |
| FEED-08 | Presigned URL upload | manual-only | `npm run build` (type check) | N/A |

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** `cd aini-inu-frontend && npm run build`
- **Phase gate:** Build green + lint clean before `/gsd:verify-work`

### Wave 0 Gaps
None -- frontend has no test runner. Validation is via `npm run lint` + `npm run build` (TypeScript type checking).

## Sources

### Primary (HIGH confidence)
- Direct code inspection of `api/community.ts` -- all 10 API functions with types verified
- Direct code inspection of `api/upload.ts` -- `uploadImageFlow()` pattern verified
- Direct code inspection of existing consumers (FeedItem, CreatePostModal, PostDetailModal, ProfileFeed, usePostForm, postService)
- Direct code inspection of `useFollowToggle.ts` -- optimistic update pattern verified
- Direct code inspection of `api/types.ts` -- `SliceResponse`, `PaginationParams` verified

### Secondary (MEDIUM confidence)
- CONTEXT.md decisions on behavior (locked by user)
- Prior phase decisions from STATE.md for pattern consistency

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all libraries already in project, no new dependencies
- Architecture: HIGH - all patterns established in prior phases (4, 5, 6, 7)
- Pitfalls: HIGH - derived from direct code inspection of type mismatches
- Field mapping: HIGH - compared `FeedPostType` fields against `PostResponse` fields line by line

**Research date:** 2026-03-07
**Valid until:** 2026-04-07 (stable -- no external dependencies changing)

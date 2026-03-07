---
status: resolved
trigger: "Post edit functionality shows '수정 기능 준비 중' toast instead of working in both FeedItem and PostDetailModal"
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: FeedItem shows placeholder toast because parent (feed/page.tsx) never passes onEdit prop; PostDetailModal already has working edit implementation
test: Read both components and their parent usage
expecting: FeedItem fallback branch fires toast; PostDetailModal has full edit flow
next_action: Report diagnosis (research only)

## Symptoms

expected: Clicking edit in the three-dot dropdown should open an edit form for the post
actual: FeedItem shows "수정 기능 준비 중" toast; PostDetailModal edit status needs verification at parent level
errors: No runtime errors - placeholder toast is intentional stub code
reproduction: Open any own post in feed, click three-dot menu, click 수정
started: Stub was present since dropdown was implemented

## Eliminated

- hypothesis: updatePost API function does not exist
  evidence: updatePost exists at api/community.ts:106-111, accepts (postId, PostUpdateRequest), returns PostResponse via PATCH /posts/{postId}
  timestamp: 2026-03-07

- hypothesis: PostDetailModal has the same toast stub
  evidence: PostDetailModal (lines 39-57) has full working edit flow - handleEdit() sets editing state, handleSave() calls updatePost API, inline textarea renders when isEditing=true
  timestamp: 2026-03-07

## Evidence

- timestamp: 2026-03-07
  checked: FeedItem.tsx lines 304-317 (dropdown edit button)
  found: Conditional rendering - if onEdit prop is provided, calls onEdit(post); if NOT provided, shows toast('수정 기능 준비 중'). The fallback branch at line 313 is the stub.
  implication: FeedItem delegates edit to parent via callback. The component itself has no inline edit UI.

- timestamp: 2026-03-07
  checked: feed/page.tsx lines 147-153 (FeedItem usage)
  found: FeedItem is rendered with props key, post, currentUserId, onDelete, onLikeUpdate. NO onEdit prop is passed.
  implication: Since onEdit is undefined, the conditional at line 304 always falls to the else branch, triggering the toast stub.

- timestamp: 2026-03-07
  checked: FeedItem props interface (line 38)
  found: onEdit is typed as optional: `onEdit?: (post: PostResponse) => void`
  implication: FeedItem expects the parent to provide an edit handler. It's designed as a callback pattern, not inline edit.

- timestamp: 2026-03-07
  checked: PostDetailModal.tsx lines 10, 30-57, 86-113, 120-133
  found: PostDetailModal imports updatePost, has isEditing/editContent state, handleEdit() populates edit state, handleSave() calls updatePost API, renders textarea when editing. Full working edit flow exists.
  implication: PostDetailModal's edit works correctly when accessed from profile pages. The bug description about PostDetailModal may be inaccurate, or the issue is that PostDetailModal is not used from the feed page.

- timestamp: 2026-03-07
  checked: api/community.ts lines 18-22, 106-111
  found: PostUpdateRequest type exists (content, caption?, imageUrls?). updatePost function calls apiClient.patch on /posts/{postId}.
  implication: API layer is complete and ready for use.

## Resolution

root_cause: |
  TWO distinct issues:

  1. **FeedItem (feed page):** The feed page (src/app/feed/page.tsx line 147) does NOT pass an `onEdit` prop to `<FeedItem>`. FeedItem's dropdown has a conditional at line 304: if `onEdit` exists, it calls the callback; otherwise (line 313) it fires `toast('수정 기능 준비 중')`. Since feed/page.tsx never provides onEdit, the toast stub always fires.

  2. **FeedItem design gap:** FeedItem has no inline editing capability. It uses a callback pattern (`onEdit?: (post: PostResponse) => void`), meaning the parent page must handle the edit UI (e.g., opening a modal or navigating to an edit form). No such handler exists in feed/page.tsx.

  PostDetailModal is NOT affected - it already has a complete working edit flow (inline textarea + updatePost API call).

fix: (not applied - research only)
verification: (not applicable)
files_changed: []

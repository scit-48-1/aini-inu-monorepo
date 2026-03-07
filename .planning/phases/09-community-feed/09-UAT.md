---
status: resolved
phase: 09-community-feed
source: [09-01-SUMMARY.md, 09-02-SUMMARY.md, 09-03-SUMMARY.md, 09-04-SUMMARY.md]
started: 2026-03-07T06:50:00Z
updated: 2026-03-07T07:30:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Create a Post with Images
expected: Open the create post modal. Select images — thumbnails appear as previews. Write content and submit. Post is created successfully (no upload purpose error) and appears in the feed with uploaded images displayed.
result: pass

### 2. Like a Post
expected: Click the like button on a post. The like count increments immediately (optimistic update). No transaction error from the server. If the API call fails, the count rolls back to the previous value.
result: pass

### 3. Add a Comment
expected: Expand a post's comment section. Type a comment and submit. The comment appears in the list with your author info (no NPE from server). The comment count on the post increments.
result: pass

### 4. Delete a Comment (Permission-based)
expected: On a comment you authored, a delete option is visible. On comments by others, delete is only visible if you are the post author. Clicking delete removes the comment from the list.
result: pass

### 5. Edit Own Post from Detail Modal
expected: Navigate to your profile. Click on a post you created. In PostDetailModal, edit and delete buttons are visible. Click edit, modify content, save. The post updates with new content. On someone else's post, edit/delete buttons are not shown.
result: issue
reported: "삭제하는 버튼은 점점점 모양의 이미지가 있는데 수정 버튼은 안보여. 점점점을 누르면 삭제 또는 수정 2가지의 기능을 선택해서 할 수 있도록 뜨게 할 수 있어?"
severity: major

### 6. Delete Own Post from Detail Modal
expected: In PostDetailModal for your own post, click delete. The post is removed. The profile feed grid updates to reflect the deletion.
result: issue
reported: "삭제가 안되고 있어. 외래키 제약조건 위반 - post 테이블 삭제 시 comment 테이블에서 참조하고 있어서 FK constraint violation. 외래키 제거하고 인덱스와 참조값만 유지하라"
severity: blocker

## Summary

total: 6
passed: 4
issues: 2
pending: 0
skipped: 0

## Gaps

- truth: "Edit and delete buttons visible in PostDetailModal for own post, accessible via three-dot menu"
  status: resolved
  reason: "User reported: 삭제하는 버튼은 점점점 모양의 이미지가 있는데 수정 버튼은 안보여. 점점점을 누르면 삭제 또는 수정 2가지의 기능을 선택해서 할 수 있도록 뜨게 할 수 있어?"
  severity: major
  test: 5
  root_cause: "Neither PostDetailModal nor FeedItem has a dropdown menu. PostDetailModal has edit/delete as bare footer buttons (edit works but not discoverable). FeedItem three-dot button hardcodes onClick to setShowDeleteConfirm(true) — no dropdown, no edit option."
  artifacts:
    - path: "aini-inu-frontend/src/components/profile/PostDetailModal.tsx"
      issue: "Edit/delete are bare buttons in footer, no three-dot dropdown menu"
    - path: "aini-inu-frontend/src/components/feed/FeedItem.tsx"
      issue: "Three-dot button onClick hardcoded to delete confirmation, no dropdown, no edit"
  missing:
    - "Add dropdown/popover menu on three-dot click with edit and delete options in both components"
  debug_session: ".planning/debug/post-detail-modal-missing-edit.md"
- truth: "Post is deleted successfully and removed from profile feed grid"
  status: resolved
  reason: "User reported: 삭제가 안됨. FK constraint violation - comment 테이블이 post를 참조하여 삭제 불가. 외래키 제거하고 인덱스+참조값만 유지하라"
  severity: blocker
  test: 6
  root_cause: "Comment.java and PostLike.java have @ManyToOne @JoinColumn to Post, causing Hibernate to auto-generate FK constraints. PostService.deletePost() calls postRepository.delete(post) without deleting child rows first."
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/community/entity/Comment.java"
      issue: "@ManyToOne @JoinColumn(name='post_id') generates FK constraint blocking post deletion"
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/community/entity/PostLike.java"
      issue: "@ManyToOne @JoinColumn(name='post_id') generates second FK constraint"
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/community/service/PostService.java"
      issue: "deletePost() does not delete comments/likes before deleting post"
  missing:
    - "Replace @ManyToOne Post with @Column Long postId in Comment.java and PostLike.java"
    - "Add deleteAllByPostId() to CommentRepository and PostLikeRepository"
    - "Update PostService.deletePost() to bulk-delete children before parent"
    - "Add DDL migration to drop existing FK constraints"
  debug_session: ".planning/debug/post-delete-fk-violation.md"

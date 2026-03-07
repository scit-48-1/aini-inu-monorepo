---
status: diagnosed
phase: 09-community-feed
source: [09-01-SUMMARY.md, 09-02-SUMMARY.md, 09-03-SUMMARY.md]
started: 2026-03-07T06:20:00Z
updated: 2026-03-07T06:30:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Create a Post with Images
expected: Open the create post modal. Select multiple images — thumbnails appear as previews. Write content and submit. Post is created successfully and appears in the feed with uploaded images displayed.
result: issue
reported: "등록 실패해 '업로드 목적 또는 파일 정보가 올바르지 않습니다' 이렇게 나타나"
severity: major

### 2. Feed Infinite Scroll
expected: Open the feed page. Initial posts load with a loading spinner. Scroll to the bottom — more posts load automatically without a button click. A small loading spinner appears while fetching the next page. If no more posts exist, scrolling stops triggering loads.
result: pass

### 3. Like a Post
expected: Click the like button on a post. The like count increments immediately (optimistic). If the API call fails, the count rolls back to the previous value.
result: issue
reported: "해당 기능이 서버에서 실패하는 것 같아. 'org.springframework.transaction.TransactionSystemException: Could not commit JPA transaction' 이런 에러가 서버에서 나타나"
severity: major

### 4. Add a Comment
expected: Expand a post's comment section. Type a comment and submit. The comment appears in the list with your author info. The comment count on the post increments.
result: issue
reported: "이것도 실패하는데 서버에서는 'java.lang.NullPointerException: Cannot invoke \"java.lang.Long.longValue()\" because \"current\" is null' 이런 에러가 나타나"
severity: major

### 5. Delete a Comment (Permission-based)
expected: On a comment you authored, a delete option is visible. On comments by others, delete is only visible if you are the post author. Clicking delete removes the comment from the list.
result: skipped
reason: 댓글 작성이 안되므로 테스트 불가

### 6. Feed Empty and Error States
expected: With no posts in the system, the feed shows an empty state message. If the API call fails, an error message with a retry button is displayed. Clicking retry re-fetches.
result: pass

### 7. View Post Detail from Profile
expected: Navigate to your profile. Click on a post in the profile feed grid. PostDetailModal opens showing the post content, images, and metadata.
result: pass

### 8. Edit Own Post from Detail Modal
expected: In PostDetailModal for your own post, edit and delete buttons are visible. Click edit, modify content, save. The post updates with new content. On someone else's post, edit/delete buttons are not shown.
result: skipped
reason: 글 작성이 안되므로 테스트 불가

### 9. Delete Own Post from Detail Modal
expected: In PostDetailModal for your own post, click delete. The post is removed. The profile feed grid updates to reflect the deletion.
result: skipped
reason: 글 작성이 안되므로 테스트 불가

## Summary

total: 9
passed: 3
issues: 3
pending: 0
skipped: 3

## Gaps

- truth: "Post is created successfully with uploaded images"
  status: failed
  reason: "User reported: 등록 실패해 '업로드 목적 또는 파일 정보가 올바르지 않습니다' 이렇게 나타나"
  severity: major
  test: 1
  root_cause: "Frontend sends 'COMMUNITY_POST' as upload purpose but backend UploadPurpose enum only defines 'POST'"
  artifacts:
    - path: "aini-inu-frontend/src/hooks/forms/usePostForm.ts"
      issue: "Line 42: uploadImageFlow(f, 'COMMUNITY_POST') uses wrong purpose string"
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/community/dto/UploadPurpose.java"
      issue: "Enum defines POST, not COMMUNITY_POST"
  missing:
    - "Change purpose argument from 'COMMUNITY_POST' to 'POST' in usePostForm.ts"
  debug_session: ".planning/debug/post-creation-upload-purpose.md"
- truth: "Like count increments immediately and persists via API"
  status: failed
  reason: "User reported: 서버에서 실패 - org.springframework.transaction.TransactionSystemException: Could not commit JPA transaction"
  severity: major
  test: 3
  root_cause: "Post entity @Version Long version field is nullable and uninitialized. Seed SQL omits version column, leaving NULL. Optimistic lock UPDATE fails with NULL version."
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/community/entity/Post.java"
      issue: "Line 44: @Version private Long version — nullable, no default"
    - path: "aini-inu-backend/src/main/resources/db/seed/10_core_sample_seed.sql"
      issue: "INSERT omits version column for post table"
    - path: "aini-inu-backend/src/main/resources/db/seed/20_status_edge_seed.sql"
      issue: "INSERT omits version column for post table"
  missing:
    - "Initialize version field: private Long version = 0L"
    - "Add version column with value 0 to all seed SQL INSERTs"
  debug_session: ".planning/debug/like-toggle-transaction-error.md"
- truth: "Comment appears in list with author info after submission"
  status: failed
  reason: "User reported: 서버에서 java.lang.NullPointerException: Cannot invoke \"java.lang.Long.longValue()\" because \"current\" is null"
  severity: major
  test: 4
  root_cause: "Same as Issue 2: Post entity @Version Long version is NULL on seeded posts. Comment creation dirties entity via increaseComment(), Hibernate version check NPEs on null Long unboxing."
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/community/entity/Post.java"
      issue: "Line 44: @Version private Long version — nullable, no default"
  missing:
    - "Fix shared with Issue 2 — initializing version field fixes both"
  debug_session: ".planning/debug/comment-create-npe.md"

---
phase: 09-community-feed
plan: 05
subsystem: database
tags: [jpa, spring-data, fk-constraint, cascade-delete, postgresql]

requires:
  - phase: 09-community-feed
    provides: "Comment and PostLike entities with @ManyToOne FK to Post"
provides:
  - "FK-free Comment/PostLike entities using @Column Long postId"
  - "Manual cascade delete in PostService.deletePost()"
  - "DDL migration for FK constraint removal"
affects: [09-community-feed]

tech-stack:
  added: []
  patterns: ["Manual cascade delete pattern: bulk-delete children before parent when FK constraints removed"]

key-files:
  created:
    - "aini-inu-backend/src/main/resources/db/ddl/07_community_fk_removal.sql"
  modified:
    - "aini-inu-backend/src/main/java/scit/ainiinu/community/service/PostService.java"
    - "aini-inu-backend/src/test/java/scit/ainiinu/community/service/PostServiceTest.java"

key-decisions:
  - "Manual cascade delete (deleteAllByPostId) instead of JPA CascadeType to avoid FK constraint issues"

patterns-established:
  - "FK removal pattern: replace @ManyToOne with @Column Long id + manual cascade delete in service layer"

requirements-completed: [FEED-02]

duration: 5min
completed: 2026-03-07
---

# Phase 09 Plan 05: FK Violation Fix Summary

**Manual cascade delete for post deletion -- bulk-deletes comments and likes before post, with FK constraints removed from entities and DDL**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-07T07:13:26Z
- **Completed:** 2026-03-07T07:18:31Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- PostService.deletePost() now cascade-deletes all comments and likes before deleting the post
- All repository methods updated from Post-object-based to postId-based queries
- New InOrder test verifies children are deleted before parent
- DDL migration script ready for FK constraint removal in production

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace @ManyToOne FK with @Column in Comment and PostLike entities** - `1b27b7d` (refactor) -- pre-existing from 09-06 plan
2. **Task 2: Update PostService to cascade-delete children and use postId-based queries** - `6d4fd29` (fix)

**Plan metadata:** pending (docs: complete plan)

## Files Created/Modified
- `aini-inu-backend/src/main/java/scit/ainiinu/community/service/PostService.java` - Cascade delete in deletePost(), all methods updated to postId-based queries
- `aini-inu-backend/src/main/resources/db/ddl/07_community_fk_removal.sql` - DDL migration to drop FK constraints
- `aini-inu-backend/src/test/java/scit/ainiinu/community/service/PostServiceTest.java` - All tests updated for postId-based API, new cascade-delete order test

## Decisions Made
- Manual cascade delete (deleteAllByPostId) instead of JPA CascadeType to avoid FK constraint issues while keeping entities decoupled

## Deviations from Plan

None - plan executed exactly as written. Task 1 entity/repository changes were already committed as part of 09-06 plan (commit 1b27b7d), so only Task 2 (PostService + tests + DDL) required new work.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Post deletion FK violation is resolved
- All PostServiceTest tests pass including new cascade-delete verification
- Full test suite passes with no regressions

---
*Phase: 09-community-feed*
*Completed: 2026-03-07*

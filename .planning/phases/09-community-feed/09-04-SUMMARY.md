---
phase: 09-community-feed
plan: 04
subsystem: community
tags: [jpa, optimistic-locking, upload, seed-data, bug-fix]

requires:
  - phase: 09-community-feed
    provides: Community feed CRUD, like toggle, comment creation
provides:
  - Working post creation with correct upload purpose
  - Reliable like toggle and comment creation via @Version initialization
  - Seed data with version column for community posts
affects: []

tech-stack:
  added: []
  patterns:
    - "@Version field always initialized to 0L to prevent null unboxing"

key-files:
  created: []
  modified:
    - aini-inu-frontend/src/hooks/forms/usePostForm.ts
    - aini-inu-backend/src/main/java/scit/ainiinu/community/entity/Post.java
    - aini-inu-backend/src/main/resources/db/seed/10_core_sample_seed.sql
    - aini-inu-backend/src/main/resources/db/seed/20_status_edge_seed.sql

key-decisions:
  - "Post @Version initialized to 0L in entity field declaration to prevent NPE on optimistic lock operations"

patterns-established:
  - "@Version Long fields must be initialized to 0L, not left as null, to prevent unboxing NPE during Hibernate optimistic locking"

requirements-completed: [FEED-01, FEED-05, FEED-06]

duration: 2min
completed: 2026-03-07
---

# Phase 09 Plan 04: UAT Gap Closure Summary

**Fixed three UAT failures: upload purpose mismatch (COMMUNITY_POST -> POST), and @Version null causing like toggle transaction error and comment creation NPE**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-07T06:42:04Z
- **Completed:** 2026-03-07T06:43:56Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Post creation now uses correct 'POST' upload purpose matching backend UploadPurpose enum
- Post.version initialized to 0L preventing null Long unboxing NPE on optimistic lock operations
- Both seed SQL files updated with version=0 for all post rows, fixing seeded data consistency

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix upload purpose string in usePostForm** - `04c52e4` (fix)
2. **Task 2: Initialize Post @Version field and update seed SQL** - `dda4a38` (fix)

## Files Created/Modified
- `aini-inu-frontend/src/hooks/forms/usePostForm.ts` - Changed upload purpose from 'COMMUNITY_POST' to 'POST'
- `aini-inu-backend/src/main/java/scit/ainiinu/community/entity/Post.java` - Initialized @Version field to 0L
- `aini-inu-backend/src/main/resources/db/seed/10_core_sample_seed.sql` - Added version=0 column to post INSERTs
- `aini-inu-backend/src/main/resources/db/seed/20_status_edge_seed.sql` - Added version=0 column to post INSERTs

## Decisions Made
- Post @Version initialized to 0L in entity field declaration to prevent NPE on optimistic lock operations

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required. For existing databases with NULL version on post rows, run: `UPDATE post SET version = 0 WHERE version IS NULL;`

## Next Phase Readiness
- All three UAT failures (post creation, like toggle, comment creation) are fixed
- Community feed core functionality is fully operational
- Ready for next phase

---
*Phase: 09-community-feed*
*Completed: 2026-03-07*

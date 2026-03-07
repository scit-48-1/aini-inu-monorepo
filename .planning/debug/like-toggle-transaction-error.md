---
status: diagnosed
trigger: "Like toggle fails with TransactionSystemException: Could not commit JPA transaction"
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: Post entity @Version field is null for seeded rows, causing optimistic lock failure on UPDATE
test: Checked seed SQL and entity definition
expecting: Seed SQL omits version column, entity initializes version as null
next_action: Report root cause

## Symptoms

expected: Like toggle API completes successfully, toggling like state
actual: TransactionSystemException on JPA commit
errors: org.springframework.transaction.TransactionSystemException: Could not commit JPA transaction
reproduction: Call POST /api/v1/posts/{postId}/like on any post
started: Likely since seed data was loaded or since @Version was added

## Eliminated

(none)

## Evidence

- timestamp: 2026-03-07
  checked: Post entity (Post.java)
  found: "@Version private Long version;" declared on line 44 -- nullable Long type, never initialized in Post.create()
  implication: New posts get version=null; JPA uses "WHERE version IS NULL" for first update which may work, but seed-loaded rows have version=NULL in DB and JPA's optimistic lock handling with null versions is problematic

- timestamp: 2026-03-07
  checked: Seed SQL (10_core_sample_seed.sql, 20_status_edge_seed.sql)
  found: INSERT INTO post columns are (id, author_id, content, like_count, comment_count, created_at, updated_at) -- NO version column
  implication: All seeded posts have version=NULL in database. When toggleLike modifies likeCount, JPA flushes UPDATE with WHERE version IS NULL, which fails or produces unexpected behavior depending on DB dialect

- timestamp: 2026-03-07
  checked: toggleLike method in PostService.java (line 189-211)
  found: Method loads Post, modifies likeCount (dirty entity), JPA auto-flushes on transaction commit with optimistic lock version check
  implication: The version mismatch / null version causes the TransactionSystemException on commit

- timestamp: 2026-03-07
  checked: BaseTimeEntity
  found: createdAt has @Column(nullable = false, updatable = false), updatedAt has @Column(nullable = false) -- both use JPA auditing
  implication: Auditing fields are fine; the issue is specifically the @Version field

## Resolution

root_cause: The Post entity uses @Version with a nullable Long type (line 44), and the version column is not populated in seed SQL. When toggleLike() modifies the Post's likeCount, JPA tries to flush an UPDATE with an optimistic lock check on a NULL version value. This causes a constraint violation or lock failure at transaction commit time. The root issue is two-fold: (1) seed data omits the version column, leaving it NULL; (2) the @Version field type is Long (nullable object) rather than long (primitive, defaults to 0).
fix: (not applied -- diagnosis only)
verification: (not applied -- diagnosis only)
files_changed: []

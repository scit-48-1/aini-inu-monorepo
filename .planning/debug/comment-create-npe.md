---
status: diagnosed
trigger: "Comment creation fails with NullPointerException: Cannot invoke java.lang.Long.longValue() because 'current' is null"
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: Post.version (Long) is NULL in DB for seed-data rows; Hibernate unboxes it during optimistic lock check causing NPE
test: confirmed by reading seed SQL and entity definition
expecting: n/a - root cause confirmed
next_action: report diagnosis

## Symptoms

expected: Comment creation succeeds and increments post comment count
actual: NullPointerException thrown during comment creation
errors: "java.lang.NullPointerException: Cannot invoke java.lang.Long.longValue() because 'current' is null"
reproduction: Create a comment on any seed-data post
started: Since seed data was added without version column

## Eliminated

(none - first hypothesis confirmed)

## Evidence

- timestamp: 2026-03-07
  checked: Post entity definition (Post.java line 43-44)
  found: "@Version private Long version;" - boxed Long, no default initializer
  implication: If DB column is NULL, field will be null in Java

- timestamp: 2026-03-07
  checked: Seed SQL (10_core_sample_seed.sql lines 400-410, 20_status_edge_seed.sql lines 247-251)
  found: INSERT INTO post columns are (id, author_id, content, like_count, comment_count, created_at, updated_at) - version column is OMITTED
  implication: All seed-data posts have version=NULL in the database

- timestamp: 2026-03-07
  checked: PostService.createComment() (PostService.java lines 220-234)
  found: Method calls post.increaseComment() which dirties the entity, triggering Hibernate flush with optimistic lock version check
  implication: Hibernate reads version field (null), tries to call longValue() on it, throws NPE

- timestamp: 2026-03-07
  checked: Post.create() factory method (Post.java line 46-51)
  found: Factory method does not initialize version field; for NEW entities JPA handles this, but for seed-loaded rows with NULL version column, it stays null
  implication: Only seed-data posts are affected; posts created through the application would get version=0 from JPA

## Resolution

root_cause: The Post entity's @Version field is declared as `Long version` (boxed, nullable) with no default value. Seed SQL scripts omit the version column, leaving it NULL in the database. When Hibernate performs an optimistic locking version check during flush (triggered by post.increaseComment()), it attempts to unbox the null Long via longValue(), causing the NullPointerException. The internal Hibernate variable "current" in the error message refers to the current version value read from the entity.
fix: (not applied - diagnosis only)
verification: (not applied - diagnosis only)
files_changed: []

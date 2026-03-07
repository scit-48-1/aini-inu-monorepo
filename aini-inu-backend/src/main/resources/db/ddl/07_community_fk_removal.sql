-- Drop FK constraints from comment and post_like tables
-- Hibernate auto-generated these from @ManyToOne; now using @Column with index only
-- Using DROP IF EXISTS for idempotent execution

-- Comment table: drop both named and Hibernate-auto-generated FK constraints
ALTER TABLE comment DROP CONSTRAINT IF EXISTS fk_comment_post_id;
ALTER TABLE comment DROP CONSTRAINT IF EXISTS fkbxj0iufqno03hcdpnx2p4bhnj;

-- PostLike table: drop both named and Hibernate-auto-generated FK constraints
ALTER TABLE post_like DROP CONSTRAINT IF EXISTS fk_post_like_post_id;
ALTER TABLE post_like DROP CONSTRAINT IF EXISTS fkj7iy0k7n3d0vkh8o7ibjna884;

-- Keep index for query performance (already exists from 04_community_indexes_constraints.sql)

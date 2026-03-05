-- Community domain index/constraint alignment
CREATE INDEX IF NOT EXISTS idx_post_created_at_id
    ON post (created_at, id);

CREATE INDEX IF NOT EXISTS idx_post_author_created_at
    ON post (author_id, created_at);

CREATE INDEX IF NOT EXISTS idx_comment_post_created_at
    ON comment (post_id, created_at);

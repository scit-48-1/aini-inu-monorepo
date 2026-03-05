-- Walk domain index/constraint alignment
CREATE INDEX IF NOT EXISTS idx_thread_status_start_time
    ON thread (status, start_time);

CREATE INDEX IF NOT EXISTS idx_walk_diary_member_created_id
    ON walk_diary (member_id, created_at, id);

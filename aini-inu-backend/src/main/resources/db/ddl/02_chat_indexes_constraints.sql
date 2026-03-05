-- Chat domain index/constraint alignment
CREATE INDEX IF NOT EXISTS idx_chat_room_thread_id
    ON chat_room (thread_id);

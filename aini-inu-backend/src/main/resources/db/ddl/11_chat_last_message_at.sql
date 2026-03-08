-- Add last_message_at column to chat_room for proper message-based sorting
ALTER TABLE chat_room ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMPTZ;

-- Backfill from existing messages
UPDATE chat_room cr SET last_message_at = (
    SELECT MAX(m.sent_at) FROM message m WHERE m.chat_room_id = cr.id
) WHERE cr.last_message_at IS NULL;

-- Index for sorting chat rooms by latest message
CREATE INDEX IF NOT EXISTS idx_chat_room_last_message_at ON chat_room (last_message_at DESC NULLS LAST, id DESC);

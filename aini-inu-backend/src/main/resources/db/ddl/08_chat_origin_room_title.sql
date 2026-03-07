ALTER TABLE chat_room ADD COLUMN IF NOT EXISTS origin VARCHAR(20);
ALTER TABLE chat_room ADD COLUMN IF NOT EXISTS room_title VARCHAR(200);

-- Backfill: walk rooms (thread_id present)
UPDATE chat_room SET origin = 'WALK' WHERE thread_id IS NOT NULL AND origin IS NULL;

-- Backfill: lost pet rooms (linked via lost_pet_match)
UPDATE chat_room SET origin = 'LOST_PET'
WHERE id IN (SELECT DISTINCT chat_room_id FROM lost_pet_match WHERE chat_room_id IS NOT NULL)
  AND origin IS NULL;

-- Backfill: remaining are DM
UPDATE chat_room SET origin = 'DM' WHERE origin IS NULL;

ALTER TABLE chat_room ALTER COLUMN origin SET NOT NULL;

-- Walk room titles
UPDATE chat_room cr SET room_title = t.title
FROM thread t WHERE cr.thread_id = t.id AND cr.origin = 'WALK' AND cr.room_title IS NULL;

-- Lost pet room titles
UPDATE chat_room cr
SET room_title = COALESCE(lpr.breed || ' ', '') || lpr.pet_name || '를 찾습니다'
FROM lost_pet_match lpm
JOIN lost_pet_report lpr ON lpm.lost_pet_id = lpr.id
WHERE lpm.chat_room_id = cr.id AND cr.origin = 'LOST_PET' AND cr.room_title IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_room_origin ON chat_room (origin);

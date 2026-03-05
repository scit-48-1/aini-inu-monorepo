-- Legacy Story table cleanup
DROP INDEX IF EXISTS idx_story_expires_at;
DROP TABLE IF EXISTS story;

-- Timeline feature: timeline_event table + member timeline visibility

ALTER TABLE member ADD COLUMN IF NOT EXISTS is_timeline_public BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS timeline_event (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    reference_id BIGINT NOT NULL,
    title VARCHAR(100),
    summary VARCHAR(500),
    thumbnail_url VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (event_type, reference_id)
);

CREATE INDEX IF NOT EXISTS idx_timeline_member_deleted_occurred
    ON timeline_event (member_id, deleted, occurred_at DESC);

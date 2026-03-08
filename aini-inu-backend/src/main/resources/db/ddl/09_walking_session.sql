CREATE TABLE IF NOT EXISTS walking_session (
    id                BIGSERIAL PRIMARY KEY,
    member_id         BIGINT NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at        TIMESTAMP NOT NULL,
    ended_at          TIMESTAMP,
    last_heartbeat_at TIMESTAMP NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_walking_session_member_active
    ON walking_session (member_id) WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_walking_session_status_heartbeat
    ON walking_session (status, last_heartbeat_at);

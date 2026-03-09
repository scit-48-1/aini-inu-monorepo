-- 알림 테이블
CREATE TABLE IF NOT EXISTS notification (
    id              BIGSERIAL PRIMARY KEY,
    recipient_member_id BIGINT       NOT NULL,
    type            VARCHAR(30)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    reference_id    BIGINT,
    reference_type  VARCHAR(30),
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 수신자별 최신 알림 조회용
CREATE INDEX IF NOT EXISTS idx_notification_recipient_created
    ON notification (recipient_member_id, created_at DESC);

-- 안읽은 알림 카운트용 partial index
CREATE INDEX IF NOT EXISTS idx_notification_recipient_unread
    ON notification (recipient_member_id)
    WHERE is_read = FALSE;

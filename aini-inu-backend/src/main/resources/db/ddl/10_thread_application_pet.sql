CREATE TABLE IF NOT EXISTS thread_application_pet (
    id              BIGSERIAL PRIMARY KEY,
    application_id  BIGINT NOT NULL,
    pet_id          BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_application_pet UNIQUE (application_id, pet_id)
);

CREATE INDEX IF NOT EXISTS idx_thread_application_pet_application_id
    ON thread_application_pet (application_id);

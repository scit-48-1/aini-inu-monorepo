-- LostPet domain schema/index alignment (PostgreSQL + pgvector)

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS lost_pet_report (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    pet_name VARCHAR(100) NOT NULL,
    breed VARCHAR(100),
    photo_url VARCHAR(500) NOT NULL,
    description VARCHAR(2000),
    last_seen_at TIMESTAMP NOT NULL,
    last_seen_location VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS sighting (
    id BIGSERIAL PRIMARY KEY,
    finder_id BIGINT NOT NULL,
    photo_url VARCHAR(500) NOT NULL,
    found_at TIMESTAMP NOT NULL,
    found_location VARCHAR(255) NOT NULL,
    memo VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Spring AI pgvector store table for sighting retrieval
CREATE TABLE IF NOT EXISTS lostpet_vector_store (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding VECTOR(768)
);

CREATE TABLE IF NOT EXISTS lost_pet_search_session (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    lost_pet_id BIGINT NOT NULL,
    query_mode VARCHAR(20) NOT NULL,
    query_image_url VARCHAR(1000),
    query_text VARCHAR(2000),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lost_pet_search_session_lost_pet
        FOREIGN KEY (lost_pet_id) REFERENCES lost_pet_report(id)
);

CREATE TABLE IF NOT EXISTS lost_pet_search_candidate (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    sighting_id BIGINT NOT NULL,
    score_similarity DECIMAL(6, 5) NOT NULL,
    score_distance DECIMAL(6, 5) NOT NULL,
    score_recency DECIMAL(6, 5) NOT NULL,
    score_total DECIMAL(6, 5) NOT NULL,
    rank_order INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lost_pet_search_candidate_session
        FOREIGN KEY (session_id) REFERENCES lost_pet_search_session(id),
    CONSTRAINT fk_lost_pet_search_candidate_sighting
        FOREIGN KEY (sighting_id) REFERENCES sighting(id)
);

CREATE TABLE IF NOT EXISTS lost_pet_match (
    id BIGSERIAL PRIMARY KEY,
    lost_pet_id BIGINT NOT NULL,
    sighting_id BIGINT NOT NULL,
    similarity_total DECIMAL(5, 4) NOT NULL,
    status VARCHAR(40) NOT NULL,
    approved_by_member_id BIGINT,
    approved_at TIMESTAMP,
    chat_room_id BIGINT,
    invalidated_reason VARCHAR(40),
    invalidated_at TIMESTAMP,
    matched_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lost_pet_match_lost_pet
        FOREIGN KEY (lost_pet_id) REFERENCES lost_pet_report(id),
    CONSTRAINT fk_lost_pet_match_sighting
        FOREIGN KEY (sighting_id) REFERENCES sighting(id)
);

CREATE INDEX IF NOT EXISTS idx_lost_pet_report_owner_status
    ON lost_pet_report (owner_id, status);

CREATE INDEX IF NOT EXISTS idx_lost_pet_report_status_last_seen_at
    ON lost_pet_report (status, last_seen_at);

CREATE INDEX IF NOT EXISTS idx_sighting_finder_found_at
    ON sighting (finder_id, found_at);

CREATE INDEX IF NOT EXISTS idx_sighting_status_found_at
    ON sighting (status, found_at);

CREATE INDEX IF NOT EXISTS idx_lost_pet_match_lost_pet_status
    ON lost_pet_match (lost_pet_id, status);

CREATE INDEX IF NOT EXISTS idx_lost_pet_match_sighting_status
    ON lost_pet_match (sighting_id, status);

CREATE INDEX IF NOT EXISTS idx_lost_pet_search_session_owner_lost_pet_created
    ON lost_pet_search_session (owner_id, lost_pet_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_lost_pet_search_session_expires_at
    ON lost_pet_search_session (expires_at);

CREATE INDEX IF NOT EXISTS idx_lost_pet_search_candidate_session_score
    ON lost_pet_search_candidate (session_id, score_total DESC, rank_order ASC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lost_pet_search_candidate_session_sighting
    ON lost_pet_search_candidate (session_id, sighting_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lost_pet_search_candidate_session_rank
    ON lost_pet_search_candidate (session_id, rank_order);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lost_pet_match_pair
    ON lost_pet_match (lost_pet_id, sighting_id);

CREATE INDEX IF NOT EXISTS idx_lostpet_vector_store_embedding_hnsw
    ON lostpet_vector_store
    USING HNSW (embedding vector_cosine_ops);

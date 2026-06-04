CREATE TABLE knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL DEFAULT 1,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    document_count INTEGER NOT NULL DEFAULT 0,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_base_owner_id ON knowledge_base (owner_id);

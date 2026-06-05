CREATE TABLE document (
    id BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    processing_version INTEGER NOT NULL DEFAULT 1,
    failed_stage VARCHAR(30),
    error_message TEXT,
    created_by BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_knowledge_base
        FOREIGN KEY (knowledge_base_id)
        REFERENCES knowledge_base (id)
);

CREATE INDEX idx_document_knowledge_base_id
    ON document (knowledge_base_id);

CREATE TABLE document_chunk (
    id BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    processing_version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    token_count INTEGER,
    vector_id VARCHAR(100),
    page_number INTEGER,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_chunk_knowledge_base
        FOREIGN KEY (knowledge_base_id)
        REFERENCES knowledge_base (id),
    CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_id)
        REFERENCES document (id)
);

CREATE INDEX idx_document_chunk_document_id
    ON document_chunk (document_id);

CREATE INDEX idx_document_chunk_knowledge_base_id
    ON document_chunk (knowledge_base_id);

CREATE INDEX idx_document_chunk_document_version_active
    ON document_chunk (document_id, processing_version, is_active);

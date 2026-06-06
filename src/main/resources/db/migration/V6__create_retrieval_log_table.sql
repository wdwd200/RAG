CREATE TABLE retrieval_log (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    session_id BIGINT,
    message_id BIGINT,
    knowledge_base_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    retriever_type VARCHAR(30) NOT NULL DEFAULT 'VECTOR',
    top_k INTEGER NOT NULL,
    chunk_id BIGINT,
    document_id BIGINT,
    rank_position INTEGER NOT NULL,
    score DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_retrieval_log_request_id
    ON retrieval_log (request_id);

CREATE INDEX idx_retrieval_log_session_id
    ON retrieval_log (session_id);

CREATE INDEX idx_retrieval_log_knowledge_base_id
    ON retrieval_log (knowledge_base_id);

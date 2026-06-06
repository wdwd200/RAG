CREATE TABLE llm_call_log (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    session_id BIGINT,
    message_id BIGINT,
    knowledge_base_id BIGINT,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    latency_ms BIGINT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_call_log_request_id
    ON llm_call_log (request_id);

CREATE INDEX idx_llm_call_log_session_id
    ON llm_call_log (session_id);

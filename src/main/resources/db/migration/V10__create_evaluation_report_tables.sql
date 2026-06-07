CREATE TABLE evaluation_report (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    top_k INTEGER NOT NULL,
    question_count INTEGER NOT NULL,
    recall_at_k DOUBLE PRECISION NOT NULL,
    hit_rate_at_k DOUBLE PRECISION NOT NULL,
    mrr DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE INDEX idx_evaluation_report_dataset_id
    ON evaluation_report (dataset_id);

CREATE INDEX idx_evaluation_report_knowledge_base_id
    ON evaluation_report (knowledge_base_id);

CREATE TABLE evaluation_question_result (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    expected_chunk_ids_json TEXT NOT NULL,
    retrieved_chunk_ids_json TEXT NOT NULL,
    hit BOOLEAN NOT NULL,
    reciprocal_rank DOUBLE PRECISION NOT NULL,
    recall_at_k DOUBLE PRECISION NOT NULL,
    ranked_hit_position INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evaluation_question_result_report_id
    ON evaluation_question_result (report_id);

CREATE INDEX idx_evaluation_question_result_question_id
    ON evaluation_question_result (question_id);

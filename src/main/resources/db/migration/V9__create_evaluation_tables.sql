CREATE TABLE evaluation_dataset (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    description TEXT,
    question_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evaluation_dataset_knowledge_base_id
    ON evaluation_dataset (knowledge_base_id);

CREATE TABLE evaluation_question (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    ground_truth_answer TEXT,
    relevant_chunk_ids_json TEXT NOT NULL,
    relevant_content_hashes_json TEXT,
    document_processing_version INTEGER,
    question_type VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evaluation_question_dataset_id
    ON evaluation_question (dataset_id);

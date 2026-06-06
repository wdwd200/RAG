ALTER TABLE chat_message
    ADD COLUMN request_id VARCHAR(64);

CREATE INDEX idx_chat_message_request_id
    ON chat_message (request_id);

CREATE TABLE IF NOT EXISTS runtime_errors (
    id BIGSERIAL PRIMARY KEY,
    error_code VARCHAR(50),
    message TEXT,
    stack_trace TEXT,
    user_id BIGINT REFERENCES platform_users(id),
    psp_id VARCHAR(50) REFERENCES psps(psp_id),
    occurred_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

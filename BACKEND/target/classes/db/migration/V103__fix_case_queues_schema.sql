CREATE TABLE IF NOT EXISTS case_queues (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    psp_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

ALTER TABLE compliance_cases ADD COLUMN IF NOT EXISTS queue_id BIGINT;
-- Check if constraint exists before adding? Flyway doesn't support IF NOT EXISTS for constraints easily in standard SQL without blocks.
-- Postgres supports it in DO blocks.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_cases_queue') THEN
        ALTER TABLE compliance_cases ADD CONSTRAINT fk_compliance_cases_queue FOREIGN KEY (queue_id) REFERENCES case_queues(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS case_activities (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_case_activities_case FOREIGN KEY (case_id) REFERENCES compliance_cases(id),
    CONSTRAINT fk_case_activities_user FOREIGN KEY (user_id) REFERENCES platform_users(id)
);

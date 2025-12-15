-- Add merchant_id to compliance_cases for merchant-level filtering
ALTER TABLE compliance_cases
ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_compliance_cases_merchant ON compliance_cases(merchant_id);


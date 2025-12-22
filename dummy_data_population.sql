-- dummy_data_population.sql
-- Populates the database with Merchants and Compliance Cases

-- 1. Ensure TechFlow PSP exists (from V99)
-- (We assume V99 ran or we handle it here just in case)

-- 2. Insert Dummy Merchants
INSERT INTO merchants (legal_name, trading_name, country, registration_number, mcc, business_type, expected_monthly_volume, transaction_channel, website, contact_email, status, psp_id)
SELECT 'TechFlow Retail Store', 'TechFlow Store', 'USA', 'REG-12345', '5411', 'CORPORATION', 5000000, 'IN_STORE', 'https://store.techflow.com', 'store@techflow.com', 'ACTIVE', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE registration_number = 'REG-12345');

INSERT INTO merchants (legal_name, trading_name, country, registration_number, mcc, business_type, expected_monthly_volume, transaction_channel, website, contact_email, status, psp_id)
SELECT 'Global Logistics Solutions', 'Global Log', 'GBR', 'REG-67890', '4214', 'LLC', 15000000, 'ONLINE', 'https://logistics.global.com', 'ops@globallog.com', 'ACTIVE', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE registration_number = 'REG-67890');

INSERT INTO merchants (legal_name, trading_name, country, registration_number, mcc, business_type, expected_monthly_volume, transaction_channel, website, contact_email, status, psp_id)
SELECT 'Phoenix Digital Services', 'Phoenix Dig', 'ZAF', 'REG-11223', '7372', 'PRIVATE_LIMITED', 2500000, 'ONLINE', 'https://phoenix.digital', 'contact@phoenix.digital', 'UNDER_REVIEW', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE registration_number = 'REG-11223');

-- 3. Insert Compliance Cases
INSERT INTO compliance_cases (case_reference, description, status, priority, sla_deadline, days_open, assigned_to_user_id, merchant_id, psp_id, created_at)
SELECT 'CASE-2024-001', 'High value transaction sequence from new merchant', 'IN_PROGRESS', 'HIGH', NOW() + INTERVAL '48 hours', 2, (SELECT id FROM platform_users WHERE username = 'investigator'), 'REG-12345', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'), NOW() - INTERVAL '2 days'
WHERE NOT EXISTS (SELECT 1 FROM compliance_cases WHERE case_reference = 'CASE-2024-001');

INSERT INTO compliance_cases (case_reference, description, status, priority, sla_deadline, days_open, assigned_to_user_id, merchant_id, psp_id, created_at)
SELECT 'CASE-2024-002', 'Sudden spike in cross-border volume', 'NEW', 'MEDIUM', NOW() + INTERVAL '72 hours', 0, NULL, 'REG-67890', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'), NOW()
WHERE NOT EXISTS (SELECT 1 FROM compliance_cases WHERE case_reference = 'CASE-2024-002');

INSERT INTO compliance_cases (case_reference, description, status, priority, sla_deadline, days_open, assigned_to_user_id, merchant_id, psp_id, created_at)
SELECT 'CASE-2024-003', 'Periodic review for high-risk MCC', 'NEW', 'LOW', NOW() + INTERVAL '120 hours', 1, (SELECT id FROM platform_users WHERE username = 'compliance'), 'REG-11223', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'), NOW() - INTERVAL '1 day'
WHERE NOT EXISTS (SELECT 1 FROM compliance_cases WHERE case_reference = 'CASE-2024-003');

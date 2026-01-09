-- Seed data for integration tests
-- Insert a test PSP (Payment Service Provider) required for merchant creation

INSERT INTO psps (psp_code, legal_name, trading_name, country, registration_number, tax_id, 
                  contact_email, contact_phone, billing_plan, billing_cycle, payment_terms, 
                  currency, status, is_test_mode, created_at, updated_at)
SELECT 'TEST_PSP', 'Test Payment Provider', 'Test PSP', 'KEN', 'REG-TEST-001', 'TAX-TEST-001',
       'test@testpsp.com', '+254700000000', 'STANDARD', 'MONTHLY', 30, 'KES', 'ACTIVE', true,
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM psps WHERE psp_code = 'TEST_PSP');

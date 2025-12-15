-- V99__Dummy_Credentials.sql
-- Creates dummy PSP, Roles, and Users for testing/demo purposes

-- 1. Create Dummy PSP 'TechFlow'
INSERT INTO psps (psp_code, legal_name, trading_name, country, contact_email, status, billing_plan)
VALUES ('TECHFLOW_PSP', 'TechFlow Inc.', 'TechFlow', 'USA', 'admin@techflow.com', 'ACTIVE', 'PAY_AS_YOU_GO')
ON CONFLICT (psp_code) DO NOTHING;

-- 2. Create Roles

-- a) System Admin (Global)
INSERT INTO roles (name, description, psp_id)
SELECT 'ADMIN', 'Global System Administrator', NULL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN' AND psp_id IS NULL);

-- b) TechFlow Admin (PSP Admin)
INSERT INTO roles (name, description, psp_id)
SELECT 'ADMIN', 'TechFlow Administrator', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN' AND psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'));

-- c) TechFlow Compliance Officer
INSERT INTO roles (name, description, psp_id)
SELECT 'COMPLIANCE_OFFICER', 'Compliance Officer for TechFlow', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'COMPLIANCE_OFFICER' AND psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'));

-- d) TechFlow Investigator
INSERT INTO roles (name, description, psp_id)
SELECT 'INVESTIGATOR', 'Investigator for TechFlow', (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'INVESTIGATOR' AND psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'));

-- 3. Assign Permissions to Roles (Basic Set)

-- Compliance Officer Permissions
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p 
FROM roles r, 
LATERAL (VALUES 
    ('CREATE_CASES'), ('ASSIGN_CASES'), ('CLOSE_CASES'), 
    ('VIEW_CASES'), ('VIEW_SAR'), ('APPROVE_SAR'), ('VIEW_PII')
) AS t(p)
WHERE r.name = 'COMPLIANCE_OFFICER' AND r.psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission = t.p);

-- Investigator Permissions
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p 
FROM roles r, 
LATERAL (VALUES 
    ('VIEW_CASES'), ('ADD_CASE_NOTES'), ('ADD_CASE_EVIDENCE'), ('CREATE_SAR')
) AS t(p)
WHERE r.name = 'INVESTIGATOR' AND r.psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission = t.p);

-- 4. Create Users (Password is 'password' for all: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG)

-- a) System Admin: admin / password
INSERT INTO platform_users (username, password_hash, email, first_name, last_name, role_id, psp_id, enabled, created_at)
SELECT 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'admin@sys.com', 'System', 'Admin', 
       (SELECT id FROM roles WHERE name = 'ADMIN' AND psp_id IS NULL), 
       NULL, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM platform_users WHERE username = 'admin');

-- b) TechFlow Admin: techflow_admin / password
INSERT INTO platform_users (username, password_hash, email, first_name, last_name, role_id, psp_id, enabled, created_at)
SELECT 'techflow_admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'admin@techflow.com', 'TechFlow', 'Admin',
       (SELECT id FROM roles WHERE name = 'ADMIN' AND psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')),
       (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'), true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM platform_users WHERE username = 'techflow_admin');

-- c) TechFlow Compliance: compliance_officer / password
INSERT INTO platform_users (username, password_hash, email, first_name, last_name, role_id, psp_id, enabled, created_at)
SELECT 'compliance', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'compliance@techflow.com', 'Jane', 'Compliance',
       (SELECT id FROM roles WHERE name = 'COMPLIANCE_OFFICER' AND psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')),
       (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'), true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM platform_users WHERE username = 'compliance');

-- d) TechFlow Investigator: investigator / password
INSERT INTO platform_users (username, password_hash, email, first_name, last_name, role_id, psp_id, enabled, created_at)
SELECT 'investigator', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'investigator@techflow.com', 'Bob', 'Investigator',
       (SELECT id FROM roles WHERE name = 'INVESTIGATOR' AND psp_id = (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP')),
       (SELECT psp_id FROM psps WHERE psp_code = 'TECHFLOW_PSP'), true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM platform_users WHERE username = 'investigator');

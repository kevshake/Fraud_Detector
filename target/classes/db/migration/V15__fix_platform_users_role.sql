-- Fix for platform_users table violating not-null constraint on 'role' column
-- This column is a vestige of previous schema (string role) and conflicts with the new 'role_id' foreign key.

ALTER TABLE platform_users DROP COLUMN IF EXISTS role;

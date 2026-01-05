package com.posgateway.aml.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Running SchemaFixer: Attempting to fix schema...");

            // Fix 1: Drop legacy 'role' column from platform_users
            try {
                // First ensure it's nullable so inserts don't fail if drop fails
                jdbcTemplate.execute("ALTER TABLE platform_users ALTER COLUMN role DROP NOT NULL");
                System.out.println("SchemaFixer: Successfully dropped NOT NULL from 'role' column.");

                jdbcTemplate.execute("ALTER TABLE platform_users DROP COLUMN IF EXISTS role");
                System.out.println("SchemaFixer: Successfully dropped 'role' column.");
            } catch (Exception e) {
                System.out.println("SchemaFixer: 'role' column modification skipped/failed: " + e.getMessage());
            }

            // Fix 2: Cast resolved_by to bigint safely
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE compliance_cases ALTER COLUMN resolved_by TYPE bigint USING (CASE WHEN resolved_by~E'^\\\\d+$' THEN resolved_by::bigint ELSE NULL END)");
                System.out.println("SchemaFixer: Successfully altered 'resolved_by' to bigint.");
            } catch (Exception e) {
                System.out.println("SchemaFixer: 'resolved_by' alter skipped/failed: " + e.getMessage());
            }

            // Fix 3: Ensure merchant_id is bigint
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE compliance_cases ALTER COLUMN merchant_id TYPE bigint USING (CASE WHEN merchant_id~E'^\\\\d+$' THEN merchant_id::bigint ELSE NULL END)");
                System.out.println("SchemaFixer: Successfully altered 'merchant_id' to bigint.");
            } catch (Exception e) {
                System.out.println("SchemaFixer: 'merchant_id' alter skipped/failed: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("SchemaFixer: Warning - Failed to drop column. " + e.getMessage());
            // Don't throw, let app try to proceed
        }
    }
}

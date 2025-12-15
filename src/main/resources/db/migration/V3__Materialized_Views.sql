-- Materialized View for Merchant Risk Summary
CREATE MATERIALIZED VIEW merchant_risk_summary_mv AS
SELECT
    m.country,
    m.business_type,
    m.status,
    COUNT(*) as merchant_count,
    AVG(CASE WHEN mrs.overall_risk_score > 0 THEN mrs.overall_risk_score ELSE NULL END) as avg_risk_score
FROM merchants m
LEFT JOIN merchant_risk_scores mrs ON m.merchant_id = mrs.merchant_id
GROUP BY m.country, m.business_type, m.status;

CREATE INDEX idx_risk_summary_country ON merchant_risk_summary_mv(country);

-- Function to refresh the view
CREATE OR REPLACE FUNCTION refresh_merchant_risk_summary()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY merchant_risk_summary_mv;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger to refresh on risk score changes (optional, maybe run periodically instead)
-- CREATE TRIGGER refresh_risk_summary_trigger
-- AFTER INSERT OR UPDATE ON merchant_risk_scores
-- FOR EACH STATEMENT
-- EXECUTE FUNCTION refresh_merchant_risk_summary();

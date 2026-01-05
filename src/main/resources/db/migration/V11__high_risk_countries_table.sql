CREATE TABLE high_risk_countries (
    id SERIAL PRIMARY KEY,
    country_code VARCHAR(3) NOT NULL UNIQUE,
    country_name VARCHAR(100),
    risk_level VARCHAR(20) DEFAULT 'HIGH',
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100)
);

INSERT INTO high_risk_countries (country_code, country_name, risk_level, added_by) VALUES
('AF', 'Afghanistan', 'HIGH', 'SYSTEM'),
('KP', 'North Korea', 'CRITICAL', 'SYSTEM'),
('IR', 'Iran', 'CRITICAL', 'SYSTEM'),
('SY', 'Syria', 'HIGH', 'SYSTEM'),
('YE', 'Yemen', 'HIGH', 'SYSTEM'),
('MM', 'Myanmar', 'HIGH', 'SYSTEM'),
('VE', 'Venezuela', 'HIGH', 'SYSTEM'),
('ZW', 'Zimbabwe', 'HIGH', 'SYSTEM');

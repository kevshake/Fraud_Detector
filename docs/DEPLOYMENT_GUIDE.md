# AML Fraud Detector - Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Database Configuration](#database-configuration)
4. [Application Configuration](#application-configuration)
5. [Build and Deployment](#build-and-deployment)
6. [Post-Deployment Verification](#post-deployment-verification)
7. [Monitoring and Maintenance](#monitoring-and-maintenance)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- **Java 21** or higher
- **Maven 3.6+**
- **PostgreSQL 13+** (production database)
- **Aerospike 6.0+** (for sanctions screening and caching)
- **Nginx** or similar reverse proxy (recommended for production)

### System Requirements
- **CPU:** Minimum 4 cores, recommended 8+ cores
- **RAM:** Minimum 8GB, recommended 16GB+
- **Disk:** Minimum 100GB SSD
- **Network:** High-speed connection for API calls

---

## Environment Setup

### 1. Java Installation

```bash
# Verify Java version
java -version  # Should be 21 or higher

# Set JAVA_HOME if not set
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

### 2. PostgreSQL Setup

```bash
# Install PostgreSQL (Ubuntu/Debian)
sudo apt-get update
sudo apt-get install postgresql-13

# Create database and user
sudo -u postgres psql
CREATE DATABASE aml_fraud_db;
CREATE USER aml_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE aml_fraud_db TO aml_user;
\q
```

### 3. Aerospike Setup

```bash
# Install Aerospike (Ubuntu/Debian)
wget -O aerospike-server.tgz https://download.aerospike.com/artifacts/aerospike-server-community/6.4.0.4/aerospike-server-community-6.4.0.4-ubuntu20.04.tgz
tar -xvf aerospike-server.tgz
cd aerospike-server-community-6.4.0.4-ubuntu20.04
sudo ./asinstall

# Start Aerospike
sudo systemctl start aerospike
sudo systemctl enable aerospike
```

---

## Database Configuration

### 1. Database Migration

The application uses Flyway for database migrations. Migrations are automatically applied on startup.

**Manual Migration (if needed):**
```bash
# Connect to database
psql -U aml_user -d aml_fraud_db

# Verify migrations
SELECT * FROM flyway_schema_history;
```

### 2. Initial Data Setup

**High-Risk Countries:**
```sql
INSERT INTO high_risk_countries (country_code, country_name, risk_level) VALUES
('AFG', 'Afghanistan', 'CRITICAL'),
('IRN', 'Iran', 'CRITICAL'),
('PRK', 'North Korea', 'CRITICAL'),
('SYR', 'Syria', 'CRITICAL');
```

**Default Roles:**
```sql
INSERT INTO roles (role_name, description) VALUES
('ADMIN', 'System Administrator'),
('COMPLIANCE_OFFICER', 'Compliance Officer'),
('ANALYST', 'Risk Analyst'),
('INVESTIGATOR', 'Case Investigator'),
('MLRO', 'Money Laundering Reporting Officer');
```

---

## Application Configuration

### 1. Environment Variables

Create `.env` file or set environment variables:

```bash
# Database Configuration
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aml_fraud_db
export SPRING_DATASOURCE_USERNAME=aml_user
export SPRING_DATASOURCE_PASSWORD=your_secure_password

# Server Configuration
export SERVER_PORT=2637
export SERVER_ADDRESS=0.0.0.0

# Aerospike Configuration
export AEROSPIKE_HOSTS=localhost:3000
export AEROSPIKE_NAMESPACE=aml_namespace

# AML/Fraud Detection
export AML_ENABLED=true
export FRAUD_ENABLED=true
export FRAUD_THRESHOLD_BLOCK=0.95
export FRAUD_THRESHOLD_HOLD=0.7

# Scoring Service
export SCORING_SERVICE_URL=http://localhost:8000

# JWT Configuration
export JWT_SECRET=your_jwt_secret_key_min_256_bits
export JWT_EXPIRATION=86400000

# Logging
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_COM_POSGATEWAY=DEBUG
```

### 2. application.properties

Key configuration sections in `src/main/resources/application.properties`:

```properties
# Server
server.port=${SERVER_PORT:2637}
server.address=${SERVER_ADDRESS:0.0.0.0}

# Database
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Aerospike
aerospike.hosts=${AEROSPIKE_HOSTS:localhost:3000}
aerospike.namespace=${AEROSPIKE_NAMESPACE:aml_namespace}

# AML Configuration
aml.enabled=${AML_ENABLED:true}
fraud.enabled=${FRAUD_ENABLED:true}
fraud.threshold.block=${FRAUD_THRESHOLD_BLOCK:0.95}
fraud.threshold.hold=${FRAUD_THRESHOLD_HOLD:0.7}
```

---

## Build and Deployment

### 1. Build Application

```bash
# Clone repository
git clone <repository-url>
cd AML_FRAU_DETECTOR

# Build with Maven
mvn clean package -DskipTests

# Build output: target/aml-fraud-detector-1.0.0.jar
```

### 2. Production Deployment

**Option A: Standalone JAR**
```bash
# Copy JAR to server
scp target/aml-fraud-detector-1.0.0.jar user@server:/opt/aml-fraud-detector/

# Run application
java -jar -Xmx4g -Xms2g \
  -Dspring.profiles.active=prod \
  aml-fraud-detector-1.0.0.jar
```

**Option B: Systemd Service**
```bash
# Create service file: /etc/systemd/system/aml-fraud-detector.service
[Unit]
Description=AML Fraud Detector Application
After=network.target postgresql.service aerospike.service

[Service]
Type=simple
User=aml_user
WorkingDirectory=/opt/aml-fraud-detector
ExecStart=/usr/bin/java -jar -Xmx4g -Xms2g \
  -Dspring.profiles.active=prod \
  /opt/aml-fraud-detector/aml-fraud-detector-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable aml-fraud-detector
sudo systemctl start aml-fraud-detector
```

**Option C: Docker**
```bash
# Build Docker image
docker build -t aml-fraud-detector:latest .

# Run container
docker run -d \
  --name aml-fraud-detector \
  -p 2637:2637 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/aml_fraud_db \
  -e SPRING_DATASOURCE_USERNAME=aml_user \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  aml-fraud-detector:latest
```

### 3. Reverse Proxy Configuration (Nginx)

```nginx
upstream aml_backend {
    server localhost:2637;
}

server {
    listen 80;
    server_name aml-api.yourdomain.com;

    location / {
        proxy_pass http://aml_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## Post-Deployment Verification

### 1. Health Checks

```bash
# Application health
curl http://localhost:2637/actuator/health

# Database connectivity
curl http://localhost:2637/actuator/health/db

# API documentation
curl http://localhost:2637/swagger-ui.html
```

### 2. API Testing

```bash
# Test transaction ingestion
curl -X POST http://localhost:2637/api/v1/transactions/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "TEST-001",
    "amountCents": 10000,
    "currency": "USD",
    "pan": "4242424242424242"
  }'

# Test authentication
curl -X POST http://localhost:2637/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 3. Verify Database Migrations

```sql
-- Check migration status
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- Verify tables exist
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;
```

---

## Monitoring and Maintenance

### 1. Application Logs

```bash
# View logs (systemd)
sudo journalctl -u aml-fraud-detector -f

# View logs (Docker)
docker logs -f aml-fraud-detector

# Log location (file-based)
tail -f /var/log/aml-fraud-detector/application.log
```

### 2. Metrics and Monitoring

**Prometheus Metrics:**
- Available at `/actuator/prometheus`
- Key metrics:
  - `aml.transactions.processed` - Total transactions processed
  - `aml.alerts.generated` - Total alerts generated
  - `aml.geographic.risk` - Geographic risk detections
  - `http.server.requests` - HTTP request metrics

**Health Endpoints:**
- `/actuator/health` - Overall health
- `/actuator/health/db` - Database health
- `/actuator/info` - Application info

### 3. Database Maintenance

```sql
-- Check table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Vacuum and analyze
VACUUM ANALYZE;

-- Check for slow queries (enable query logging first)
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;
```

### 4. Backup Strategy

```bash
# Database backup
pg_dump -U aml_user -d aml_fraud_db > backup_$(date +%Y%m%d).sql

# Restore database
psql -U aml_user -d aml_fraud_db < backup_20240106.sql
```

---

## Troubleshooting

### Common Issues

**1. Application won't start**
- Check Java version: `java -version`
- Verify database connectivity
- Check port availability: `netstat -tuln | grep 2637`
- Review logs for errors

**2. Database connection errors**
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check credentials in `application.properties`
- Verify database exists: `psql -U aml_user -l`

**3. Aerospike connection errors**
- Verify Aerospike is running: `sudo systemctl status aerospike`
- Check network connectivity: `telnet localhost 3000`
- Verify namespace exists

**4. High memory usage**
- Adjust JVM heap size: `-Xmx4g -Xms2g`
- Review connection pool settings
- Check for memory leaks in logs

**5. Slow performance**
- Enable query logging to identify slow queries
- Review database indexes
- Check Aerospike cache hit rates
- Monitor CPU and memory usage

### Log Analysis

```bash
# Search for errors
grep -i error /var/log/aml-fraud-detector/application.log

# Search for specific patterns
grep "OutOfMemoryError" /var/log/aml-fraud-detector/application.log

# Monitor real-time logs
tail -f /var/log/aml-fraud-detector/application.log | grep -i error
```

---

## Security Considerations

1. **Change default passwords** - Update all default credentials
2. **Enable HTTPS** - Use SSL/TLS certificates in production
3. **Firewall rules** - Restrict access to necessary ports only
4. **Regular updates** - Keep dependencies updated
5. **Audit logging** - Monitor audit logs regularly
6. **Backup encryption** - Encrypt database backups
7. **JWT secrets** - Use strong, randomly generated JWT secrets

---

## Support

For issues or questions:
- **Email:** support@posgateway.com
- **Documentation:** See `README.md` and `SWAGGER_OPENAPI_SETUP.md`
- **API Docs:** http://localhost:2637/swagger-ui.html

---

**Last Updated:** January 6, 2026


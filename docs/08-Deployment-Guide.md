# Deployment Guide
## AML Fraud Detector System

**Version:** 1.0  
**Date:** January 2026

---

## 1. Overview

This guide covers deployment of the AML Fraud Detector system in development, staging, and production environments.

### 1.1 Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Java | JDK 17 | JDK 21 |
| Memory | 8 GB | 16+ GB |
| CPU | 4 cores | 8+ cores |
| Disk | 50 GB | 200+ GB |
| OS | Ubuntu 20.04+ / RHEL 8+ / Windows Server 2019+ | Linux |

### 1.2 Required Services

| Service | Version | Purpose |
|---------|---------|---------|
| PostgreSQL | 13+ | Primary database |
| Aerospike | 6.0+ | Sanctions screening, caching |
| Redis | 6+ | Statistics, sessions |

---

## 2. Quick Start (Development)

### 2.1 Clone and Build

```bash
# Clone repository
git clone <repository-url>
cd AML_FRAU_DETECTOR

# Build with Maven
mvn clean package -DskipTests
```

### 2.2 Configure Environment

Create `.env` file or set environment variables:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aml_fraud_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=yourpassword

# Application
export SERVER_PORT=2637
export SPRING_PROFILES_ACTIVE=dev

# External Services
export SCORING_SERVICE_URL=http://localhost:8000
export AEROSPIKE_HOSTS=localhost:3000
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 2.3 Start Application

```bash
# Using Maven
mvn spring-boot:run

# Or using JAR
java -jar target/aml-fraud-detector-1.0.0-SNAPSHOT.jar
```

### 2.4 Verify Deployment

```bash
# Health check
curl http://localhost:2637/actuator/health

# Access dashboard
open http://localhost:2637
```

---

## 3. Production Deployment

### 3.1 System Architecture

```
                        ┌─────────────────┐
                        │  Load Balancer  │
                        │  (Nginx/HAProxy)│
                        └────────┬────────┘
                                 │
                ┌────────────────┼────────────────┐
                ▼                ▼                ▼
          ┌──────────┐    ┌──────────┐    ┌──────────┐
          │  App #1  │    │  App #2  │    │  App #3  │
          │  :2637   │    │  :2637   │    │  :2637   │
          └────┬─────┘    └────┬─────┘    └────┬─────┘
               │               │               │
               └───────────────┼───────────────┘
                               │
               ┌───────────────┼───────────────┐
               ▼               ▼               ▼
         ┌──────────┐   ┌──────────┐   ┌──────────┐
         │PostgreSQL│   │Aerospike │   │  Redis   │
         │ Primary  │   │ Cluster  │   │ Cluster  │
         └──────────┘   └──────────┘   └──────────┘
```

### 3.2 PostgreSQL Setup

```sql
-- Create database
CREATE DATABASE aml_fraud_db;

-- Create user
CREATE USER aml_user WITH ENCRYPTED PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE aml_fraud_db TO aml_user;

-- Connect and setup schema
\c aml_fraud_db

-- Run migration script
\i src/main/resources/db/migration/V1__Initial_Schema.sql
```

**postgresql.conf optimizations:**
```ini
# Connection settings
max_connections = 500
shared_buffers = 4GB
effective_cache_size = 12GB
work_mem = 256MB

# Write-ahead log
wal_buffers = 64MB
checkpoint_completion_target = 0.9

# Query planning
random_page_cost = 1.1
effective_io_concurrency = 200
```

### 3.3 Aerospike Setup

```bash
# Install Aerospike
# See: https://docs.aerospike.com/server/operations/install

# Configure namespace in aerospike.conf
namespace aml {
    replication-factor 2
    memory-size 4G
    default-ttl 30d
    
    storage-engine device {
        file /opt/aerospike/data/aml.dat
        filesize 16G
        write-block-size 128K
    }
}

namespace sanctions {
    replication-factor 2
    memory-size 2G
    default-ttl 0  # Never expire
    
    storage-engine memory
}
```

### 3.4 Redis Setup

```bash
# Redis configuration
# /etc/redis/redis.conf

maxmemory 2gb
maxmemory-policy allkeys-lru
appendonly yes

# For cluster
cluster-enabled yes
cluster-config-file nodes.conf
```

---

## 4. Application Configuration

### 4.1 Environment Variables Reference

#### Core Settings
```bash
# Server
SERVER_PORT=2637
SERVER_SERVLET_CONTEXT_PATH=/

# Profiles
SPRING_PROFILES_ACTIVE=prod
```

#### Database
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/aml_fraud_db
SPRING_DATASOURCE_USERNAME=aml_user
SPRING_DATASOURCE_PASSWORD=secure_password
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=100
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
```

#### Aerospike
```bash
AEROSPIKE_ENABLED=true
AEROSPIKE_HOSTS=aero1:3000,aero2:3000
AEROSPIKE_NAMESPACE=aml
AEROSPIKE_USERNAME=admin
AEROSPIKE_PASSWORD=secure_password
```

#### Redis
```bash
REDIS_ENABLED=true
REDIS_HOST=redis-host
REDIS_PORT=6379
REDIS_PASSWORD=secure_password
SPRING_DATA_REDIS_LETTUCE_POOL_MAX_ACTIVE=100
```

#### External Services
```bash
SCORING_SERVICE_URL=http://ml-scoring:8000
SUMSUB_API_URL=https://api.sumsub.com
SUMSUB_API_KEY=your_key
SUMSUB_SECRET_KEY=your_secret
```

#### Fraud Detection
```bash
FRAUD_THRESHOLD_BLOCK=0.95
FRAUD_THRESHOLD_HOLD=0.70
AML_HIGH_VALUE_THRESHOLD=1000000
```

### 4.2 application-prod.properties

```properties
# Server
server.port=${SERVER_PORT:2637}

# Database
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.hikari.maximum-pool-size=100
spring.datasource.hikari.minimum-idle=20
spring.datasource.hikari.connection-timeout=30000

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Logging
logging.level.root=INFO
logging.level.com.posgateway.aml=INFO
logging.file.name=/var/log/aml-fraud-detector/application.log

# Actuator
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=when_authorized
```

---

## 5. Deployment Options

### 5.1 JAR Deployment

```bash
# Build production JAR
mvn clean package -Pprod -DskipTests

# Run with explicit config
java -Xms4g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Dspring.profiles.active=prod \
     -jar target/aml-fraud-detector-1.0.0-SNAPSHOT.jar
```

### 5.2 Systemd Service

Create `/etc/systemd/system/aml-fraud-detector.service`:

```ini
[Unit]
Description=AML Fraud Detector Service
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=aml
Group=aml
WorkingDirectory=/opt/aml-fraud-detector
ExecStart=/usr/bin/java -Xms4g -Xmx8g -XX:+UseG1GC -jar aml-fraud-detector.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/aml-fraud-detector/stdout.log
StandardError=append:/var/log/aml-fraud-detector/stderr.log
Environment=SPRING_PROFILES_ACTIVE=prod
EnvironmentFile=/etc/aml-fraud-detector/env

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable aml-fraud-detector
sudo systemctl start aml-fraud-detector
sudo systemctl status aml-fraud-detector
```

### 5.3 Docker Deployment

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/aml-fraud-detector-*.jar app.jar

EXPOSE 2637

ENTRYPOINT ["java", "-Xms4g", "-Xmx8g", "-XX:+UseG1GC", "-jar", "app.jar"]
```

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "2637:2637"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/aml_fraud_db
      - SPRING_DATASOURCE_USERNAME=aml_user
      - SPRING_DATASOURCE_PASSWORD=secure_password
      - REDIS_HOST=redis
      - AEROSPIKE_HOSTS=aerospike:3000
    depends_on:
      - postgres
      - redis
      - aerospike

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: aml_fraud_db
      POSTGRES_USER: aml_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

  aerospike:
    image: aerospike/aerospike-server:6.4.0.2
    volumes:
      - aerospike_data:/opt/aerospike/data

volumes:
  postgres_data:
  redis_data:
  aerospike_data:
```

```bash
# Deploy with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f app
```

---

## 6. Reverse Proxy (Nginx)

```nginx
upstream aml_backend {
    least_conn;
    server 127.0.0.1:2637 weight=1;
    server 127.0.0.1:2638 weight=1;
    server 127.0.0.1:2639 weight=1;
}

server {
    listen 443 ssl http2;
    server_name aml.yourcompany.com;

    ssl_certificate /etc/ssl/certs/aml.crt;
    ssl_certificate_key /etc/ssl/private/aml.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://aml_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /actuator/health {
        proxy_pass http://aml_backend;
        allow 10.0.0.0/8;
        deny all;
    }
}
```

---

## 7. Monitoring Setup

### 7.1 Prometheus Configuration

Add to `prometheus.yml`:
```yaml
scrape_configs:
  - job_name: 'aml-fraud-detector'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app1:2637', 'app2:2637', 'app3:2637']
```

### 7.2 Grafana Dashboard

Import the dashboard from `grafana/dashboards/aml-fraud-detector.json`

Key panels:
- Transaction throughput
- Fraud detection latency
- Alert generation rate
- Database connection pool status
- JVM metrics

### 7.3 Log Aggregation

Configure log shipping to ELK or similar:

```xml
<!-- logback-spring.xml -->
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>${LOGSTASH_HOST}:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

---

## 8. Security Hardening

### 8.1 Checklist

- [ ] Use strong passwords for all services
- [ ] Enable TLS/SSL for all connections
- [ ] Configure firewall rules
- [ ] Enable database encryption at rest
- [ ] Set up audit logging
- [ ] Configure session timeouts
- [ ] Enable CSRF protection
- [ ] Set secure cookie flags

### 8.2 Firewall Rules

```bash
# Allow HTTPS
sudo ufw allow 443/tcp

# Allow internal services only from app servers
sudo ufw allow from 10.0.0.0/8 to any port 5432  # PostgreSQL
sudo ufw allow from 10.0.0.0/8 to any port 3000  # Aerospike
sudo ufw allow from 10.0.0.0/8 to any port 6379  # Redis

# Deny all other incoming
sudo ufw default deny incoming
sudo ufw enable
```

---

## 9. Backup & Recovery

### 9.1 Database Backup

```bash
#!/bin/bash
# /opt/scripts/backup-postgres.sh

BACKUP_DIR=/backup/postgres
DATE=$(date +%Y%m%d_%H%M%S)

pg_dump -h localhost -U aml_user aml_fraud_db | gzip > $BACKUP_DIR/aml_fraud_db_$DATE.sql.gz

# Keep last 30 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
```

### 9.2 Crontab

```bash
# Backup at 2 AM daily
0 2 * * * /opt/scripts/backup-postgres.sh
```

### 9.3 Recovery

```bash
# Restore from backup
gunzip -c aml_fraud_db_backup.sql.gz | psql -h localhost -U aml_user aml_fraud_db
```

---

## 10. Maintenance

### 10.1 Health Checks

```bash
# Application health
curl -s http://localhost:2637/actuator/health | jq

# Database connectivity
psql -h localhost -U aml_user -d aml_fraud_db -c "SELECT 1;"

# Redis connectivity
redis-cli -h localhost ping

# Aerospike connectivity
asinfo -h localhost
```

### 10.2 Log Rotation

Create `/etc/logrotate.d/aml-fraud-detector`:
```
/var/log/aml-fraud-detector/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 640 aml aml
    sharedscripts
    postrotate
        systemctl reload aml-fraud-detector
    endscript
}
```

### 10.3 Updating the Application

```bash
# Stop service
sudo systemctl stop aml-fraud-detector

# Backup current JAR
cp /opt/aml-fraud-detector/aml-fraud-detector.jar /opt/aml-fraud-detector/aml-fraud-detector.jar.bak

# Deploy new version
cp target/aml-fraud-detector-*.jar /opt/aml-fraud-detector/aml-fraud-detector.jar

# Start service
sudo systemctl start aml-fraud-detector

# Verify
sudo systemctl status aml-fraud-detector
curl http://localhost:2637/actuator/health
```

---

## 11. Troubleshooting

### 11.1 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| App won't start | Missing DB connection | Check DATASOURCE_URL |
| Slow queries | Missing indexes | Run index creation scripts |
| Out of memory | Heap too small | Increase -Xmx |
| Connection refused | Service not running | Check systemctl status |
| 503 errors | All instances down | Check health endpoints |

### 11.2 Logs Location

| Log | Location |
|-----|----------|
| Application | `/var/log/aml-fraud-detector/application.log` |
| PostgreSQL | `/var/log/postgresql/postgresql-*.log` |
| Nginx | `/var/log/nginx/access.log`, `/var/log/nginx/error.log` |

### 11.3 Debug Mode

```bash
# Enable debug logging temporarily
java -jar aml-fraud-detector.jar --logging.level.com.posgateway.aml=DEBUG
```

---

## Appendix: Quick Reference

### A. Default Ports

| Service | Port |
|---------|------|
| Application | 2637 |
| PostgreSQL | 5432 |
| Aerospike | 3000 |
| Redis | 6379 |
| ML Scoring | 8000 |

### B. Default Credentials (Change Immediately)

| Service | Username | Password |
|---------|----------|----------|
| Application | super.admin@aml.com | Admin@123 |
| PostgreSQL | aml_user | (set during install) |

### C. Useful Commands

```bash
# View application logs
tail -f /var/log/aml-fraud-detector/application.log

# Check service status
systemctl status aml-fraud-detector

# Check database connections
psql -c "SELECT count(*) FROM pg_stat_activity;"

# Check disk usage
df -h

# Check memory
free -h
```

# Environment Variables Configuration Guide

## Required Environment Variables

### Sumsub Configuration
```bash
# Enable Sumsub integration (set to true for production)
export SUMSUB_ENABLED=true

# Sumsub API Credentials (get these from Sumsub dashboard)
export SUMSUB_API_KEY=your_app_token_here
export SUMSUB_API_SECRET=your_secret_key_here
```

### Aerospike Configuration
```bash
# Aerospike enabled (default: true)
export AEROSPIKE_ENABLED=true

# Aerospike hosts (comma-separated if multiple)
export AEROSPIKE_HOSTS=localhost:3000

# Aerospike credentials (if authentication enabled)
export AEROSPIKE_USERNAME=your_username
export AEROSPIKE_PASSWORD=your_password
```

### Database Configuration
```bash
# PostgreSQL connection
export DATABASE_URL=jdbc:postgresql://localhost:5432/aml_fraud_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=your_password
```

### Encryption
```bash
# AES-256 encryption key for PII (32 bytes hex)
export ENCRYPTION_KEY=your_32_byte_hex_key_here
```

## Development Setup

### 1. Local Development (Sumsub Sandbox)
```bash
# Use Sumsub sandbox environment
export SUMSUB_ENABLED=true
export SUMSUB_API_KEY=sbx_your_sandbox_token
export SUMSUB_API_SECRET=sbx_your_sandbox_secret
```

### 2. Local Development (Aerospike Disabled)
```bash
# Disable Aerospike for local testing
export AEROSPIKE_ENABLED=false

# System will fall back to PostgreSQL only
```

### 3. Production Setup
```bash
# Production Sumsub credentials
export SUMSUB_ENABLED=true
export SUMSUB_API_KEY=prod_your_production_token
export SUMSUB_API_SECRET=prod_your_production_secret

# Production Aerospike cluster
export AEROSPIKE_HOSTS=aerospike-node1:3000,aerospike-node2:3000,aerospike-node3:3000
export AEROSPIKE_USERNAME=aml_app_user
export AEROSPIKE_PASSWORD=secure_password

# Production database
export DATABASE_URL=jdbc:postgresql://prod-db-host:5432/aml_fraud_prod
export DATABASE_USERNAME=aml_app_user
export DATABASE_PASSWORD=secure_db_password

# Production encryption key
export ENCRYPTION_KEY=<generate_secure_32_byte_key>
```

## How to Get Sumsub Credentials

1. Sign up at https://sumsub.com/
2. Navigate to Dashboard → Settings → API
3. Create a new App Token
4. Copy the App Token (SUMSUB_API_KEY)
5. Copy the Secret Key (SUMSUB_API_SECRET)

## Testing Configuration

### Test if Sumsub is configured:
```bash
curl -X GET https://api.sumsub.com/resources/applicants \
  -H "X-App-Token: ${SUMSUB_API_KEY}" \
  -H "X-App-Access-Sig: <signature>" \
  -H "X-App-Access-Ts: <timestamp>"
```

### Test if Aerospike is running:
```bash
asadm -h localhost:3000 -e "info"
```

## Security Best Practices

1. **Never commit credentials** to version control
2. **Use environment variables** for all secrets
3. **Rotate credentials** regularly
4. **Use different credentials** for dev/staging/production
5. **Encrypt sensitive data** in database using ENCRYPTION_KEY

## Docker Compose Example

```yaml
version: '3.8'
services:
  aml-fraud-detector:
    image: aml-fraud-detector:latest
    environment:
      - SUMSUB_ENABLED=true
      - SUMSUB_API_KEY=${SUMSUB_API_KEY}
      - SUMSUB_API_SECRET=${SUMSUB_API_SECRET}
      - AEROSPIKE_ENABLED=true
      - AEROSPIKE_HOSTS=aerospike:3000
      - DATABASE_URL=jdbc:postgresql://postgres:5432/aml_fraud_db
      - DATABASE_USERNAME=postgres
      - DATABASE_PASSWORD=${DB_PASSWORD}
      - ENCRYPTION_KEY=${ENCRYPTION_KEY}
```

## Verification Checklist

- [ ] SUMSUB_API_KEY is set and valid
- [ ] SUMSUB_API_SECRET is set and valid
- [ ] AEROSPIKE_HOSTS points to running Aerospike instance
- [ ] Database connection works
- [ ] ENCRYPTION_KEY is 32 bytes (64 hex characters)
- [ ] All environment variables load correctly on application startup

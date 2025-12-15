# Aerospike Database Research & Integration Guide

## Overview

Aerospike is a high-performance, distributed NoSQL database designed for real-time applications that require low latency, high throughput, and scalability. This document provides research findings and integration considerations for the AML Fraud Detector system.

## Key Features

### 1. High Performance
- **Low Latency**: Sub-millisecond read/write operations
- **High Throughput**: Can handle millions of transactions per second
- **Hybrid Memory Architecture (HMA)**: Combines RAM and SSD for optimal performance/cost
- **Predictable Performance**: Consistent latency even under high load

### 2. Scalability
- **Horizontal Scaling**: Add nodes seamlessly to scale
- **Data Volume Support**: From gigabytes to petabytes
- **No Re-platforming**: Scale without application changes
- **Distributed Architecture**: Built for large-scale deployments

### 3. High Availability
- **99.999% Uptime**: Mission-critical reliability
- **Fast Failover**: Automatic node failover
- **Replication**: Data replication across nodes
- **Cross-Datacenter Replication (XDR)**: Geographic redundancy

### 4. ACID Transactions (Aerospike 8+)
- **Distributed ACID Transactions**: Strict serializability
- **Consistency**: Data consistency across large-scale OLTP
- **Transactional Support**: Complex multi-record transactions

### 5. Multi-Model Support
- **Key-Value**: Traditional key-value operations
- **JSON Documents**: Document-based data model
- **Graph**: Graph database capabilities
- **Vector Search**: AI/ML vector similarity search

### 6. Vector Search (AI/ML)
- **Similarity Search**: Vector-based similarity queries
- **AI-Driven Applications**: Support for ML/AI workloads
- **Embeddings Support**: Store and search vector embeddings

## Why Aerospike for AML Fraud Detection?

### Performance Requirements
1. **30,000+ Concurrent Requests**: Aerospike can handle this scale easily
2. **Sub-millisecond Latency**: Critical for real-time fraud detection
3. **High Throughput**: Process millions of transactions efficiently
4. **Predictable Performance**: Consistent latency under load

### Use Cases in Fraud Detection

#### 1. Real-Time Velocity Checks
- **Fast Aggregations**: Track transaction velocity per card/merchant
- **Time-Window Queries**: Query transactions within time windows
- **In-Memory Performance**: RAM-like speed for velocity checks

#### 2. Feature Cache
- **Aggregate Features**: Cache velocity features, merchant profiles
- **Fast Lookups**: Sub-millisecond feature retrieval
- **High Availability**: 99.999% uptime ensures cache availability

#### 3. Transaction State Management
- **Session Management**: Track transaction state
- **Temporary Data**: Store intermediate processing results
- **Fast Cleanup**: Automatic TTL for temporary data

#### 4. ML Feature Storage
- **Feature Vectors**: Store extracted features for ML models
- **Vector Search**: Similarity search for fraud patterns
- **Batch Operations**: Efficient bulk feature storage

#### 5. Hot Data Storage
- **Recent Transactions**: Store hot/warm data in Aerospike
- **Cold Data in PostgreSQL**: Archive older data to PostgreSQL
- **Hybrid Architecture**: Best of both worlds

## Architecture Options

### Option 1: Primary Database (Replace PostgreSQL)
**Pros:**
- Single database system
- Highest performance
- Simplified architecture

**Cons:**
- Requires migration from PostgreSQL
- Different query model (NoSQL vs SQL)
- Learning curve

### Option 2: Hybrid Approach (Recommended)
**Aerospike for:**
- Real-time velocity checks
- Feature caching
- Hot transaction data
- Session/state management
- ML feature storage

**PostgreSQL for:**
- Cold/historical data
- Complex queries
- Reporting and analytics
- Compliance/audit logs

**Benefits:**
- Best performance for real-time operations
- PostgreSQL for complex analytics
- Cost-effective (use Aerospike for hot data only)

### Option 3: Cache Layer (Complement to PostgreSQL)
**Aerospike as:**
- High-performance cache layer
- Velocity feature store
- Session state store
- Real-time aggregation store

**PostgreSQL as:**
- Primary database
- Source of truth
- Long-term storage

## Performance Characteristics

### Latency Comparison
- **Aerospike**: < 1ms (typical), < 5ms (p99)
- **PostgreSQL**: 1-10ms (typical), 10-50ms (p99)
- **Redis**: < 1ms (similar to Aerospike for cache)

### Throughput Comparison
- **Aerospike**: Millions of TPS per node
- **PostgreSQL**: 10K-100K TPS per instance
- **Scales**: Aerospike scales horizontally easier

### Storage Efficiency
- **Aerospike HMA**: Uses SSD + RAM efficiently
- **Cost**: Lower than pure RAM solutions
- **Persistence**: Data persists to SSD automatically

## Integration with Spring Boot

### Maven Dependency
```xml
<dependency>
    <groupId>com.aerospike</groupId>
    <artifactId>aerospike-client</artifactId>
    <version>8.1.0</version>
</dependency>
```

### Spring Data Aerospike (Alternative)
```xml
<dependency>
    <groupId>com.aerospike</groupId>
    <artifactId>spring-data-aerospike</artifactId>
    <version>3.4.0</version>
</dependency>
```

### Configuration Example
```properties
# Aerospike Configuration
aerospike.hosts=${AEROSPIKE_HOSTS:localhost:3000}
aerospike.namespace=${AEROSPIKE_NAMESPACE:test}
aerospike.set=${AEROSPIKE_SET:transactions}
aerospike.timeout=${AEROSPIKE_TIMEOUT:1000}
aerospike.retries=${AEROSPIKE_RETRIES:3}
```

## Use Case Examples for AML Fraud Detection

### 1. Velocity Feature Caching
```java
// Store velocity features
String key = "velocity:" + panHash + ":" + timeWindow;
VelocityFeatures features = new VelocityFeatures(count, amount, merchants);
aerospikeClient.put(null, key, bins);

// Retrieve with <1ms latency
Record record = aerospikeClient.get(null, key);
```

### 2. Real-Time Aggregation
```java
// Increment counters atomically
aerospikeClient.operate(null, key, 
    Operation.increment(bin("txn_count", 1)),
    Operation.increment(bin("total_amount", amount))
);
```

### 3. Transaction State Tracking
```java
// Store transaction state with TTL
WritePolicy writePolicy = new WritePolicy();
writePolicy.expiration = 300; // 5 minutes TTL
aerospikeClient.put(writePolicy, key, bins);
```

### 4. Batch Operations
```java
// Batch read velocity features for multiple cards
Key[] keys = panHashes.stream()
    .map(hash -> new Key(namespace, set, "velocity:" + hash))
    .toArray(Key[]::new);
Record[] records = aerospikeClient.get(null, keys);
```

## Comparison with Current Architecture

### Current (PostgreSQL Only)
- **Latency**: 5-50ms per query
- **Throughput**: Limited by database
- **Scaling**: Vertical scaling required
- **Cost**: Lower initial cost

### With Aerospike Hybrid
- **Latency**: < 1ms for hot data, < 5ms p99
- **Throughput**: Millions of TPS
- **Scaling**: Horizontal scaling
- **Cost**: Higher but justified for performance

## Implementation Recommendations

### Phase 1: Cache Layer (Quick Win)
1. Deploy Aerospike as cache layer
2. Cache velocity features
3. Cache aggregate features
4. Measure performance improvement

### Phase 2: Hot Data Storage
1. Store recent transactions (last 24 hours) in Aerospike
2. Use for real-time queries
3. Archive to PostgreSQL periodically

### Phase 3: Full Integration
1. Move all real-time operations to Aerospike
2. Use PostgreSQL for analytics/reporting
3. Implement data sync between systems

## Configuration for 30K+ Concurrent Requests

### Aerospike Cluster Setup
```properties
# Cluster Configuration
aerospike.cluster.name=${AEROSPIKE_CLUSTER:aml-fraud-cluster}
aerospike.cluster.nodes=${AEROSPIKE_NODES:node1:3000,node2:3000,node3:3000}

# Performance Tuning
aerospike.connection.pool.size=${AEROSPIKE_POOL_SIZE:300}
aerospike.async.max.commands=${AEROSPIKE_ASYNC_MAX:1000}
aerospike.batch.max=${AEROSPIKE_BATCH_MAX:1000}

# Namespace Configuration
aerospike.namespace.default=${AEROSPIKE_NAMESPACE:aml_data}
aerospike.namespace.features=${AEROSPIKE_NAMESPACE_FEATURES:features}
aerospike.namespace.cache=${AEROSPIKE_NAMESPACE_CACHE:cache}
```

## Cost Considerations

### Aerospike Community Edition (Free)
- Unlimited nodes
- All features except:
  - Cross-datacenter replication
  - LDAP integration
  - Some enterprise features

### Aerospike Enterprise Edition (Paid)
- Full feature set
- Support included
- Cross-datacenter replication
- Additional enterprise features

### Hybrid Architecture Cost
- **Aerospike**: For hot data (last 24-48 hours)
- **PostgreSQL**: For cold data (historical)
- **Cost Savings**: Store less in Aerospike, more in PostgreSQL

## Migration Path

### Step 1: Parallel Write
- Write to both PostgreSQL and Aerospike
- Read from PostgreSQL (existing)
- Verify Aerospike data consistency

### Step 2: Gradual Read Migration
- Read hot data from Aerospike
- Read cold data from PostgreSQL
- Monitor performance and accuracy

### Step 3: Optimization
- Optimize Aerospike data model
- Tune performance parameters
- Scale cluster as needed

## Monitoring & Operations

### Key Metrics
- **Latency**: P50, P95, P99 latencies
- **Throughput**: Operations per second
- **Cluster Health**: Node status, replication
- **Memory Usage**: RAM and SSD utilization
- **Connection Pool**: Active connections

### Aerospike Tools
- **Aerospike Monitoring Console**: Web-based monitoring
- **asadm**: Command-line admin tool
- **asbackup/asrestore**: Backup and restore
- **asinfo**: Cluster information

## Security Features

### Authentication
- LDAP integration (Enterprise)
- Role-based access control
- User management

### Encryption
- TLS/SSL encryption
- Encryption at rest
- Network encryption

### Compliance
- Audit logging
- Data retention policies
- Compliance features

## Resources & Documentation

### Official Resources
- **Website**: https://aerospike.com
- **Documentation**: https://docs.aerospike.com
- **GitHub**: https://github.com/aerospike
- **Java Client**: https://github.com/aerospike/aerospike-client-java

### Spring Boot Integration
- **Spring Data Aerospike**: Community project
- **Client Libraries**: Official Java client
- **Examples**: GitHub examples repository

## Conclusion

### Best Fit For:
1. **Real-time velocity checks** - Sub-millisecond performance
2. **Feature caching** - High-speed feature retrieval
3. **Hot data storage** - Recent transaction data
4. **High-throughput operations** - 30K+ concurrent requests
5. **Scalability** - Horizontal scaling

### Recommended Approach:
**Hybrid Architecture** with Aerospike for hot data/real-time operations and PostgreSQL for cold data/analytics provides the best balance of performance, cost, and functionality.

### Next Steps:
1. Evaluate Aerospike Community Edition
2. Set up test cluster
3. Implement cache layer pilot
4. Measure performance improvements
5. Plan full integration if successful


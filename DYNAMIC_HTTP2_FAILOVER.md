# Dynamic HTTP/2 Auto-Detection and Failover

## Overview

This document describes the dynamic HTTP/2 support with automatic detection, health monitoring, and failover to HTTP/1.1 when issues are detected.

## Features

### 1. Auto-Detection
- **Hardware/Software Capability Detection**: Automatically detects if the system supports HTTP/2
- **JVM Version Check**: Verifies Java version supports HTTP/2 (Java 9+)
- **Server Support Check**: Tests if server can handle HTTP/2 requests
- **Automatic Enable/Disable**: Dynamically enables HTTP/2 if supported

### 2. Health Monitoring
- **Connection Drop Tracking**: Monitors and counts connection drops
- **Error Rate Monitoring**: Tracks error rate percentage
- **Latency Monitoring**: Monitors average request latency
- **Consecutive Drop Tracking**: Counts consecutive connection failures
- **Health Status**: HEALTHY, DEGRADED, or CRITICAL

### 3. Automatic Failover
- **Auto-Failover**: Automatically switches to HTTP/1.1 when issues detected
- **Retry Logic**: Periodically retries HTTP/2 after failover
- **Failover Triggers**:
  - Consecutive drops exceed threshold (default: 10)
  - Error rate exceeds threshold (default: 5%)
  - Average latency exceeds threshold (default: 1000ms)
- **Retry After Failover**: Automatically retries HTTP/2 after configured interval

### 4. Protocol Comparison
- **Performance Metrics**: Tracks HTTP/2 vs HTTP/1.1 performance
- **Error Rate Comparison**: Compares error rates between protocols
- **Latency Comparison**: Compares latencies between protocols
- **Recommendations**: Provides recommendations based on metrics

## Services

### Http2DetectionService

**Purpose**: Detects if HTTP/2 is supported by hardware/software

**Key Methods**:
- `detectHttp2Support()`: Main detection method
- `isHttp2Supported()`: Get current support status
- `refreshDetection()`: Force refresh of detection

**Detection Checks**:
1. System/JVM HTTP/2 support (Java 9+)
2. Server HTTP/2 support (connection test)
3. ALPN (Application-Layer Protocol Negotiation) availability

### Http2HealthMonitorService

**Purpose**: Monitors HTTP/2 connection health and detects problems

**Metrics Tracked**:
- Total HTTP/2 requests
- Total HTTP/2 errors
- Total connection drops
- Average latency
- Consecutive drops
- Error rate percentage

**Health Status**:
- **HEALTHY**: No issues detected
- **DEGRADED**: Elevated error rate or latency
- **CRITICAL**: Multiple consecutive drops

**Key Methods**:
- `recordRequest(latency)`: Record successful request
- `recordError(type)`: Record error
- `recordDrop()`: Record connection drop
- `shouldFailover()`: Check if failover needed
- `getHealthMetrics()`: Get current metrics

### Http2FailoverService

**Purpose**: Manages automatic failover between HTTP/2 and HTTP/1.1

**Failover Logic**:
1. Monitors HTTP/2 health continuously
2. Triggers failover when health is CRITICAL or DEGRADED with issues
3. Switches to HTTP/1.1 immediately
4. Periodically retries HTTP/2 after configured interval
5. Re-enables HTTP/2 if retry successful

**Key Methods**:
- `shouldUseHttp2()`: Determine if HTTP/2 should be used
- `performFailover()`: Manually trigger failover
- `getCurrentProtocol()`: Get current protocol
- `getFailoverStats()`: Get failover statistics

## Configuration

### application.properties

```properties
# HTTP/2 Auto-Detection Configuration
http2.auto.detect.enabled=true
http2.detection.test.url=http://localhost:8080/actuator/health
http2.detection.timeout.ms=5000
http2.detection.refresh.interval.seconds=300

# HTTP/2 Health Monitoring Configuration
http2.health.monitoring.enabled=true
http2.health.check.interval.seconds=30
http2.failover.drop.threshold=10
http2.failover.error.rate.threshold=0.05
http2.failover.latency.threshold.ms=1000

# HTTP/2 Failover Configuration
http2.failover.enabled=true
http2.failover.retry.interval.seconds=300
http2.failover.min.retry.interval.seconds=60
```

### Environment Variables

```bash
export HTTP2_AUTO_DETECT_ENABLED=true
export HTTP2_DETECTION_TEST_URL=http://localhost:8080/actuator/health
export HTTP2_DETECTION_TIMEOUT=5000
export HTTP2_DETECTION_REFRESH_INTERVAL=300

export HTTP2_HEALTH_MONITORING_ENABLED=true
export HTTP2_HEALTH_CHECK_INTERVAL=30
export HTTP2_FAILOVER_DROP_THRESHOLD=10
export HTTP2_FAILOVER_ERROR_RATE_THRESHOLD=0.05
export HTTP2_FAILOVER_LATENCY_THRESHOLD=1000

export HTTP2_FAILOVER_ENABLED=true
export HTTP2_FAILOVER_RETRY_INTERVAL=300
export HTTP2_FAILOVER_MIN_RETRY_INTERVAL=60
```

## API Endpoints

### Get HTTP/2 Status
```
GET /api/v1/http2/status
```

**Response**:
```json
{
  "currentProtocol": "HTTP/2",
  "http2Enabled": true,
  "failoverActive": false,
  "failoverCount": 0,
  "lastFailoverTime": 0,
  "lastRetryTime": 0
}
```

### Get HTTP/2 Health Metrics
```
GET /api/v1/http2/health
```

**Response**:
```json
{
  "totalRequests": 1000,
  "totalErrors": 5,
  "totalDrops": 2,
  "errorRate": 0.005,
  "averageLatency": 45,
  "consecutiveDrops": 0,
  "healthStatus": "HEALTHY",
  "lastHealthCheckTime": 1234567890
}
```

### Get Protocol Comparison
```
GET /api/v1/http2/comparison
```

**Response**:
```json
{
  "currentProtocol": "HTTP/2",
  "http2Health": "HEALTHY",
  "http2ErrorRate": 0.5,
  "http2AverageLatency": 45,
  "http2ConnectionDrops": 2,
  "failoverActive": false,
  "recommendation": "HTTP/2 is healthy"
}
```

### Trigger Detection Refresh
```
POST /api/v1/http2/detect
```

### Trigger Manual Failover
```
POST /api/v1/http2/failover
```

### Reset Health Metrics
```
POST /api/v1/http2/reset
```

## How It Works

### 1. Initial Detection

1. Application starts
2. `Http2DetectionService` runs detection
3. Checks JVM version, system support, server support
4. If all checks pass, HTTP/2 is enabled
5. If any check fails, HTTP/1.1 is used

### 2. Runtime Monitoring

1. Every request records metrics (latency, success/failure)
2. `Http2HealthMonitorService` tracks:
   - Error rates
   - Connection drops
   - Average latency
   - Consecutive failures
3. Health status updated based on metrics
4. Periodic health checks (every 30 seconds)

### 3. Failover Process

1. Health monitor detects issues (CRITICAL or DEGRADED)
2. `Http2FailoverService` triggers failover
3. Immediately switches to HTTP/1.1
4. Records failover time and count
5. Resets health metrics for fresh start

### 4. Retry Process

1. After failover, waits for retry interval (default: 5 minutes)
2. Re-detects HTTP/2 support
3. If detection passes, re-enables HTTP/2
4. Monitors health closely after re-enable
5. If issues persist, fails back to HTTP/1.1

## Failover Triggers

### Critical Failover (Immediate)
- Consecutive connection drops >= 10
- Health status = CRITICAL

### Degraded Failover (After Monitoring)
- Error rate >= 5% over monitoring period
- Average latency >= 1000ms
- Health status = DEGRADED with multiple drops

## Monitoring and Troubleshooting

### Key Metrics to Monitor

1. **Protocol Status**
   - Current protocol (HTTP/2 or HTTP/1.1)
   - Failover active status
   - Failover count

2. **Health Metrics**
   - Total requests
   - Error rate
   - Connection drops
   - Average latency

3. **Failover Statistics**
   - Number of failovers
   - Last failover time
   - Last retry time

### Troubleshooting

**HTTP/2 Not Detected:**
1. Check Java version (requires Java 9+)
2. Verify server HTTP/2 support
3. Check network connectivity
4. Review detection logs

**Frequent Failovers:**
1. Check error rate metrics
2. Monitor connection drops
3. Review latency metrics
4. Check network stability
5. Consider adjusting thresholds

**High Error Rate:**
1. Check server logs
2. Monitor network conditions
3. Verify HTTP/2 compatibility
4. Consider disabling HTTP/2 if unstable

## Best Practices

1. **Monitor Metrics**
   - Regularly check HTTP/2 health endpoints
   - Monitor failover statistics
   - Track error rates

2. **Adjust Thresholds**
   - Tune failover thresholds based on environment
   - Balance between stability and performance
   - Consider network conditions

3. **Test Failover**
   - Test failover triggers
   - Verify retry logic
   - Ensure smooth transitions

4. **Logging**
   - Enable debug logging for HTTP/2 operations
   - Monitor failover events
   - Track health status changes

5. **Gradual Rollout**
   - Start with auto-detection enabled
   - Monitor metrics closely
   - Adjust thresholds as needed

## Background Network Stability Monitoring

### Http2NetworkStabilityService

**Purpose**: Background service that continuously monitors network stability

**Key Features**:
- **Regular Network Checks**: Runs every 30 seconds (configurable)
- **Connectivity Testing**: Tests network connectivity and response times
- **Stability Thresholds**: Requires multiple consecutive successes before considering network stable
- **Failure Detection**: Detects network instability quickly

**Stability Checks**:
1. Connectivity test (HTTP request to test URL)
2. Response time check (must be under timeout)
3. Response code validation (200-299)
4. Consecutive success tracking

**Status Types**:
- **STABLE**: Network is stable, safe for HTTP/2
- **TESTING**: Network stability being determined
- **UNSTABLE**: Network has issues, not safe for HTTP/2

### Automatic HTTP/2 Retry

**Enhanced Retry Logic**:
1. Background process monitors network stability continuously
2. When failover is active, checks network stability regularly
3. Only retries HTTP/2 when network is stable
4. Performs comprehensive checks before re-enabling:
   - Network stability confirmation
   - HTTP/2 capability re-detection
   - Connectivity test
   - Health metrics reset

**Retry Conditions**:
- Network stability confirmed (configurable threshold)
- Minimum time since last retry elapsed
- HTTP/2 still supported
- Connectivity test passes

### New Configuration

```properties
# Network Stability Monitoring (Background Process)
http2.network.stability.enabled=true
http2.network.stability.check.interval.seconds=30
http2.network.stability.success.threshold=5
http2.network.stability.failure.threshold=3

# Automatic Retry Configuration
http2.failover.auto.retry.enabled=true
```

## New API Endpoints

### Get Network Stability Metrics
```
GET /api/v1/http2/network/stability
```

**Response**:
```json
{
  "totalTests": 100,
  "successfulTests": 95,
  "successRate": 0.95,
  "consecutiveSuccesses": 5,
  "consecutiveFailures": 0,
  "networkStable": true,
  "stabilityStatus": "STABLE",
  "lastCheckTime": 1234567890
}
```

### Get Comprehensive Status
```
GET /api/v1/http2/comprehensive
```

**Response**: Includes failover stats, health metrics, network stability, and recommendations

## How Automatic Retry Works

### 1. Background Monitoring
- Network stability service runs every 30 seconds
- Tests connectivity continuously
- Tracks consecutive successes/failures
- Updates stability status

### 2. Failover Active State
- When HTTP/2 fails, system switches to HTTP/1.1
- Background process continues monitoring
- Waits for network to stabilize
- Monitors health of HTTP/1.1 connections

### 3. Network Stabilization
- Network stability service detects improvement
- Requires configurable number of consecutive successes (default: 5)
- Updates status to STABLE
- Triggers retry evaluation

### 4. Automatic Retry Process
- Failover service checks network stability
- If stable, performs HTTP/2 re-detection
- Tests connectivity
- Re-enables HTTP/2 if all checks pass
- Resets health metrics for fresh monitoring

### 5. Continuous Monitoring
- After re-enabling HTTP/2, continues monitoring
- Tracks HTTP/2 health metrics
- If issues recur, fails back to HTTP/1.1
- Process repeats as needed

## Benefits

1. **Automatic Recovery**: No manual intervention needed
2. **Intelligent Retry**: Only retries when network is stable
3. **Better Security**: HTTP/2 benefits restored when safe
4. **Performance Optimization**: Automatically uses faster protocol
5. **Resilient**: Handles network fluctuations gracefully

## Summary

**Dynamic HTTP/2 Features:**
- ✅ Auto-detection of HTTP/2 capability
- ✅ Automatic enable/disable based on support
- ✅ Health monitoring with multiple metrics
- ✅ Automatic failover to HTTP/1.1
- ✅ **Background network stability monitoring**
- ✅ **Automatic intelligent retry of HTTP/2**
- ✅ **Network stability-based retry decisions**
- ✅ Protocol comparison and recommendations
- ✅ REST API for monitoring and control
- ✅ Configurable thresholds and intervals
- ✅ All configurations externalized

**All configurations are externalized - no hardcoding!**

**Background Process Features:**
- ✅ Continuous network stability monitoring (every 30 seconds)
- ✅ Automatic HTTP/2 retry when network stabilizes
- ✅ Intelligent retry logic based on network health
- ✅ Comprehensive status monitoring
- ✅ No manual intervention required


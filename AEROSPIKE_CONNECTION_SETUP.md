# Aerospike Connection Service Setup

## Overview

Aerospike connection service is set up as a singleton for application-wide access. The connection manager maintains connections and ensures they stay alive. **Note: This is setup only - not integrated yet. Will be used when replacing PostgreSQL with Aerospike.**

## Architecture

### 1. AerospikeConnectionService (Singleton)
- **Purpose**: Provides application-wide access to Aerospike client
- **Pattern**: Spring Singleton Bean (one instance per application)
- **Features**:
  - Single Aerospike client instance
  - Connection lifecycle management
  - Automatic reconnection on failure
  - Thread-safe access
  - Connection status monitoring

### 2. AerospikeConnectionManager (Background Service)
- **Purpose**: Monitors and maintains Aerospike connections
- **Pattern**: Scheduled background service
- **Features**:
  - Periodic health checks (every 30 seconds)
  - Automatic reconnection if connection drops
  - Connection keep-alive
  - Status monitoring

## Configuration

### application.properties

```properties
# Aerospike Configuration (Future PostgreSQL Replacement)
aerospike.enabled=false  # Set to true when ready to use
aerospike.hosts=localhost:3000  # Comma-separated hosts
aerospike.username=  # Optional authentication
aerospike.password=  # Optional authentication
aerospike.connection.timeout=5000
aerospike.max.socket.idle=55
aerospike.tend.interval=1000
aerospike.connection.health.check.interval.seconds=30
aerospike.connection.keepalive.enabled=true
```

### Environment Variables

```bash
export AEROSPIKE_ENABLED=false
export AEROSPIKE_HOSTS=localhost:3000,node2:3000,node3:3000
export AEROSPIKE_USERNAME=admin
export AEROSPIKE_PASSWORD=secret
export AEROSPIKE_CONNECTION_TIMEOUT=5000
export AEROSPIKE_HEALTH_CHECK_INTERVAL=30
```

## Usage (When Enabled)

### Accessing Aerospike Client

```java
@Autowired
private AerospikeConnectionService aerospikeConnectionService;

public void someMethod() {
    // Get Aerospike client (singleton instance)
    AerospikeClient client = aerospikeConnectionService.getClient();
    
    // Use client for operations
    // ...
}
```

### Checking Connection Status

```java
@Autowired
private AerospikeConnectionService aerospikeConnectionService;

public void checkStatus() {
    boolean isEnabled = aerospikeConnectionService.isEnabled();
    boolean isConnected = aerospikeConnectionService.isConnected();
    
    if (isEnabled && isConnected) {
        // Safe to use Aerospike
    }
}
```

## Connection Lifecycle

### 1. Initialization (@PostConstruct)
- Service bean is created by Spring
- `initialize()` method is called automatically
- Aerospike client is created (singleton)
- Connection is established
- Status is verified

### 2. Runtime (Background Monitoring)
- ConnectionManager runs health checks every 30 seconds
- Verifies connection is alive
- Reconnects automatically if connection is lost
- Logs connection status

### 3. Shutdown (@PreDestroy)
- Service bean is destroyed by Spring
- `shutdown()` method is called automatically
- Aerospike client connection is closed
- Resources are cleaned up

## API Endpoints

### Get Connection Status
```
GET /api/v1/aerospike/status
```

**Response**:
```json
{
  "enabled": false,
  "connected": false,
  "hosts": "localhost:3000",
  "clientInitialized": false
}
```

### Check if Enabled
```
GET /api/v1/aerospike/enabled
```

**Response**:
```json
{
  "enabled": false,
  "connected": false
}
```

### Force Reconnect
```
POST /api/v1/aerospike/reconnect
```

### Get Connection Info
```
GET /api/v1/aerospike/info
```

## Singleton Pattern

### Why Singleton?
- **Single Connection Pool**: One Aerospike client instance for entire application
- **Resource Efficiency**: Reduces memory usage and connection overhead
- **Thread Safety**: Aerospike client is thread-safe
- **Global Access**: Available throughout application via Spring injection

### Implementation
- Spring's `@Service` annotation creates singleton by default
- Single instance shared across all beans
- Thread-safe access
- Lifecycle managed by Spring container

## Connection Management

### Automatic Keep-Alive
- Background process checks connection every 30 seconds
- Automatically reconnects if connection is lost
- No manual intervention needed
- Configurable check interval

### Connection Pooling
- Aerospike client manages internal connection pool
- Automatic connection reuse
- Efficient resource utilization
- Thread-safe connection access

### Reconnection Logic
- Detects connection loss automatically
- Attempts reconnection immediately
- Creates new client if needed
- Updates connection status

## Status Monitoring

### Connection Status States
- **Enabled**: Aerospike is configured and enabled
- **Initialized**: Client has been created
- **Connected**: Active connection to Aerospike cluster
- **Disconnected**: Connection lost, attempting reconnect

### Health Checks
- Periodic connection verification
- Automatic reconnection on failure
- Status logging
- Metrics tracking

## Current State

**Status**: ✅ **Setup Complete - Not Integrated Yet**

- ✅ Aerospike dependency added to pom.xml
- ✅ Connection service created (singleton)
- ✅ Connection manager created (background monitoring)
- ✅ Configuration properties added
- ✅ Monitoring endpoints created
- ⏸️ **Disabled by default** (`aerospike.enabled=false`)
- ⏸️ **Not used in any services yet** (will integrate when replacing PostgreSQL)

## Next Steps (When Ready)

1. Enable Aerospike: Set `aerospike.enabled=true`
2. Configure hosts: Set `aerospike.hosts=your-hosts:3000`
3. Test connection: Use `/api/v1/aerospike/status` endpoint
4. Integrate services: Replace PostgreSQL calls with Aerospike calls
5. Monitor: Use connection manager to ensure stability

## Benefits

1. **Single Instance**: One client for entire application
2. **Automatic Management**: Background service keeps connections alive
3. **Resilience**: Automatic reconnection on failure
4. **Monitoring**: Status endpoints for visibility
5. **Ready to Use**: When enabled, just inject and use

## Notes

- **Not Active**: Service is disabled by default
- **No Impact**: Current PostgreSQL usage is unaffected
- **Future Ready**: Easy to enable when ready to migrate
- **Singleton Access**: One client instance shared application-wide


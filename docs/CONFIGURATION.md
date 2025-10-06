# Configuration Reference

Complete configuration reference for the Firefly Common Client Library.

> **Note:** This documentation reflects the actual configuration properties defined in `ServiceClientProperties.java`. All examples and default values are accurate as of the current codebase version.

## Table of Contents

- [First Time Configuration](#first-time-configuration)
- [Overview](#overview)
- [Configuration Properties](#configuration-properties)
- [REST Configuration](#rest-configuration)
- [gRPC Configuration](#grpc-configuration)
- [Circuit Breaker Configuration](#circuit-breaker-configuration)
- [Retry Configuration](#retry-configuration)
- [Metrics Configuration](#metrics-configuration)
- [Security Configuration](#security-configuration)
- [Environment-Specific Configuration](#environment-specific-configuration)
- [Programmatic Configuration](#programmatic-configuration)

## First Time Configuration

If you're setting up the library for the first time, start with this minimal configuration:

### Minimal Configuration (Development)

Create `src/main/resources/application.yml`:

```yaml
firefly:
  service-client:
    enabled: true                    # Enable the library
    environment: DEVELOPMENT         # Use development defaults

    rest:
      logging-enabled: true          # See request/response logs for debugging
```

That's it! The library will use sensible defaults for everything else.

### Recommended Configuration (Development)

For better control during development:

```yaml
firefly:
  service-client:
    enabled: true
    default-timeout: 30s             # Global timeout for all clients
    environment: DEVELOPMENT

    rest:
      max-connections: 50            # Smaller pool for development
      response-timeout: 30s
      logging-enabled: true          # Enable logging for debugging
      compression-enabled: true

    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50.0   # Open circuit after 50% failures
      minimum-number-of-calls: 5     # Need 5 calls before evaluating
```

### Production Configuration

For production environments:

```yaml
firefly:
  service-client:
    enabled: true
    default-timeout: 30s
    environment: PRODUCTION          # Use production optimizations

    rest:
      max-connections: 200           # Larger pool for production traffic
      response-timeout: 30s
      logging-enabled: false         # Disable verbose logging
      compression-enabled: true

    grpc:
      use-plaintext-by-default: false  # Enable TLS in production
      max-concurrent-streams: 200

    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50.0
      minimum-number-of-calls: 10

    security:
      tls-enabled: true              # Enable TLS
```

### Understanding Configuration Levels

Configuration can be applied at three levels:

1. **Global Defaults** (via `application.yml`) - Applies to all clients
2. **Programmatic** (via builder) - Applies to specific client instance
3. **Environment-Specific** - Automatic adjustments based on `environment` setting

**Priority:** Programmatic > Global Defaults > Environment-Specific

**Example:**
```yaml
# Global default timeout
firefly:
  service-client:
    default-timeout: 30s
```

```java
// This client overrides the global timeout
ServiceClient client = ServiceClient.rest("my-service")
    .baseUrl("http://localhost:8080")
    .timeout(Duration.ofSeconds(60))  // Overrides global 30s
    .build();
```

## Overview

The library uses Spring Boot's configuration system with the prefix `firefly.service-client`. All properties can be configured via:

- `application.yml` or `application.properties`
- Environment variables (e.g., `FIREFLY_SERVICE_CLIENT_ENABLED=true`)
- Command-line arguments (e.g., `--firefly.service-client.enabled=true`)
- Programmatic configuration via Java beans

### Configuration Best Practices

1. **Start Simple**: Begin with minimal configuration and add properties as needed
2. **Use Environment Profiles**: Leverage `environment: DEVELOPMENT/TESTING/PRODUCTION` for automatic optimizations
3. **Enable Logging in Dev**: Set `rest.logging-enabled: true` during development
4. **Tune for Production**: Increase connection pools and disable verbose logging
5. **Monitor Circuit Breakers**: Adjust thresholds based on your service's behavior

## Configuration Properties

### Global Settings

```yaml
firefly:
  service-client:
    enabled: true                    # Enable/disable auto-configuration (default: true)
    default-timeout: 30s             # Default timeout for all operations (default: 30s)
    environment: DEVELOPMENT         # Environment: DEVELOPMENT, TESTING, PRODUCTION (default: DEVELOPMENT)
    
    # Global default headers applied to all requests
    default-headers:
      User-Agent: "MyApp/1.0"
      X-Client-Version: "1.0.0"
```

## REST Configuration

```yaml
firefly:
  service-client:
    rest:
      # Connection Pool Settings
      max-connections: 100           # Maximum number of connections (default: 100)
      max-idle-time: 5m              # Maximum idle time for connections (default: 5m)
      max-life-time: 30m             # Maximum lifetime for connections (default: 30m)
      pending-acquire-timeout: 10s   # Timeout for acquiring connection from pool (default: 10s)
      
      # Timeout Settings
      response-timeout: 30s          # Response timeout (default: 30s)
      connect-timeout: 10s           # Connection timeout (default: 10s)
      read-timeout: 30s              # Read timeout (default: 30s)
      
      # HTTP Settings
      compression-enabled: true      # Enable HTTP compression (default: true)
      follow-redirects: true         # Follow HTTP redirects (default: true)
      max-in-memory-size: 1048576    # Max in-memory buffer size in bytes (default: 1MB)
      
      # Retry Settings
      max-retries: 3                 # Maximum retry attempts (default: 3)
      
      # Content Type Settings
      default-content-type: "application/json"  # Default Content-Type header
      default-accept-type: "application/json"   # Default Accept header
      
      # Logging
      logging-enabled: false         # Enable request/response logging (default: false)
```

### REST Configuration Details

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `max-connections` | Integer | 100 | Maximum number of HTTP connections in the pool |
| `max-idle-time` | Duration | 5m | How long a connection can be idle before being closed |
| `max-life-time` | Duration | 30m | Maximum lifetime of a connection |
| `pending-acquire-timeout` | Duration | 10s | Timeout when waiting for a connection from the pool |
| `response-timeout` | Duration | 30s | Maximum time to wait for a response |
| `connect-timeout` | Duration | 10s | Maximum time to establish a connection |
| `read-timeout` | Duration | 30s | Maximum time to read response data |
| `compression-enabled` | Boolean | true | Enable gzip compression for requests/responses |
| `follow-redirects` | Boolean | true | Automatically follow HTTP redirects |
| `max-in-memory-size` | Integer | 1048576 | Maximum size of in-memory buffer (1MB) |
| `max-retries` | Integer | 3 | Maximum number of retry attempts |
| `logging-enabled` | Boolean | false | Enable detailed request/response logging |

## gRPC Configuration

```yaml
firefly:
  service-client:
    grpc:
      # Keep-Alive Settings
      keep-alive-time: 5m            # Keep-alive ping interval (default: 5m)
      keep-alive-timeout: 30s        # Keep-alive ping timeout (default: 30s)
      keep-alive-without-calls: true # Send keep-alive pings even without active calls (default: true)
      
      # Message Size Limits
      max-inbound-message-size: 4194304   # Max inbound message size in bytes (default: 4MB)
      max-inbound-metadata-size: 8192     # Max inbound metadata size in bytes (default: 8KB)
      
      # Timeout Settings
      call-timeout: 30s              # Default call timeout (default: 30s)
      
      # Retry Settings
      retry-enabled: true            # Enable gRPC retries (default: true)
      
      # Security Settings
      use-plaintext-by-default: true # Use plaintext by default (default: true for dev)
      
      # Performance Settings
      compression-enabled: true      # Enable message compression (default: true)
      max-concurrent-streams: 100    # Maximum concurrent streams per connection (default: 100)
```

### gRPC Configuration Details

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `keep-alive-time` | Duration | 5m | Interval between keep-alive pings |
| `keep-alive-timeout` | Duration | 30s | Timeout for keep-alive ping response |
| `keep-alive-without-calls` | Boolean | true | Send pings even without active calls |
| `max-inbound-message-size` | Integer | 4194304 | Maximum size of inbound messages (4MB) |
| `max-inbound-metadata-size` | Integer | 8192 | Maximum size of inbound metadata (8KB) |
| `call-timeout` | Duration | 30s | Default timeout for gRPC calls |
| `retry-enabled` | Boolean | true | Enable automatic retries for failed calls |
| `use-plaintext-by-default` | Boolean | true | Use plaintext (no TLS) by default |
| `compression-enabled` | Boolean | true | Enable message compression |
| `max-concurrent-streams` | Integer | 100 | Maximum concurrent streams |

## Circuit Breaker Configuration

```yaml
firefly:
  service-client:
    circuit-breaker:
      enabled: true                                           # Enable circuit breaker (default: true)
      failure-rate-threshold: 50.0                            # Failure rate threshold percentage (default: 50%)
      minimum-number-of-calls: 5                              # Minimum calls before calculating failure rate (default: 5)
      sliding-window-size: 10                                 # Size of sliding window (default: 10)
      wait-duration-in-open-state: 60s                        # Wait time in OPEN state (default: 60s)
      permitted-number-of-calls-in-half-open-state: 3         # Calls allowed in HALF_OPEN state (default: 3)
      max-wait-duration-in-half-open-state: 30s               # Max wait in HALF_OPEN state (default: 30s)
      call-timeout: 10s                                       # Call timeout (default: 10s)
      slow-call-duration-threshold: 5s                        # Threshold for slow calls (default: 5s)
      slow-call-rate-threshold: 100.0                         # Slow call rate threshold percentage (default: 100%)
      automatic-transition-from-open-to-half-open-enabled: true  # Auto transition to HALF_OPEN (default: true)
```

### Circuit Breaker States

- **CLOSED**: Normal operation, all requests pass through
- **OPEN**: Circuit is open, requests are rejected immediately
- **HALF_OPEN**: Testing if service has recovered, limited requests allowed

### Circuit Breaker Configuration Details

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | true | Enable circuit breaker functionality |
| `failure-rate-threshold` | Double | 50.0 | Percentage of failures to open circuit (0-100) |
| `minimum-number-of-calls` | Integer | 5 | Minimum calls before evaluating failure rate |
| `sliding-window-size` | Integer | 10 | Number of calls in the sliding window |
| `wait-duration-in-open-state` | Duration | 60s | How long to wait before transitioning to HALF_OPEN |
| `permitted-number-of-calls-in-half-open-state` | Integer | 3 | Number of test calls in HALF_OPEN state |
| `call-timeout` | Duration | 10s | Timeout for individual calls |
| `slow-call-duration-threshold` | Duration | 5s | Duration threshold for slow calls |
| `slow-call-rate-threshold` | Double | 100.0 | Percentage of slow calls to open circuit |

## Retry Configuration

```yaml
firefly:
  service-client:
    retry:
      enabled: true                  # Enable retry mechanism (default: true)
      max-attempts: 3                # Maximum retry attempts (default: 3)
      initial-interval: 1s           # Initial retry interval (default: 1s)
      multiplier: 2.0                # Backoff multiplier (default: 2.0)
      max-interval: 30s              # Maximum retry interval (default: 30s)
```

### Retry Backoff Example

With default settings:
- Attempt 1: Immediate
- Attempt 2: Wait 1s
- Attempt 3: Wait 2s (1s × 2.0)
- Attempt 4: Wait 4s (2s × 2.0)

## Metrics Configuration

```yaml
firefly:
  service-client:
    metrics:
      enabled: true                  # Enable metrics collection (default: true)
      collect-detailed-metrics: false # Collect detailed per-endpoint metrics (default: false)
      histogram-buckets: [0.001, 0.01, 0.1, 1, 10]  # Histogram buckets in seconds
```

### Available Metrics

When metrics are enabled, the following metrics are collected:

- `service.client.requests.total` - Total number of requests
- `service.client.requests.duration` - Request duration histogram
- `service.client.requests.errors` - Total number of errors
- `service.client.circuit.breaker.state` - Circuit breaker state gauge
- `service.client.circuit.breaker.calls` - Circuit breaker call counters

## Security Configuration

```yaml
firefly:
  service-client:
    security:
      tls-enabled: false             # Enable TLS/SSL (default: false)
      trust-store-path: ""           # Path to trust store
      trust-store-password: ""       # Trust store password
      key-store-path: ""             # Path to key store (for mTLS)
      key-store-password: ""         # Key store password
```

## Environment-Specific Configuration

### Development Environment

```yaml
firefly:
  service-client:
    environment: DEVELOPMENT
    rest:
      logging-enabled: true
      max-connections: 50
    circuit-breaker:
      failure-rate-threshold: 75.0   # More lenient
      minimum-number-of-calls: 10
```

### Production Environment

```yaml
firefly:
  service-client:
    environment: PRODUCTION
    rest:
      logging-enabled: false
      max-connections: 200
    circuit-breaker:
      failure-rate-threshold: 50.0   # Stricter
      minimum-number-of-calls: 5
    security:
      tls-enabled: true
```

## Programmatic Configuration

### Custom Circuit Breaker Configuration

```java
@Configuration
public class CustomCircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerConfig customCircuitBreakerConfig() {
        return CircuitBreakerConfig.builder()
            .failureRateThreshold(60.0)
            .minimumNumberOfCalls(10)
            .slidingWindowSize(20)
            .waitDurationInOpenState(Duration.ofMinutes(2))
            .permittedNumberOfCallsInHalfOpenState(5)
            .callTimeout(Duration.ofSeconds(15))
            .build();
    }
}
```

### Custom REST Client Configuration

```java
@Configuration
public class CustomRestClientConfig {
    
    @Bean
    public ServiceClient customServiceClient() {
        return ServiceClient.rest("custom-service")
            .baseUrl("https://api.example.com")
            .timeout(Duration.ofSeconds(45))
            .maxConnections(150)
            .defaultHeader("Authorization", "Bearer ${token}")
            .defaultHeader("X-API-Version", "v2")
            .jsonContentType()
            .build();
    }
}
```

## Configuration Validation

The library validates configuration on startup. Invalid configurations will cause the application to fail fast with descriptive error messages.

### Common Validation Rules

- Timeouts must be positive durations
- Connection pool sizes must be > 0
- Failure rate thresholds must be between 0 and 100
- Retry attempts must be >= 0

## Complete Configuration Examples

### Example 1: Simple Microservice (First Time Setup)

Perfect for getting started with a single REST service:

```yaml
# application.yml
firefly:
  service-client:
    enabled: true
    environment: DEVELOPMENT

    rest:
      logging-enabled: true          # See what's happening
```

```java
// UserServiceClient.java
@Service
public class UserServiceClient {

    private final ServiceClient client;

    public UserServiceClient() {
        this.client = ServiceClient.rest("user-service")
            .baseUrl("http://localhost:8080")
            .jsonContentType()
            .build();
    }

    public Mono<User> getUser(String id) {
        return client.get("/users/{id}", User.class)
            .withPathParam("id", id)
            .execute();
    }
}
```

### Example 2: Multiple Services with Authentication

Common pattern for microservices calling multiple backends:

```yaml
# application.yml
firefly:
  service-client:
    enabled: true
    default-timeout: 30s
    environment: DEVELOPMENT

    # Global headers for all clients
    default-headers:
      X-Client-Id: "my-application"
      X-Client-Version: "1.0.0"

    rest:
      max-connections: 100
      compression-enabled: true
      logging-enabled: true
```

```java
// ServiceClientsConfig.java
@Configuration
public class ServiceClientsConfig {

    @Value("${auth.token}")
    private String authToken;

    @Bean
    public ServiceClient userServiceClient() {
        return ServiceClient.rest("user-service")
            .baseUrl("http://user-service:8080")
            .defaultHeader("Authorization", "Bearer " + authToken)
            .jsonContentType()
            .build();
    }

    @Bean
    public ServiceClient orderServiceClient() {
        return ServiceClient.rest("order-service")
            .baseUrl("http://order-service:8080")
            .defaultHeader("Authorization", "Bearer " + authToken)
            .timeout(Duration.ofSeconds(45))  // Orders might take longer
            .jsonContentType()
            .build();
    }

    @Bean
    public ServiceClient inventoryServiceClient() {
        return ServiceClient.rest("inventory-service")
            .baseUrl("http://inventory-service:8080")
            .defaultHeader("Authorization", "Bearer " + authToken)
            .timeout(Duration.ofSeconds(10))  // Inventory should be fast
            .maxConnections(200)              // High traffic service
            .jsonContentType()
            .build();
    }
}
```

### Example 3: Production-Ready Configuration

Complete production setup with all resilience features:

```yaml
# application-prod.yml
firefly:
  service-client:
    enabled: true
    default-timeout: 30s
    environment: PRODUCTION

    default-headers:
      X-Client-Id: "production-app"
      User-Agent: "MyApp/2.0"

    rest:
      max-connections: 300
      max-idle-time: 5m
      max-life-time: 30m
      response-timeout: 30s
      connect-timeout: 10s
      compression-enabled: true
      logging-enabled: false         # Disable in production
      follow-redirects: true
      max-in-memory-size: 2097152    # 2MB

    grpc:
      keep-alive-time: 5m
      keep-alive-timeout: 30s
      max-inbound-message-size: 8388608  # 8MB
      use-plaintext-by-default: false    # TLS enabled
      compression-enabled: true
      max-concurrent-streams: 200

    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50.0
      minimum-number-of-calls: 10
      sliding-window-size: 20
      wait-duration-in-open-state: 60s
      permitted-number-of-calls-in-half-open-state: 5
      call-timeout: 10s
      slow-call-duration-threshold: 5s
      automatic-transition-from-open-to-half-open-enabled: true

    retry:
      enabled: true
      max-attempts: 3
      initial-interval: 1s
      multiplier: 2.0
      max-interval: 30s

    metrics:
      enabled: true
      collect-detailed-metrics: true

    security:
      tls-enabled: true
```

### Example 4: Mixed REST and gRPC Services

Using both REST and gRPC in the same application:

```yaml
# application.yml
firefly:
  service-client:
    enabled: true
    environment: DEVELOPMENT

    rest:
      max-connections: 100
      logging-enabled: true

    grpc:
      use-plaintext-by-default: true  # Dev only
      max-inbound-message-size: 4194304
```

```java
@Configuration
public class MixedServiceConfig {

    // REST client for user service
    @Bean
    public ServiceClient userRestClient() {
        return ServiceClient.rest("user-service")
            .baseUrl("http://localhost:8080")
            .jsonContentType()
            .build();
    }

    // gRPC client for payment service
    @Bean
    public ServiceClient paymentGrpcClient() {
        return ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("localhost:9090")
            .usePlaintext()
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
            .build();
    }
}
```

### Example 5: Environment-Specific Profiles

Different configurations for different environments:

```yaml
# application.yml (common settings)
firefly:
  service-client:
    enabled: true

    circuit-breaker:
      enabled: true

---
# application-dev.yml
firefly:
  service-client:
    environment: DEVELOPMENT
    rest:
      logging-enabled: true
      max-connections: 50
    grpc:
      use-plaintext-by-default: true

---
# application-test.yml
firefly:
  service-client:
    environment: TESTING
    rest:
      logging-enabled: false
      max-connections: 20
    circuit-breaker:
      minimum-number-of-calls: 2  # Faster failure detection in tests

---
# application-prod.yml
firefly:
  service-client:
    environment: PRODUCTION
    rest:
      logging-enabled: false
      max-connections: 300
    grpc:
      use-plaintext-by-default: false
    security:
      tls-enabled: true
```

## Best Practices

1. **Use environment-specific profiles** - Separate configurations for dev, test, and prod
2. **Enable metrics in production** - Essential for monitoring and troubleshooting
3. **Tune circuit breaker settings** - Based on your service's characteristics
4. **Use TLS in production** - Always enable security for production environments
5. **Monitor connection pools** - Adjust `max-connections` based on load
6. **Set appropriate timeouts** - Balance between user experience and resource usage
7. **Start with defaults** - Only override properties when you have a specific need
8. **Enable logging in development** - Helps debug integration issues
9. **Test circuit breaker behavior** - Ensure it works as expected for your use case
10. **Document custom configurations** - Explain why you deviated from defaults

## Troubleshooting

### Circuit Breaker Opening Too Frequently

Increase `failure-rate-threshold` or `minimum-number-of-calls`:

```yaml
circuit-breaker:
  failure-rate-threshold: 75.0
  minimum-number-of-calls: 10
```

### Connection Pool Exhaustion

Increase `max-connections` or reduce `max-idle-time`:

```yaml
rest:
  max-connections: 200
  max-idle-time: 2m
```

### Slow Requests

Adjust timeout settings:

```yaml
rest:
  response-timeout: 60s
  read-timeout: 60s
```

---

**Copyright © 2025 Firefly Software Solutions Inc**


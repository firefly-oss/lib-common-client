# Error Handling Guide

## Overview

The Firefly Common Client library provides **best-in-class error handling** with rich error context, automatic error mapping, and smart retry capabilities. Every exception includes comprehensive metadata to help with debugging, monitoring, and error recovery.

## Table of Contents

- [Exception Hierarchy](#exception-hierarchy)
- [Error Context](#error-context)
- [Retryable Errors](#retryable-errors)
- [Exception Types](#exception-types)
- [Error Handling Patterns](#error-handling-patterns)
- [Automatic Error Mapping](#automatic-error-mapping)
- [Best Practices](#best-practices)

## Exception Hierarchy

All service client exceptions extend `ServiceClientException`, which provides:

- **Rich Error Context**: Detailed metadata about the error
- **Error Categories**: High-level classification for monitoring
- **Backward Compatibility**: Works with existing error handling code

```
ServiceClientException (base)
├── ServiceNotFoundException (404, NOT_FOUND)
├── ServiceAuthenticationException (401/403, UNAUTHENTICATED/PERMISSION_DENIED)
├── ServiceValidationException (400, INVALID_ARGUMENT)
│   └── ServiceUnprocessableEntityException (422)
├── ServiceUnavailableException (5xx, UNAVAILABLE)
│   └── ServiceTemporarilyUnavailableException (502/503/504) [Retryable]
├── ServiceTimeoutException (408, DEADLINE_EXCEEDED) [Retryable]
│   └── CircuitBreakerTimeoutException [Retryable]
├── ServiceRateLimitException (429, RESOURCE_EXHAUSTED) [Retryable]
├── ServiceConflictException (409, ABORTED)
├── ServiceInternalErrorException (500, INTERNAL) [Retryable]
├── ServiceConnectionException (network errors) [Retryable]
├── ServiceSerializationException (parsing errors)
├── CircuitBreakerOpenException [Retryable]
├── LoadSheddingException [Retryable]
├── RateLimitExceededException [Retryable]
├── BulkheadFullException [Retryable]
├── WsdlParsingException (WSDL errors)
└── SoapFaultException (SOAP faults)
```

## Error Context

Every exception includes an `ErrorContext` object with comprehensive metadata:

```java
try {
    User user = client.get("/users/123", User.class).execute().block();
} catch (ServiceClientException e) {
    ErrorContext ctx = e.getErrorContext();
    
    // Service Information
    String serviceName = ctx.getServiceName();
    String endpoint = ctx.getEndpoint();
    String method = ctx.getMethod();
    ClientType clientType = ctx.getClientType(); // REST, GRPC, or SOAP
    
    // Request Tracking
    String requestId = ctx.getRequestId();
    String correlationId = ctx.getCorrelationId();
    Instant timestamp = ctx.getTimestamp();
    
    // Protocol Details
    Integer httpStatus = ctx.getHttpStatusCode();
    String grpcStatus = ctx.getGrpcStatusCode();
    String responseBody = ctx.getResponseBody();
    Map<String, String> headers = ctx.getHeaders();
    
    // Performance Metrics
    Duration elapsedTime = ctx.getElapsedTime();
    Integer retryAttempt = ctx.getRetryAttempt();
    
    // Helper Methods
    boolean hasHttpStatus = ctx.hasHttpStatusCode();
    boolean hasGrpcStatus = ctx.hasGrpcStatusCode();
    boolean isRetry = ctx.isRetry();
}
```

### Error Categories

Each exception has an associated category for monitoring and alerting:

```java
ErrorCategory category = exception.getErrorCategory();

switch (category) {
    case CLIENT_ERROR -> log.warn("Client error: {}", exception.getMessage());
    case SERVER_ERROR -> log.error("Server error: {}", exception.getMessage());
    case NETWORK_ERROR -> log.warn("Network error: {}", exception.getMessage());
    case AUTHENTICATION_ERROR -> log.error("Auth error: {}", exception.getMessage());
    case VALIDATION_ERROR -> log.info("Validation error: {}", exception.getMessage());
    case RATE_LIMIT_ERROR -> log.warn("Rate limit: {}", exception.getMessage());
    case CIRCUIT_BREAKER_ERROR -> log.warn("Circuit breaker: {}", exception.getMessage());
    case TIMEOUT_ERROR -> log.warn("Timeout: {}", exception.getMessage());
    case SERIALIZATION_ERROR -> log.error("Serialization error: {}", exception.getMessage());
    case CONFIGURATION_ERROR -> log.error("Configuration error: {}", exception.getMessage());
}
```

## Retryable Errors

Exceptions implementing `RetryableError` indicate that the operation may succeed if retried:

```java
try {
    return client.get("/users/123", User.class).execute().block();
} catch (ServiceClientException e) {
    if (e instanceof RetryableError retryable && retryable.isRetryable()) {
        Duration delay = retryable.getRetryDelay();
        log.info("Retryable error, waiting {} before retry", delay);
        Thread.sleep(delay.toMillis());
        // Retry the operation
    } else {
        log.error("Non-retryable error: {}", e.getMessage());
        throw e;
    }
}
```

### Retryable Exception Types

| Exception | Default Retry Delay | Reason |
|-----------|-------------------|---------|
| `ServiceTimeoutException` | 2 seconds | Timeouts may be transient |
| `ServiceRateLimitException` | Based on Retry-After header (default 60s) | Server indicates when to retry |
| `ServiceInternalErrorException` | 2 seconds | Server errors may be transient |
| `ServiceTemporarilyUnavailableException` | 5 seconds | Service may recover |
| `ServiceConnectionException` | 1 second | Network issues may be transient |
| `CircuitBreakerOpenException` | 5 seconds | Wait for circuit to close |
| `CircuitBreakerTimeoutException` | 2 seconds | Timeout may be transient |
| `LoadSheddingException` | 3 seconds | System load may decrease |
| `RateLimitExceededException` | 1 second | Client-side rate limit |
| `BulkheadFullException` | 500ms | Concurrent requests may complete |

### Using with Reactor Retry

```java
client.get("/users/123", User.class)
    .execute()
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
        .filter(throwable -> 
            throwable instanceof RetryableError && 
            ((RetryableError) throwable).isRetryable())
        .doBeforeRetry(signal -> 
            log.warn("Retrying after error: {}", signal.failure().getMessage())))
    .block();
```

## Exception Types

### Client Errors (4xx)

#### ServiceValidationException
Request validation failed (HTTP 400, gRPC INVALID_ARGUMENT).

```java
try {
    client.post("/users", invalidUser, User.class).execute().block();
} catch (ServiceValidationException e) {
    log.error("Validation failed: {}", e.getMessage());
    // Fix the request data
}
```

#### ServiceUnprocessableEntityException
Request validation failed with detailed errors (HTTP 422).

```java
try {
    client.post("/users", user, User.class).execute().block();
} catch (ServiceUnprocessableEntityException e) {
    List<ValidationError> errors = e.getValidationErrors();
    for (ValidationError error : errors) {
        log.error("Field '{}': {}", error.getField(), error.getMessage());
    }
}
```

#### ServiceAuthenticationException
Authentication or authorization failed (HTTP 401/403, gRPC UNAUTHENTICATED/PERMISSION_DENIED).

```java
try {
    client.get("/admin/users", UserList.class).execute().block();
} catch (ServiceAuthenticationException e) {
    log.error("Auth failed: {}", e.getMessage());
    // Refresh credentials or request access
}
```

#### ServiceNotFoundException
Resource not found (HTTP 404, gRPC NOT_FOUND).

```java
try {
    client.get("/users/999", User.class).execute().block();
} catch (ServiceNotFoundException e) {
    log.warn("User not found: {}", e.getMessage());
    return Optional.empty();
}
```

#### ServiceConflictException
Request conflicts with current state (HTTP 409, gRPC ABORTED).

```java
try {
    client.post("/users", user, User.class).execute().block();
} catch (ServiceConflictException e) {
    log.warn("User already exists: {}", e.getMessage());
    // Handle conflict (e.g., update instead of create)
}
```

### Server Errors (5xx)

#### ServiceInternalErrorException
Server internal error (HTTP 500, gRPC INTERNAL). **Retryable**.

```java
try {
    client.get("/users/123", User.class).execute().block();
} catch (ServiceInternalErrorException e) {
    log.error("Server error: {}", e.getMessage());
    // Retry or fallback
}
```

#### ServiceUnavailableException
Service unavailable (HTTP 5xx, gRPC UNAVAILABLE).

```java
try {
    client.get("/users/123", User.class).execute().block();
} catch (ServiceUnavailableException e) {
    log.error("Service unavailable: {}", e.getMessage());
    // Use fallback or cached data
}
```

#### ServiceTemporarilyUnavailableException
Service temporarily unavailable (HTTP 502/503/504). **Retryable**.

```java
try {
    client.get("/users/123", User.class).execute().block();
} catch (ServiceTemporarilyUnavailableException e) {
    log.warn("Service temporarily unavailable, will retry");
    // Automatic retry recommended
}
```

### Timeout Errors

#### ServiceTimeoutException
Request timeout (HTTP 408, gRPC DEADLINE_EXCEEDED). **Retryable**.

```java
try {
    client.get("/slow-endpoint", Data.class).execute().block();
} catch (ServiceTimeoutException e) {
    log.warn("Request timed out after {}", e.getErrorContext().getElapsedTime());
    // Retry with longer timeout or use cached data
}
```

### Rate Limiting

#### ServiceRateLimitException
Server rate limit exceeded (HTTP 429, gRPC RESOURCE_EXHAUSTED). **Retryable**.

```java
try {
    client.get("/users/123", User.class).execute().block();
} catch (ServiceRateLimitException e) {
    Integer retryAfter = e.getRetryAfterSeconds();
    log.warn("Rate limited, retry after {} seconds", retryAfter);
    Thread.sleep(Duration.ofSeconds(retryAfter != null ? retryAfter : 60));
    // Retry
}
```

### Network Errors

#### ServiceConnectionException
Network connection error. **Retryable**.

```java
try {
    client.get("/users/123", User.class).execute().block();
} catch (ServiceConnectionException e) {
    log.error("Connection failed: {}", e.getMessage());
    // Check network connectivity, retry
}
```

### Serialization Errors

#### ServiceSerializationException
JSON/XML parsing error.

```java
try {
    client.get("/users/123", User.class).execute().block();
} catch (ServiceSerializationException e) {
    String rawContent = e.getRawContent();
    log.error("Failed to parse response: {}", rawContent);
    // Check response format, update model
}
```

## Error Handling Patterns

### Pattern 1: Graceful Degradation

```java
public User getUser(String userId) {
    try {
        return client.get("/users/" + userId, User.class).execute().block();
    } catch (ServiceNotFoundException e) {
        log.warn("User {} not found", userId);
        return null;
    } catch (ServiceClientException e) {
        log.error("Error fetching user {}: {}", userId, e.getMessage());
        return getCachedUser(userId); // Fallback to cache
    }
}
```

### Pattern 2: Smart Retry

```java
public User getUserWithRetry(String userId) {
    return client.get("/users/" + userId, User.class)
        .execute()
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof RetryableError)
            .doBeforeRetry(signal -> {
                if (signal.failure() instanceof RetryableError retryable) {
                    Duration delay = retryable.getRetryDelay();
                    log.info("Retrying after {} due to: {}", 
                        delay, signal.failure().getMessage());
                }
            }))
        .block();
}
```

### Pattern 3: Error Categorization

```java
public void handleError(ServiceClientException e) {
    ErrorCategory category = e.getErrorCategory();
    ErrorContext ctx = e.getErrorContext();
    
    switch (category) {
        case CLIENT_ERROR, VALIDATION_ERROR -> {
            // Log and alert - client needs to fix request
            log.error("Client error for {}: {}", ctx.getEndpoint(), e.getMessage());
            metrics.incrementCounter("client_errors", "service", ctx.getServiceName());
        }
        case SERVER_ERROR -> {
            // Log, alert, and potentially retry
            log.error("Server error for {}: {}", ctx.getEndpoint(), e.getMessage());
            metrics.incrementCounter("server_errors", "service", ctx.getServiceName());
            if (e instanceof RetryableError) {
                // Retry logic
            }
        }
        case NETWORK_ERROR, TIMEOUT_ERROR -> {
            // Log and retry
            log.warn("Transient error for {}: {}", ctx.getEndpoint(), e.getMessage());
            metrics.incrementCounter("transient_errors", "service", ctx.getServiceName());
        }
    }
}
```

## Automatic Error Mapping

The library automatically maps HTTP and gRPC status codes to typed exceptions:

### HTTP Status Code Mapping

| HTTP Status | Exception Type |
|-------------|----------------|
| 400 | `ServiceValidationException` |
| 401, 403 | `ServiceAuthenticationException` |
| 404 | `ServiceNotFoundException` |
| 408 | `ServiceTimeoutException` |
| 409 | `ServiceConflictException` |
| 422 | `ServiceUnprocessableEntityException` |
| 429 | `ServiceRateLimitException` |
| 500 | `ServiceInternalErrorException` |
| 502, 503, 504 | `ServiceTemporarilyUnavailableException` |

### gRPC Status Code Mapping

| gRPC Status | Exception Type |
|-------------|----------------|
| INVALID_ARGUMENT, FAILED_PRECONDITION, OUT_OF_RANGE | `ServiceValidationException` |
| UNAUTHENTICATED, PERMISSION_DENIED | `ServiceAuthenticationException` |
| NOT_FOUND | `ServiceNotFoundException` |
| DEADLINE_EXCEEDED | `ServiceTimeoutException` |
| ABORTED, ALREADY_EXISTS | `ServiceConflictException` |
| RESOURCE_EXHAUSTED | `ServiceRateLimitException` |
| INTERNAL, DATA_LOSS, UNKNOWN | `ServiceInternalErrorException` |
| UNAVAILABLE | `ServiceTemporarilyUnavailableException` |

## Best Practices

### 1. Always Check Error Context

```java
catch (ServiceClientException e) {
    ErrorContext ctx = e.getErrorContext();
    log.error("Error calling {}: {} (Request ID: {}, Duration: {}ms)",
        ctx.getEndpoint(),
        e.getMessage(),
        ctx.getRequestId(),
        ctx.getElapsedTime().toMillis());
}
```

### 2. Use Retryable Interface for Smart Retries

```java
catch (ServiceClientException e) {
    if (e instanceof RetryableError retryable && retryable.isRetryable()) {
        // Implement retry logic
    } else {
        // Handle non-retryable error
    }
}
```

### 3. Handle Specific Exceptions

```java
try {
    return client.get("/users/" + userId, User.class).execute().block();
} catch (ServiceNotFoundException e) {
    return Optional.empty();
} catch (ServiceAuthenticationException e) {
    throw new UnauthorizedException("Access denied");
} catch (ServiceTimeoutException e) {
    return getCachedUser(userId);
}
```

### 4. Monitor Error Categories

```java
catch (ServiceClientException e) {
    metrics.incrementCounter("service_errors",
        "category", e.getErrorCategory().name(),
        "service", e.getErrorContext().getServiceName());
}
```

### 5. Include Request IDs in Logs

```java
catch (ServiceClientException e) {
    MDC.put("requestId", e.getErrorContext().getRequestId());
    log.error("Service call failed", e);
    MDC.remove("requestId");
}
```

## See Also

- [Architecture Guide](ARCHITECTURE.md) - System architecture and design
- [Quick Start Guide](QUICKSTART.md) - Getting started examples
- [SOAP Client Guide](SOAP_CLIENT_GUIDE.md) - SOAP-specific error handling
- [Testing Guide](TESTING_GUIDE.md) - Testing error scenarios


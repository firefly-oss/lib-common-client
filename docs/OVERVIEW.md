# Firefly Common Client Library - Overview

## Introduction

The `lib-common-client` library is a comprehensive, reactive service communication framework designed for microservice architectures. It provides a unified API for REST and gRPC communication while maintaining protocol-specific optimizations and built-in resilience patterns.

## Getting Started with Client Setup

### Quick Setup Overview

Setting up a service client involves three main steps:

1. **Choose Client Type**: Decide between REST or gRPC based on your service protocol
2. **Build the Client**: Use the fluent builder API to configure the client
3. **Configure Properties**: Optionally customize behavior via `application.yml`

### REST Client Setup

```java
ServiceClient restClient = ServiceClient.rest("my-service")
    .baseUrl("http://localhost:8080")      // Required: Service base URL
    .timeout(Duration.ofSeconds(30))       // Optional: Request timeout
    .jsonContentType()                     // Optional: Set JSON content type
    .build();
```

**Key Configuration Points:**
- `baseUrl()` is required and specifies where your service is hosted
- `timeout()` controls how long to wait for responses (default: 30s)
- `jsonContentType()` sets appropriate headers for JSON communication
- Additional headers can be added with `defaultHeader(name, value)`

### gRPC Client Setup

```java
ServiceClient grpcClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
    .address("localhost:9090")                                      // Required: gRPC address
    .usePlaintext()                                                 // Optional: For development
    .timeout(Duration.ofSeconds(30))                                // Optional: Call timeout
    .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))   // Required: Stub factory
    .build();
```

**Key Configuration Points:**
- `address()` specifies the gRPC service host and port
- `stubFactory()` is required and creates the gRPC stub from a channel
- `usePlaintext()` disables TLS (use only in development)
- `useTransportSecurity()` enables TLS for production environments

### Configuration via Properties

Instead of programmatic configuration, you can use `application.yml`:

```yaml
firefly:
  service-client:
    enabled: true
    default-timeout: 30s
    environment: DEVELOPMENT

    rest:
      max-connections: 100
      response-timeout: 30s
      compression-enabled: true

    grpc:
      keep-alive-time: 5m
      max-inbound-message-size: 4194304
```

These properties apply globally to all clients created in your application.

## Core Components

### ServiceClient Interface

The `ServiceClient` interface (`com.firefly.common.client.ServiceClient`) is the main entry point for all service communication. It provides:

- **Unified API**: Single interface for REST and gRPC protocols
- **Reactive Operations**: Built on Project Reactor with `Mono<T>` and `Flux<T>` return types
- **Type Safety**: Generic support with `Class<T>` and `TypeReference<T>` for complex types
- **Fluent API**: Method chaining for request building

### Client Types

The library supports the following client types defined in `ClientType` enum:

```java
public enum ClientType {
    REST("REST"),     // HTTP/REST clients using WebClient
    GRPC("gRPC");     // gRPC clients using Protocol Buffers
}
```

### Builder Pattern

#### RestClientBuilder
- **Class**: `com.firefly.common.client.builder.RestClientBuilder`
- **Purpose**: Creates REST/HTTP service clients
- **Key Methods**:
  - `baseUrl(String)`: Sets the service base URL
  - `timeout(Duration)`: Configures request timeout
  - `maxConnections(int)`: Sets connection pool size
  - `defaultHeader(String, String)`: Adds default headers
  - `jsonContentType()`: Convenience method for JSON headers
  - `xmlContentType()`: Convenience method for XML headers

#### GrpcClientBuilder
- **Class**: `com.firefly.common.client.builder.GrpcClientBuilder<T>`
- **Purpose**: Creates gRPC service clients
- **Key Methods**:
  - `address(String)`: Sets the gRPC service address
  - `timeout(Duration)`: Configures call timeout
  - `usePlaintext()`: Enables plaintext connections
  - `useTransportSecurity()`: Enables TLS
  - `stubFactory(Function<Object, T>)`: Sets stub factory

## Implementation Architecture

### REST Implementation
- **Class**: `com.firefly.common.client.impl.RestServiceClientImpl`
- **Uses**: Spring WebFlux `WebClient`
- **Features**: Connection pooling, compression, redirects, custom headers

### gRPC Implementation
- **Class**: `com.firefly.common.client.impl.GrpcServiceClientImpl<T>`
- **Uses**: gRPC ManagedChannel and generated stubs
- **Features**: Keep-alive, compression, metadata propagation, streaming support

## Resilience Patterns

### Circuit Breaker

The library includes a comprehensive circuit breaker implementation:

- **Manager**: `com.firefly.common.resilience.CircuitBreakerManager`
- **Config**: `com.firefly.common.resilience.CircuitBreakerConfig`
- **States**: `CLOSED`, `OPEN`, `HALF_OPEN` (defined in `CircuitBreakerState`)
- **Metrics**: `com.firefly.common.resilience.CircuitBreakerMetrics`

#### Circuit Breaker Features:
- **Sliding Window**: Configurable time-based or count-based windows
- **Failure Rate Calculation**: Percentage-based failure detection
- **Automatic Recovery**: Transition to half-open state for testing
- **Slow Call Detection**: Configurable slow call thresholds
- **Per-Service Configuration**: Individual settings per service

### Exception Handling

Custom exceptions for different failure scenarios:

- `ServiceClientException`: Base exception class
- `ServiceNotFoundException`: Service not found (404-like errors)
- `ServiceUnavailableException`: Service temporarily unavailable (503-like errors)
- `ServiceAuthenticationException`: Authentication failures (401-like errors)
- `ServiceValidationException`: Request validation errors (400-like errors)
- `CircuitBreakerOpenException`: Circuit breaker is open
- `CircuitBreakerTimeoutException`: Operation timeout

## Health Management

### ServiceClientHealthManager
- **Class**: `com.firefly.common.client.health.ServiceClientHealthManager`
- **Purpose**: Monitors service client health
- **Features**:
  - Health check endpoints
  - Service availability monitoring
  - Integration with Spring Boot Actuator

## Interceptor Framework

### ServiceClientInterceptor
- **Interface**: `com.firefly.common.client.interceptor.ServiceClientInterceptor`
- **Purpose**: Request/response processing pipeline
- **Built-in Interceptors**:
  - `LoggingInterceptor`: Request/response logging
  - `MetricsInterceptor`: Performance metrics collection

## Configuration System

### ServiceClientProperties
- **Class**: `com.firefly.common.config.ServiceClientProperties`
- **Prefix**: `firefly.service-client`
- **Nested Classes**:
  - `Rest`: REST-specific configuration
  - `Grpc`: gRPC-specific configuration
  - `CircuitBreaker`: Circuit breaker configuration
  - `Retry`: Retry configuration
  - `Metrics`: Metrics configuration
  - `Security`: Security configuration

### Environment-Specific Defaults

The configuration system supports three environments:
- `DEVELOPMENT`: Enhanced logging, relaxed timeouts
- `TESTING`: Reduced connections, faster failures
- `PRODUCTION`: Optimized for performance and reliability

## Auto-Configuration

### ServiceClientAutoConfiguration
- **Class**: `com.firefly.common.config.ServiceClientAutoConfiguration`
- **Purpose**: Spring Boot auto-configuration
- **Beans Created**:
  - `WebClient.Builder`: Pre-configured WebClient builder
  - `CircuitBreakerConfig`: Default circuit breaker configuration
  - `CircuitBreakerManager`: Circuit breaker manager instance
  - `RestClientBuilder`: Default REST client builder
  - `GrpcClientBuilderFactory`: Factory for gRPC client builders

## Request Builder Pattern

### RequestBuilder Interface
- **Interface**: `ServiceClient.RequestBuilder<R>`
- **Purpose**: Fluent API for building requests
- **Key Methods**:
  - `withBody(Object)`: Sets request body
  - `withPathParam(String, Object)`: Adds path parameter
  - `withQueryParam(String, Object)`: Adds query parameter
  - `withHeader(String, String)`: Adds request header
  - `withTimeout(Duration)`: Sets request timeout
  - `execute()`: Executes request and returns `Mono<R>`
  - `stream()`: Executes as streaming request and returns `Flux<R>`

## Reactive Patterns

The library is built on reactive principles:

- **Non-blocking I/O**: All operations are asynchronous
- **Backpressure Handling**: Built-in backpressure support
- **Stream Processing**: Support for streaming responses
- **Error Handling**: Comprehensive error propagation

## Integration Points

### Spring Boot Integration
- Auto-configuration via `@EnableAutoConfiguration`
- Properties binding via `@ConfigurationProperties`
- Actuator health indicators
- Metrics integration with Micrometer

### Observability
- **Tracing**: Distributed tracing support
- **Metrics**: Custom metrics for performance monitoring
- **Logging**: Structured logging with correlation IDs
- **Health Checks**: Built-in health monitoring

## Extension Points

### Custom Interceptors
Implement `ServiceClientInterceptor` for custom request/response processing.

### Custom Circuit Breaker Configurations
Create custom `CircuitBreakerConfig` instances for specific requirements.

### Custom WebClient Configurations
Provide custom `WebClient` instances for specialized HTTP configurations.

### Custom gRPC Channel Configurations
Configure `ManagedChannel` instances with custom settings.

## Performance Considerations

### Connection Management
- **Pooling**: Efficient connection reuse
- **Keep-Alive**: Persistent connections for gRPC
- **Compression**: Automatic request/response compression
- **Streaming**: Efficient handling of large responses

### Memory Management
- **Bounded Buffers**: Configurable memory limits
- **Backpressure**: Automatic flow control
- **Resource Cleanup**: Proper resource disposal

### Scaling Considerations
- **Thread Model**: Non-blocking, event-driven architecture
- **Resource Isolation**: Per-service configuration
- **Circuit Breaker**: Prevent cascade failures

## Security Features

### Authentication
- Header-based authentication support
- Bearer token support
- Custom authentication schemes

### Transport Security
- TLS support for REST clients
- gRPC transport security
- Certificate validation

### Input Validation
- Request parameter validation
- Response validation hooks
- Security header enforcement

This overview provides a comprehensive understanding of the lib-common-client library's architecture and capabilities. For detailed usage examples, see the [Quick Start Guide](QUICKSTART.md) and [Architecture Documentation](ARCHITECTURE.md).
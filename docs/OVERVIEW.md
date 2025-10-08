# Firefly Common Client Library - Overview

## Introduction

The `lib-common-client` library is a comprehensive, reactive service communication framework designed for microservice architectures. It provides **protocol-specific interfaces** for REST, gRPC, and SOAP communication while maintaining built-in resilience patterns and a unified core.

## Architecture Philosophy

The library uses a **protocol-specific facade pattern** where each protocol has its own specialized interface:

- **`RestClient`**: HTTP verbs (GET, POST, PUT, DELETE, PATCH) for REST services
- **`GrpcClient<T>`**: Native gRPC operations (unary, streaming) with type-safe stubs
- **`SoapClient`**: SOAP operation invocation with WSDL introspection

All interfaces extend a common `ServiceClient` core that provides factory methods and lifecycle operations.

## Getting Started with Client Setup

### Quick Setup Overview

Setting up a service client involves three main steps:

1. **Choose Client Type**: Decide between REST, gRPC, or SOAP based on your service protocol
2. **Build the Client**: Use the fluent builder API to configure the client
3. **Configure Properties**: Optionally customize behavior via `application.yml`

### REST Client Setup

```java
RestClient restClient = ServiceClient.rest("my-service")
    .baseUrl("http://localhost:8080")      // Required: Service base URL
    .timeout(Duration.ofSeconds(30))       // Optional: Request timeout
    .jsonContentType()                     // Optional: Set JSON content type
    .build();

// Use HTTP verbs naturally
Mono<User> user = restClient.get("/users/{id}", User.class)
    .withPathParam("id", "123")
    .execute();
```

**Key Configuration Points:**
- `baseUrl()` is required and specifies where your service is hosted
- `timeout()` controls how long to wait for responses (default: 30s)
- `jsonContentType()` sets appropriate headers for JSON communication
- Additional headers can be added with `defaultHeader(name, value)`
- Returns `RestClient` with HTTP verb methods

### gRPC Client Setup

```java
GrpcClient<PaymentServiceStub> grpcClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
    .address("localhost:9090")                                      // Required: gRPC address
    .usePlaintext()                                                 // Optional: For development
    .timeout(Duration.ofSeconds(30))                                // Optional: Call timeout
    .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))   // Required: Stub factory
    .build();

// Use native gRPC operations
Mono<PaymentResponse> response = grpcClient.unary(stub ->
    stub.processPayment(paymentRequest)
);
```

**Key Configuration Points:**
- `address()` specifies the gRPC service host and port
- `stubFactory()` is required and creates the gRPC stub from a channel
- `usePlaintext()` disables TLS (use only in development)
- `useTransportSecurity()` enables TLS for production environments
- Returns `GrpcClient<T>` with native gRPC operations

### SOAP Client Setup

```java
SoapClient soapClient = ServiceClient.soap("calculator-service")
    .wsdlUrl("http://localhost:8080/calculator?WSDL")  // Required: WSDL URL
    .timeout(Duration.ofSeconds(30))                    // Optional: Request timeout
    .credentials("username", "password")                // Optional: WS-Security
    .build();

// Invoke SOAP operations
Mono<Integer> result = soapClient.invokeAsync("Add", request, Integer.class);

// Or use fluent builder
Mono<Integer> result = soapClient.invoke("Add")
    .withParameter("a", 5)
    .withParameter("b", 3)
    .execute(Integer.class);
```

**Key Configuration Points:**
- `wsdlUrl()` is required and specifies the WSDL location
- `credentials()` enables WS-Security authentication
- `enableMtom()` enables binary attachment support
- Returns `SoapClient` with SOAP operation methods

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

    soap:
      default-timeout: 30s
      mtom-enabled: false
      schema-validation-enabled: true
```

These properties apply globally to all clients created in your application.

## Core Components

### Protocol-Specific Interfaces

The library provides three protocol-specific interfaces, all extending a common `ServiceClient` core:

#### ServiceClient (Core Interface)

The `ServiceClient` interface (`com.firefly.common.client.ServiceClient`) provides:

- **Factory Methods**: `rest()`, `grpc()`, `soap()` for creating protocol-specific clients
- **Lifecycle Operations**: `getServiceName()`, `isReady()`, `healthCheck()`, `shutdown()`
- **Type Information**: `getClientType()` returns the protocol type

#### RestClient

The `RestClient` interface extends `ServiceClient` and provides:

- **HTTP Verb Methods**: `get()`, `post()`, `put()`, `delete()`, `patch()`
- **Streaming Support**: `stream()` for Server-Sent Events
- **Fluent Request Builder**: `RequestBuilder<R>` for building complex requests
- **REST Metadata**: `getBaseUrl()` returns the base URL

#### GrpcClient<T>

The `GrpcClient<T>` interface extends `ServiceClient` and provides:

- **Direct Stub Access**: `getStub()` returns the typed gRPC stub
- **Unary Operations**: `unary()`, `execute()` for single request/response
- **Server Streaming**: `serverStream()`, `executeStream()` for server-side streaming
- **Client Streaming**: `clientStream()` for client-side streaming
- **Bidirectional Streaming**: `bidiStream()` for full-duplex streaming
- **gRPC Metadata**: `getAddress()`, `getChannel()` for low-level access

#### SoapClient

The `SoapClient` interface extends `ServiceClient` and provides:

- **Operation Invocation**: `invoke()`, `invokeAsync()` for calling SOAP operations
- **WSDL Introspection**: `getOperations()` lists available operations
- **Port Access**: `getPort()` for typed port access
- **SOAP Metadata**: `getWsdlUrl()`, `getServiceQName()`, `getPortQName()`

### Client Types

The library supports the following client types defined in `ClientType` enum:

```java
public enum ClientType {
    REST("REST"),     // HTTP/REST clients using WebClient
    GRPC("gRPC"),     // gRPC clients using Protocol Buffers
    SOAP("SOAP");     // SOAP clients using JAX-WS
}
```

### Builder Pattern

#### RestClientBuilder
- **Class**: `com.firefly.common.client.builder.RestClientBuilder`
- **Purpose**: Creates `RestClient` instances for REST/HTTP services
- **Returns**: `RestClient`
- **Key Methods**:
  - `baseUrl(String)`: Sets the service base URL
  - `timeout(Duration)`: Configures request timeout
  - `maxConnections(int)`: Sets connection pool size
  - `defaultHeader(String, String)`: Adds default headers
  - `jsonContentType()`: Convenience method for JSON headers
  - `xmlContentType()`: Convenience method for XML headers

#### GrpcClientBuilder<T>
- **Class**: `com.firefly.common.client.builder.GrpcClientBuilder<T>`
- **Purpose**: Creates `GrpcClient<T>` instances for gRPC services
- **Returns**: `GrpcClient<T>`
- **Key Methods**:
  - `address(String)`: Sets the gRPC service address
  - `timeout(Duration)`: Configures call timeout
  - `usePlaintext()`: Enables plaintext connections
  - `useTransportSecurity()`: Enables TLS
  - `stubFactory(Function<ManagedChannel, T>)`: Sets stub factory

#### SoapClientBuilder
- **Class**: `com.firefly.common.client.builder.SoapClientBuilder`
- **Purpose**: Creates `SoapClient` instances for SOAP/WSDL services
- **Returns**: `SoapClient`
- **Key Methods**:
  - `wsdlUrl(String)`: Sets the WSDL URL
  - `timeout(Duration)`: Configures request timeout
  - `credentials(String, String)`: Sets WS-Security credentials
  - `enableMtom()`: Enables MTOM for binary attachments
  - `trustStore(String, String)`: Configures SSL trust store
  - `disableSslVerification()`: Disables SSL verification (development only)

## Implementation Architecture

### REST Implementation
- **Class**: `com.firefly.common.client.impl.RestServiceClientImpl`
- **Implements**: `RestClient`
- **Uses**: Spring WebFlux `WebClient`
- **Features**: Connection pooling, compression, redirects, custom headers, HTTP verb methods

### gRPC Implementation
- **Class**: `com.firefly.common.client.impl.GrpcServiceClientImpl<T>`
- **Implements**: `GrpcClient<T>`
- **Uses**: gRPC ManagedChannel and generated stubs
- **Features**: Keep-alive, compression, metadata propagation, full streaming support (unary, server, client, bidi)

### SOAP Implementation
- **Class**: `com.firefly.common.client.impl.SoapServiceClientImpl`
- **Implements**: `SoapClient`
- **Uses**: Apache CXF JAX-WS libraries
- **Features**: WSDL parsing, dynamic invocation, WS-Security, MTOM, SSL/TLS

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
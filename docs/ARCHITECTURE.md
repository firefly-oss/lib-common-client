# Architecture Documentation - lib-common-client

## System Overview

The `lib-common-client` library follows a layered architecture designed for reactive, non-blocking service communication in microservice environments. The architecture emphasizes separation of concerns, extensibility, and resilience patterns.

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
├─────────────────────────────────────────────────────────────┤
│                  ServiceClient Interface                     │
├─────────────────────────────────────────────────────────────┤
│   REST Implementation    │    gRPC Implementation           │
│   (RestServiceClientImpl)│    (GrpcServiceClientImpl)       │
├─────────────────────────────────────────────────────────────┤
│              Resilience Layer (Circuit Breaker)             │
├─────────────────────────────────────────────────────────────┤
│    WebClient (HTTP/REST) │  ManagedChannel (gRPC)          │
├─────────────────────────────────────────────────────────────┤
│                   Network Transport Layer                    │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. ServiceClient Interface Layer

**Location**: `com.firefly.common.client.ServiceClient`

The `ServiceClient` interface serves as the unified entry point for all service communication. It provides:

- **Protocol Abstraction**: Hides REST vs gRPC implementation details
- **Reactive API**: All methods return `Mono<T>` or `Flux<T>`
- **Type Safety**: Generic type support with compile-time checking
- **Fluent Interface**: Method chaining for request building

#### Key Interface Methods:

```java
public interface ServiceClient {
    // Factory methods
    static RestClientBuilder rest(String serviceName);
    static <T> GrpcClientBuilder<T> grpc(String serviceName, Class<T> stubType);
    
    // HTTP verb methods
    <R> RequestBuilder<R> get(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> post(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> put(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> delete(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> patch(String endpoint, Class<R> responseType);
    
    // Streaming methods
    <R> Flux<R> stream(String endpoint, Class<R> responseType);
    
    // Lifecycle methods
    String getServiceName();
    boolean isReady();
    Mono<Void> healthCheck();
    ClientType getClientType();
    void shutdown();
}
```

### 2. Implementation Layer

#### REST Implementation (`RestServiceClientImpl`)

**Location**: `com.firefly.common.client.impl.RestServiceClientImpl`

Built on Spring WebFlux's `WebClient`, this implementation provides:

- **Connection Pooling**: Reactor Netty connection pool with configurable limits
- **HTTP/2 Support**: Automatic protocol negotiation
- **Request/Response Processing**: JSON/XML serialization via Jackson
- **Error Mapping**: HTTP status codes to domain exceptions
- **Streaming Support**: Server-Sent Events and chunked responses

**Architecture**:
```java
RestServiceClientImpl
├── WebClient (Spring WebFlux)
├── ConnectionProvider (Reactor Netty)
├── HttpClient (Reactor Netty)
├── CircuitBreakerManager
└── RequestBuilderImpl<T>
    ├── Path parameter substitution
    ├── Query parameter handling  
    ├── Header management
    └── Body serialization
```

#### gRPC Implementation (`GrpcServiceClientImpl<T>`)

**Location**: `com.firefly.common.client.impl.GrpcServiceClientImpl<T>`

Built on gRPC Java libraries, this implementation provides:

- **Stub Management**: Type-safe gRPC stub handling
- **Channel Management**: ManagedChannel lifecycle
- **Metadata Propagation**: gRPC metadata handling
- **Streaming Support**: Unary, client-streaming, server-streaming, bidirectional
- **HTTP->gRPC Mapping**: Translates HTTP-style calls to gRPC methods

**Architecture**:
```java
GrpcServiceClientImpl<T>
├── ManagedChannel (gRPC)
├── Stub<T> (Generated gRPC stub)
├── CircuitBreakerManager
└── GrpcRequestBuilder<T>
    ├── HTTP to gRPC method mapping
    ├── Metadata handling
    └── Stream processing
```

### 3. Builder Pattern Layer

#### RestClientBuilder

**Location**: `com.firefly.common.client.builder.RestClientBuilder`

Responsible for:
- WebClient configuration and customization
- Connection pool setup
- Default header management
- Timeout configuration
- Validation of configuration parameters

**Key Configuration Methods**:
```java
public class RestClientBuilder {
    RestClientBuilder baseUrl(String baseUrl);
    RestClientBuilder timeout(Duration timeout);
    RestClientBuilder maxConnections(int maxConnections);
    RestClientBuilder defaultHeader(String name, String value);
    RestClientBuilder jsonContentType();
    RestClientBuilder xmlContentType();
    RestClientBuilder webClient(WebClient webClient);
    RestClientBuilder circuitBreakerManager(CircuitBreakerManager manager);
}
```

#### GrpcClientBuilder<T>

**Location**: `com.firefly.common.client.builder.GrpcClientBuilder<T>`

Responsible for:
- ManagedChannel configuration
- Stub factory setup
- TLS/plaintext configuration
- Keep-alive settings
- gRPC-specific optimizations

**Key Configuration Methods**:
```java
public class GrpcClientBuilder<T> {
    GrpcClientBuilder<T> address(String address);
    GrpcClientBuilder<T> timeout(Duration timeout);
    GrpcClientBuilder<T> usePlaintext();
    GrpcClientBuilder<T> useTransportSecurity();
    GrpcClientBuilder<T> stubFactory(Function<Object, T> stubFactory);
    GrpcClientBuilder<T> channel(ManagedChannel channel);
}
```

### 4. Resilience Layer

#### Circuit Breaker Architecture

**Location**: `com.firefly.common.resilience.*`

The circuit breaker implementation follows the State pattern:

```java
CircuitBreakerManager
├── CircuitBreakerConfig (Configuration)
├── CircuitBreakerState (CLOSED, OPEN, HALF_OPEN)
├── CircuitBreakerMetrics (Performance metrics)
├── SlidingWindow (Failure rate calculation)
└── Service-specific instances (Map<String, CircuitBreaker>)
```

**State Transitions**:
```
CLOSED ──(failure rate > threshold)──> OPEN
   ↑                                     │
   │                                     │
   └──(test calls succeed)─── HALF_OPEN ←┘
                                  │
                                  └──(test calls fail)──> OPEN
```

**Key Classes**:

- **CircuitBreakerConfig**: Immutable configuration with builder pattern
- **CircuitBreakerManager**: Manages multiple circuit breakers per service
- **CircuitBreakerMetrics**: Tracks success/failure rates, call counts
- **SlidingWindow**: Implements sliding window algorithm for failure calculation

#### Circuit Breaker Integration

Circuit breakers are automatically applied to all service calls:

```java
// REST example
private <R> Mono<R> applyCircuitBreakerProtection(Mono<R> operation) {
    return circuitBreakerManager.executeWithCircuitBreaker(serviceName, () -> operation)
        .doOnError(error -> log.warn("Circuit breaker detected failure: {}", error.getMessage()))
        .doOnSuccess(result -> log.debug("Circuit breaker allowed successful request"));
}

// gRPC example  
public <R> Mono<R> executeWithCircuitBreaker(Mono<R> operation) {
    return applyCircuitBreakerProtection(operation);
}
```

### 5. Configuration System

#### ServiceClientProperties

**Location**: `com.firefly.common.config.ServiceClientProperties`
**Configuration Prefix**: `firefly.service-client`

Hierarchical configuration structure:

```java
ServiceClientProperties
├── enabled: boolean
├── defaultTimeout: Duration
├── environment: Environment (DEVELOPMENT, TESTING, PRODUCTION)
├── defaultHeaders: Map<String, String>
├── rest: Rest
│   ├── maxConnections: int
│   ├── responseTimeout: Duration
│   ├── compressionEnabled: boolean
│   └── loggingEnabled: boolean
├── grpc: Grpc
│   ├── keepAliveTime: Duration
│   ├── maxInboundMessageSize: int
│   ├── usePlaintextByDefault: boolean
│   └── compressionEnabled: boolean
├── circuitBreaker: CircuitBreaker
│   ├── enabled: boolean
│   ├── failureRateThreshold: float
│   ├── waitDurationInOpenState: Duration
│   └── slidingWindowSize: int
└── metrics: Metrics
    ├── enabled: boolean
    └── collectDetailedMetrics: boolean
```

#### Environment-Specific Defaults

The configuration system applies environment-specific defaults:

```java
public void applyEnvironmentDefaults(Environment environment) {
    switch (environment) {
        case DEVELOPMENT:
            rest.loggingEnabled = true;
            rest.maxConnections = 50;
            grpc.usePlaintextByDefault = true;
            break;
        case TESTING:
            rest.maxConnections = 20;
            circuitBreaker.minimumNumberOfCalls = 2;
            break;
        case PRODUCTION:
            rest.maxConnections = 200;
            rest.compressionEnabled = true;
            grpc.usePlaintextByDefault = false;
            break;
    }
}
```

### 6. Auto-Configuration Layer

#### ServiceClientAutoConfiguration

**Location**: `com.firefly.common.config.ServiceClientAutoConfiguration`

Spring Boot auto-configuration that creates:

```java
@Bean
public WebClient.Builder webClientBuilder() {
    // Configures connection pool, timeouts, compression
}

@Bean  
public CircuitBreakerConfig circuitBreakerConfig() {
    // Creates default circuit breaker configuration
}

@Bean
public CircuitBreakerManager circuitBreakerManager() {
    // Creates circuit breaker manager instance
}

@Bean
public RestClientBuilder restClientBuilder() {
    // Creates default REST client builder
}

@Bean
public GrpcClientBuilderFactory grpcClientBuilderFactory() {
    // Creates factory for gRPC client builders
}
```

### 7. Request Processing Pipeline

#### REST Request Pipeline

```
Request Builder
     ↓
Path Parameter Substitution
     ↓
Query Parameter Encoding
     ↓
Header Addition
     ↓
Body Serialization (JSON/XML)
     ↓
Circuit Breaker Check
     ↓
WebClient Execution
     ↓
Response Deserialization
     ↓
Error Handling/Mapping
     ↓
Result (Mono<T>/Flux<T>)
```

#### gRPC Request Pipeline

```
Request Builder
     ↓
HTTP Method to gRPC Method Mapping
     ↓
Metadata Addition
     ↓
Message Serialization (Protobuf)
     ↓
Circuit Breaker Check
     ↓
gRPC Stub Call
     ↓
Response Deserialization
     ↓
Error Handling/Mapping
     ↓
Result (Mono<T>/Flux<T>)
```

## Threading Model

### Reactive Threading

The library follows reactive programming principles:

- **Event Loop**: Reactor Netty event loop for I/O operations
- **Non-blocking**: No thread blocking for I/O operations
- **Backpressure**: Automatic flow control via Reactor streams
- **Scheduler Integration**: Pluggable schedulers for different operations

### Thread Pool Configuration

```java
// WebClient uses Reactor Netty event loop
WebClient client = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(
        HttpClient.create(connectionProvider)
            .runOn(LoopResources.create("http-client"))
    ))
    .build();

// gRPC uses its own thread pool
ManagedChannel channel = ManagedChannelBuilder.forTarget(address)
    .executor(customExecutor) // Optional custom executor
    .build();
```

## Error Handling Strategy

### Exception Hierarchy

```java
ServiceClientException (Base)
├── ServiceNotFoundException (404, NOT_FOUND)
├── ServiceUnavailableException (503, UNAVAILABLE) 
├── ServiceAuthenticationException (401, UNAUTHENTICATED)
├── ServiceValidationException (400, INVALID_ARGUMENT)
└── CircuitBreakerException
    ├── CircuitBreakerOpenException
    └── CircuitBreakerTimeoutException
```

### Error Mapping

#### REST Error Mapping

```java
// HTTP status codes to domain exceptions
switch (statusCode.value()) {
    case 404: throw new ServiceNotFoundException(...);
    case 503: throw new ServiceUnavailableException(...);  
    case 401: throw new ServiceAuthenticationException(...);
    case 400: throw new ServiceValidationException(...);
    default: throw new ServiceClientException(...);
}
```

#### gRPC Error Mapping

```java
// gRPC status codes to domain exceptions  
switch (status.getCode()) {
    case NOT_FOUND: throw new ServiceNotFoundException(...);
    case UNAVAILABLE: throw new ServiceUnavailableException(...);
    case UNAUTHENTICATED: throw new ServiceAuthenticationException(...);
    case INVALID_ARGUMENT: throw new ServiceValidationException(...);
    default: throw new ServiceClientException(...);
}
```

## Observability Integration

### Metrics Collection

The library integrates with Micrometer for metrics:

```java
// Request metrics
Timer.Sample sample = Timer.start(meterRegistry);
return operation
    .doOnSuccess(result -> {
        sample.stop(Timer.builder("service.client.request")
            .tag("service", serviceName)
            .tag("method", method)
            .tag("status", "success")
            .register(meterRegistry));
    })
    .doOnError(error -> {
        sample.stop(Timer.builder("service.client.request")
            .tag("service", serviceName) 
            .tag("method", method)
            .tag("status", "error")
            .register(meterRegistry));
    });
```

### Health Indicators

Spring Boot Actuator integration:

```java
@Component
public class ServiceClientHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return serviceClient.isReady() 
            ? Health.up().build()
            : Health.down().build();
    }
}
```

### Distributed Tracing

Integration with Spring Cloud Sleuth:

```java
// Automatic trace propagation in HTTP headers
WebClient client = WebClient.builder()
    .filter(traceExchangeFilterFunction) // Automatic trace injection
    .build();

// gRPC metadata propagation
Metadata metadata = new Metadata();
metadata.put(TRACE_ID_KEY, currentTraceId);
```

## Extension Points

### Custom Interceptors

```java
public interface ServiceClientInterceptor {
    Mono<ClientResponse> intercept(ClientRequest request, ExchangeFunction next);
}

// Usage
RestClientBuilder builder = ServiceClient.rest("service")
    .interceptor(new CustomAuthInterceptor())
    .interceptor(new CustomLoggingInterceptor());
```

### Custom Circuit Breaker Configuration

```java
@Bean
public CircuitBreakerConfig customCircuitBreakerConfig() {
    return CircuitBreakerConfig.builder()
        .failureRateThreshold(60.0)
        .minimumNumberOfCalls(10)
        .slidingWindowSize(20)
        .build();
}
```

## Performance Characteristics

### Connection Management

- **REST**: Reactor Netty connection pool with configurable limits
- **gRPC**: ManagedChannel with connection multiplexing
- **Keep-Alive**: Configurable keep-alive for both protocols
- **Compression**: Automatic compression for both REST and gRPC

### Memory Usage

- **Bounded Buffers**: Configurable memory limits for responses
- **Streaming**: Efficient handling of large responses via reactive streams
- **Connection Pooling**: Reuse of connections to minimize overhead

### Scalability

- **Non-blocking I/O**: Event-driven architecture for high concurrency
- **Backpressure**: Automatic flow control prevents memory exhaustion  
- **Circuit Breaker**: Prevents cascade failures in distributed systems
- **Resource Isolation**: Per-service configuration and circuit breakers

This architecture provides a robust, scalable, and extensible foundation for service communication in reactive microservice environments.
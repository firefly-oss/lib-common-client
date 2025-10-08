# Architecture Documentation - lib-common-client

## System Overview

The `lib-common-client` library follows a layered architecture designed for reactive, non-blocking service communication in microservice environments. The architecture emphasizes **protocol-specific interfaces**, separation of concerns, extensibility, and resilience patterns.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Application Layer                                │
├──────────────────────────────────────────────────────────────────────────┤
│                    ServiceClient Core Interface                          │
│                  (Factory Methods & Lifecycle)                           │
├──────────────────────────────────────────────────────────────────────────┤
│   RestClient    │    GrpcClient<T>    │    SoapClient                    │
│   (HTTP Verbs)  │  (gRPC Operations)  │ (SOAP Operations)                │
├──────────────────────────────────────────────────────────────────────────┤
│  REST Implementation  │  gRPC Implementation  │  SOAP Implementation     │
│(RestServiceClientImpl)│(GrpcServiceClientImpl)│ (SoapServiceClientImpl)  │
├──────────────────────────────────────────────────────────────────────────┤
│                   Resilience Layer (Circuit Breaker)                     │
├──────────────────────────────────────────────────────────────────────────┤
│ WebClient (HTTP/REST) │ ManagedChannel (gRPC) │ Apache CXF (SOAP/WSDL)   │
├──────────────────────────────────────────────────────────────────────────┤
│                        Network Transport Layer                           │
└──────────────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. ServiceClient Interface Layer

The library uses a **protocol-specific facade pattern** where each protocol (REST, gRPC, SOAP) has its own specialized interface extending a common `ServiceClient` core. This design provides:

- **Natural Protocol APIs**: Each protocol exposes methods that match its native semantics
- **Type Safety**: Protocol-specific types prevent misuse (e.g., can't call HTTP verbs on gRPC clients)
- **Reactive API**: All methods return `Mono<T>` or `Flux<T>`
- **Fluent Interface**: Method chaining for request building

#### ServiceClient Core Interface

**Location**: `com.firefly.common.client.ServiceClient`

The core interface provides factory methods and common lifecycle operations:

```java
public interface ServiceClient {
    // Factory methods
    static RestClientBuilder rest(String serviceName);
    static <T> GrpcClientBuilder<T> grpc(String serviceName, Class<T> stubType);
    static SoapClientBuilder soap(String serviceName);

    // Lifecycle methods (common to all protocols)
    String getServiceName();
    boolean isReady();
    Mono<Void> healthCheck();
    ClientType getClientType();
    void shutdown();
}
```

#### RestClient Interface

**Location**: `com.firefly.common.client.RestClient`

Protocol-specific interface for REST/HTTP services:

```java
public interface RestClient extends ServiceClient {
    // HTTP verb methods
    <R> RequestBuilder<R> get(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> post(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> put(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> delete(String endpoint, Class<R> responseType);
    <R> RequestBuilder<R> patch(String endpoint, Class<R> responseType);

    // Streaming methods
    <R> Flux<R> stream(String endpoint, Class<R> responseType);

    // REST-specific metadata
    String getBaseUrl();

    // Fluent request builder
    interface RequestBuilder<R> {
        RequestBuilder<R> withPathParam(String name, Object value);
        RequestBuilder<R> withQueryParam(String name, Object value);
        RequestBuilder<R> withHeader(String name, String value);
        RequestBuilder<R> withBody(Object body);
        RequestBuilder<R> withTimeout(Duration timeout);
        Mono<R> execute();
    }
}
```

#### GrpcClient Interface

**Location**: `com.firefly.common.client.GrpcClient`

Protocol-specific interface for gRPC services with full streaming support:

```java
public interface GrpcClient<T> extends ServiceClient {
    // Direct stub access
    T getStub();

    // Unary operations
    <R> Mono<R> unary(Function<T, R> operation);
    <R> Mono<R> execute(Function<T, R> operation);

    // Server streaming
    <R> Flux<R> serverStream(Function<T, Iterator<R>> operation);
    <R> Flux<R> executeStream(Function<T, Publisher<R>> operation);

    // Client streaming
    <Req, Res> Mono<Res> clientStream(
        Function<T, StreamObserver<Res>> operation,
        Publisher<Req> requests
    );

    // Bidirectional streaming
    <Req, Res> Flux<Res> bidiStream(
        Function<T, StreamObserver<Res>> operation,
        Publisher<Req> requests
    );

    // gRPC-specific metadata
    String getAddress();
    ManagedChannel getChannel();
}
```

#### SoapClient Interface

**Location**: `com.firefly.common.client.SoapClient`

Protocol-specific interface for SOAP/WSDL services:

```java
public interface SoapClient extends ServiceClient {
    // Operation invocation
    OperationBuilder invoke(String operationName);
    <Req, Res> Mono<Res> invokeAsync(String operationName, Req request, Class<Res> responseType);

    // WSDL introspection
    List<String> getOperations();
    <P> P getPort(Class<P> portType);

    // SOAP-specific metadata
    String getWsdlUrl();
    QName getServiceQName();
    QName getPortQName();

    // Fluent operation builder
    interface OperationBuilder {
        OperationBuilder withParameter(String name, Object value);
        OperationBuilder withHeader(String name, String value);
        OperationBuilder withTimeout(Duration timeout);
        <R> Mono<R> execute(Class<R> responseType);
    }
}
```

### 2. Implementation Layer

#### REST Implementation (`RestServiceClientImpl`)

**Location**: `com.firefly.common.client.impl.RestServiceClientImpl`

Implements `RestClient` interface. Built on Spring WebFlux's `WebClient`, this implementation provides:

- **Connection Pooling**: Reactor Netty connection pool with configurable limits
- **HTTP/2 Support**: Automatic protocol negotiation
- **Request/Response Processing**: JSON/XML serialization via Jackson
- **Error Mapping**: HTTP status codes to domain exceptions
- **Streaming Support**: Server-Sent Events and chunked responses
- **Natural HTTP API**: HTTP verbs (GET, POST, PUT, DELETE, PATCH) with fluent request builders

**Architecture**:
```java
RestServiceClientImpl implements RestClient
├── WebClient (Spring WebFlux)
├── ConnectionProvider (Reactor Netty)
├── HttpClient (Reactor Netty)
├── CircuitBreakerManager
└── RestRequestBuilder<T> implements RestClient.RequestBuilder<T>
    ├── Path parameter substitution
    ├── Query parameter handling
    ├── Header management
    └── Body serialization
```

#### gRPC Implementation (`GrpcServiceClientImpl<T>`)

**Location**: `com.firefly.common.client.impl.GrpcServiceClientImpl<T>`

Implements `GrpcClient<T>` interface. Built on gRPC Java libraries, this implementation provides:

- **Stub Management**: Type-safe gRPC stub handling with direct access
- **Channel Management**: ManagedChannel lifecycle
- **Metadata Propagation**: gRPC metadata handling
- **Full Streaming Support**: Unary, client-streaming, server-streaming, bidirectional
- **Native gRPC API**: Direct stub access and functional-style operation invocation
- **Circuit Breaker Integration**: Automatic resilience for all operation types

**Architecture**:
```java
GrpcServiceClientImpl<T> implements GrpcClient<T>
├── ManagedChannel (gRPC)
├── Stub<T> (Generated gRPC stub)
├── CircuitBreakerManager
└── Native gRPC operations
    ├── unary() - Single request/response
    ├── serverStream() - Server streaming
    ├── clientStream() - Client streaming
    ├── bidiStream() - Bidirectional streaming
    └── execute/executeStream() - Generic operations
```

#### SOAP Implementation (`SoapServiceClientImpl`)

**Location**: `com.firefly.common.client.impl.SoapServiceClientImpl`

Implements `SoapClient` interface. Built on Apache CXF JAX-WS libraries, this implementation provides:

- **WSDL Parsing**: Automatic service discovery from WSDL without code generation
- **Dynamic Invocation**: Runtime operation discovery and invocation
- **WS-Security**: Username token authentication support
- **MTOM/XOP**: Message Transmission Optimization Mechanism for binary data
- **SSL/TLS Support**: Custom trust stores and client certificates
- **Connection Pooling**: HTTP connection reuse and keep-alive
- **SOAP Fault Mapping**: SOAP faults to domain exceptions
- **Operation-Based API**: Natural SOAP operation invocation with fluent builders

**Architecture**:
```java
SoapServiceClientImpl implements SoapClient
├── Service (JAX-WS)
├── Port (Dynamic proxy)
├── HTTPConduit (Apache CXF)
│   ├── HTTPClientPolicy (Timeouts, connection pooling)
│   ├── TLSClientParameters (SSL/TLS configuration)
│   └── WSS4JOutInterceptor (WS-Security)
├── CircuitBreakerManager
├── LoggingInterceptors (Request/response logging)
└── SoapOperationBuilder implements SoapClient.OperationBuilder
    ├── Operation name mapping
    ├── JAXB marshalling/unmarshalling
    ├── SOAP header handling
    └── Fault processing
```

**Key Features**:

- **WSDL URL Authentication**: Automatic credential extraction from WSDL URLs
  ```
  https://secure.example.com/service?WSDL&user=username&password=pass
  ```
- **Operation Discovery**: `getOperations()` returns list of available SOAP operations
- **Schema Validation**: Optional XML schema validation
- **Message Logging**: Configurable SOAP message logging for debugging
- **Health Checks**: WSDL availability verification

### 3. Builder Pattern Layer

#### RestClientBuilder

**Location**: `com.firefly.common.client.builder.RestClientBuilder`

Builds `RestClient` instances. Responsible for:
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
    RestClient build();  // Returns RestClient
}
```

#### GrpcClientBuilder<T>

**Location**: `com.firefly.common.client.builder.GrpcClientBuilder<T>`

Builds `GrpcClient<T>` instances. Responsible for:
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
    GrpcClientBuilder<T> stubFactory(Function<ManagedChannel, T> stubFactory);
    GrpcClientBuilder<T> channel(ManagedChannel channel);
    GrpcClientBuilder<T> circuitBreakerManager(CircuitBreakerManager manager);
    GrpcClient<T> build();  // Returns GrpcClient<T>
}
```

#### SoapClientBuilder

**Location**: `com.firefly.common.client.builder.SoapClientBuilder`

Builds `SoapClient` instances. Responsible for:
- WSDL URL configuration with automatic credential extraction
- Service and port QName selection
- WS-Security configuration
- SSL/TLS trust store and key store setup
- MTOM enablement for binary attachments
- Schema validation configuration
- Custom SOAP properties and headers

**Key Configuration Methods**:
```java
public class SoapClientBuilder {
    SoapClientBuilder wsdlUrl(String wsdlUrl);
    SoapClientBuilder serviceName(QName serviceName);
    SoapClientBuilder portName(QName portName);
    SoapClientBuilder timeout(Duration timeout);
    SoapClientBuilder credentials(String username, String password);
    SoapClientBuilder enableMtom();
    SoapClientBuilder disableMtom();
    SoapClientBuilder trustStore(String path, String password);
    SoapClientBuilder keyStore(String path, String password);
    SoapClientBuilder disableSslVerification();
    SoapClientBuilder header(String name, String value);
    SoapClientBuilder property(String name, Object value);
    SoapClientBuilder endpointAddress(String address);
    SoapClientBuilder enableSchemaValidation();
    SoapClientBuilder disableSchemaValidation();
    SoapClientBuilder circuitBreakerManager(CircuitBreakerManager manager);
    SoapClient build();  // Returns SoapClient
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
├── soap: Soap
│   ├── defaultTimeout: Duration
│   ├── connectionTimeout: Duration
│   ├── receiveTimeout: Duration
│   ├── mtomEnabled: boolean
│   ├── schemaValidationEnabled: boolean
│   ├── messageLoggingEnabled: boolean
│   ├── maxMessageSize: int
│   ├── wsAddressingEnabled: boolean
│   └── wsSecurityEnabled: boolean
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
            soap.messageLoggingEnabled = true;
            soap.schemaValidationEnabled = false;
            break;
        case TESTING:
            rest.maxConnections = 20;
            circuitBreaker.minimumNumberOfCalls = 2;
            soap.schemaValidationEnabled = true;
            break;
        case PRODUCTION:
            rest.maxConnections = 200;
            rest.compressionEnabled = true;
            grpc.usePlaintextByDefault = false;
            soap.messageLoggingEnabled = false;
            soap.schemaValidationEnabled = true;
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
RestClient.get/post/put/delete/patch()
     ↓
RestClient.RequestBuilder
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
GrpcClient.unary/serverStream/clientStream/bidiStream()
     ↓
Function<T, R> Operation
     ↓
Stub Access (getStub())
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

#### SOAP Request Pipeline

```
SoapClient.invoke() or invokeAsync()
     ↓
SoapClient.OperationBuilder (optional)
     ↓
WSDL URL Credential Extraction
     ↓
Operation Name Resolution
     ↓
JAXB Object Marshalling
     ↓
SOAP Envelope Creation
     ↓
WS-Security Header Addition
     ↓
Custom Header Addition
     ↓
Circuit Breaker Check
     ↓
HTTP Conduit Execution (Apache CXF)
     ↓
SSL/TLS Handshake (if configured)
     ↓
SOAP Response Processing
     ↓
JAXB Object Unmarshalling
     ↓
SOAP Fault Handling/Mapping
     ↓
Result (Mono<T>)
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
├── SoapFaultException (SOAP Fault)
│   ├── faultCode: String
│   ├── faultString: String
│   ├── faultActor: String
│   └── faultDetail: String
├── WsdlParsingException (WSDL parsing errors)
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

#### SOAP Error Mapping

```java
// SOAP faults to domain exceptions
try {
    Object result = method.invoke(port, request);
    return responseType.cast(result);
} catch (SOAPFaultException e) {
    String faultCode = e.getFault().getFaultCode();
    String faultString = e.getFault().getFaultString();
    String faultActor = e.getFault().getFaultActor();
    String faultDetail = extractFaultDetail(e.getFault());

    throw new SoapFaultException(faultCode, faultString, faultActor, faultDetail, e);
} catch (Exception e) {
    throw new ServiceClientException("SOAP operation failed", e);
}
```

**SOAP Fault Helper Methods**:
```java
public class SoapFaultException extends ServiceClientException {
    public boolean isClientFault() {
        return faultCode != null && faultCode.contains("Client");
    }

    public boolean isServerFault() {
        return faultCode != null && faultCode.contains("Server");
    }

    public boolean isVersionMismatchFault() {
        return faultCode != null && faultCode.contains("VersionMismatch");
    }
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
- **SOAP**: Apache CXF HTTP conduit with connection pooling and keep-alive
- **Keep-Alive**: Configurable keep-alive for all protocols
- **Compression**: Automatic compression for REST, gRPC, and SOAP

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
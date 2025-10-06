# Firefly Common Client Library

[![Maven Central](https://img.shields.io/badge/Maven-1.0.0--SNAPSHOT-blue)](https://maven.apache.org)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green)](https://spring.io/projects/spring-boot)
[![Reactive](https://img.shields.io/badge/Reactive-WebFlux-purple)](https://projectreactor.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A comprehensive, reactive service communication framework for microservice architectures developed by **Firefly Software Solutions Inc**. Provides unified REST and gRPC client interfaces with built-in resilience patterns, circuit breakers, and comprehensive observability.

> **Developed by [Firefly Software Solutions Inc](https://getfirefly.io)** - Building enterprise-grade solutions for modern microservice architectures.

## 🚀 Features

### Core Capabilities
- **🔗 Unified API**: Single interface for REST and gRPC communication
- **⚡ Reactive Programming**: Non-blocking operations with Spring WebFlux and Project Reactor
- **🛡️ Circuit Breaker**: Advanced resilience patterns with automatic recovery
- **💊 Health Checks**: Built-in service health monitoring and diagnostics
- **📊 Observability**: Metrics, tracing, and logging integration
- **🔄 Streaming Support**: Server-Sent Events and gRPC streaming
- **🎯 Type Safety**: Strong typing with generics and compile-time validation

### Advanced Features
- **🏗️ Builder Pattern**: Fluent API for client configuration
- **🔌 Interceptors**: Extensible request/response processing pipeline
- **⚙️ Auto-Configuration**: Zero-config Spring Boot integration
- **🌍 Environment Profiles**: Development, testing, and production optimizations
- **🔐 Security**: Authentication, authorization, and TLS support
- **🧪 Testing Support**: Comprehensive testing utilities and mocks

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Installation](#-installation)
- [Basic Usage](#-basic-usage)
- [Configuration](#-configuration)
- [Advanced Features](#-advanced-features)
- [Architecture](#-architecture)
- [Documentation](#-documentation)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [About Firefly Software Solutions Inc](#-about-firefly-software-solutions-inc)

---
## 🚀 Quick Start

### Installation

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Auto-Configuration

The library auto-configures with Spring Boot:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ ServiceClient components are automatically available
    }
}
```

### Simple REST Example

```java
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;

@Service
public class UserService {
    
    private final ServiceClient userClient;
    
    public UserService() {
        this.userClient = ServiceClient.rest("user-service")
            .baseUrl("http://user-service:8080")
            .timeout(Duration.ofSeconds(30))
            .jsonContentType()
            .build();
    }
    
    public Mono<User> getUser(String userId) {
        return userClient.get("/users/{id}", User.class)
            .withPathParam("id", userId)
            .execute();
    }
    
    public Mono<User> createUser(CreateUserRequest request) {
        return userClient.post("/users", User.class)
            .withBody(request)
            .withHeader("X-Request-ID", UUID.randomUUID().toString())
            .execute();
    }
    
    public Flux<User> searchUsers(String query) {
        return userClient.get("/users/search", new TypeReference<List<User>>() {})
            .withQueryParam("q", query)
            .execute()
            .flatMapMany(Flux::fromIterable);
    }
}
```

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.firefly:lib-common-client:1.0.0-SNAPSHOT'
```

### Requirements

- **Java**: 21 or higher
- **Spring Boot**: 3.2 or higher
- **Spring WebFlux**: For reactive support
- **Project Reactor**: For reactive streams

## 🎯 First Time Setup

This section guides you through setting up your first service client from scratch.

### Step 1: Add the Dependency

Add the library to your `pom.xml` (Maven) or `build.gradle` (Gradle) as shown in the [Installation](#-installation) section above.

### Step 2: Enable Auto-Configuration

The library automatically configures itself when Spring Boot detects it on the classpath. Simply ensure your application has `@SpringBootApplication`:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ ServiceClient components are now automatically available
    }
}
```

### Step 3: Create Your First REST Client

Create a service class and build your first REST client:

```java
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class UserService {

    private final ServiceClient userClient;

    public UserService() {
        // Build a REST client with essential properties
        this.userClient = ServiceClient.rest("user-service")
            .baseUrl("http://localhost:8080")           // Required: Base URL of the service
            .timeout(Duration.ofSeconds(30))            // Optional: Request timeout (default: 30s)
            .jsonContentType()                          // Optional: Set Content-Type to application/json
            .build();
    }

    public Mono<User> getUser(String userId) {
        return userClient.get("/users/{id}", User.class)
            .withPathParam("id", userId)
            .execute();
    }
}
```

### Step 4: Configure Properties (Optional)

Create an `application.yml` file to customize client behavior:

```yaml
firefly:
  service-client:
    enabled: true                    # Enable the library (default: true)
    default-timeout: 30s             # Global timeout for all clients
    environment: DEVELOPMENT         # DEVELOPMENT, TESTING, or PRODUCTION

    # REST-specific settings
    rest:
      max-connections: 100           # Connection pool size
      response-timeout: 30s          # How long to wait for responses
      connect-timeout: 10s           # How long to wait for connection
      compression-enabled: true      # Enable gzip compression
      logging-enabled: true          # Enable request/response logging (useful for debugging)

    # Circuit breaker for resilience
    circuit-breaker:
      enabled: true                  # Enable circuit breaker pattern
      failure-rate-threshold: 50.0   # Open circuit after 50% failures
      minimum-number-of-calls: 5     # Need at least 5 calls before evaluating
```

### Step 5: Understanding Key Properties

#### Essential REST Client Properties

| Property | Description | Default | When to Change |
|----------|-------------|---------|----------------|
| `baseUrl()` | The base URL of your service | None (required) | Always set this |
| `timeout()` | Maximum time to wait for response | 30s | Increase for slow services |
| `jsonContentType()` | Sets Content-Type to application/json | Not set | Use when sending JSON |
| `maxConnections()` | Connection pool size | 100 | Increase for high-traffic services |
| `defaultHeader()` | Add headers to all requests | None | Use for auth tokens, API keys |

#### Essential gRPC Client Properties

| Property | Description | Default | When to Change |
|----------|-------------|---------|----------------|
| `address()` | gRPC service address (host:port) | None (required) | Always set this |
| `usePlaintext()` | Disable TLS (for development) | false | Use in development only |
| `timeout()` | Maximum time for gRPC calls | 30s | Increase for slow operations |
| `stubFactory()` | Factory to create gRPC stub | None (required) | Always provide this |

### Step 6: Create Your First gRPC Client (Optional)

If you're using gRPC, here's how to set up your first gRPC client:

```java
import com.firefly.common.client.ServiceClient;
import com.firefly.common.client.impl.GrpcServiceClientImpl;
import com.example.grpc.PaymentServiceGrpc;
import com.example.grpc.PaymentServiceGrpc.PaymentServiceStub;

@Service
public class PaymentService {

    private final GrpcServiceClientImpl<PaymentServiceStub> paymentClient;

    public PaymentService() {
        ServiceClient client = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("localhost:9090")                                      // Required: gRPC service address
            .usePlaintext()                                                 // Optional: Use for development (no TLS)
            .timeout(Duration.ofSeconds(30))                                // Optional: Call timeout
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))   // Required: How to create the stub
            .build();

        this.paymentClient = (GrpcServiceClientImpl<PaymentServiceStub>) client;
    }

    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        return paymentClient.executeWithCircuitBreaker(
            Mono.fromCallable(() -> paymentClient.getStub().processPayment(request))
        );
    }
}
```

### Step 7: Common Configuration Patterns

#### Pattern 1: Service with Authentication

```java
ServiceClient authenticatedClient = ServiceClient.rest("secure-service")
    .baseUrl("https://api.example.com")
    .defaultHeader("Authorization", "Bearer your-token-here")
    .defaultHeader("X-API-Key", "your-api-key")
    .jsonContentType()
    .build();
```

#### Pattern 2: High-Performance Service

```java
ServiceClient highPerfClient = ServiceClient.rest("high-perf-service")
    .baseUrl("http://fast-service:8080")
    .timeout(Duration.ofSeconds(5))        // Short timeout for fast service
    .maxConnections(200)                   // Large connection pool
    .build();
```

#### Pattern 3: External Service with Retries

```yaml
# In application.yml
firefly:
  service-client:
    retry:
      enabled: true
      max-attempts: 3                      # Retry up to 3 times
      initial-interval: 1s                 # Wait 1s before first retry
      multiplier: 2.0                      # Double wait time each retry
```

### Next Steps

- **Read the [Quick Start Guide](docs/QUICKSTART.md)** for more examples
- **See the [Configuration Reference](docs/CONFIGURATION.md)** for all available properties
- **Check the [Architecture Guide](docs/ARCHITECTURE.md)** to understand how it works
- **Review [Testing Guide](docs/TESTING.md)** to learn how to test your clients

## 🔧 Basic Usage

### REST Client Examples

#### GET Requests

```java
// Simple GET
Mono<User> user = client.get("/users/{id}", User.class)
    .withPathParam("id", "123")
    .execute();

// GET with query parameters
Mono<List<User>> users = client.get("/users", new TypeReference<List<User>>() {})
    .withQueryParam("page", 0)
    .withQueryParam("size", 10)
    .withQueryParam("sort", "name")
    .execute();

// GET with custom headers
Mono<User> user = client.get("/users/{id}", User.class)
    .withPathParam("id", "123")
    .withHeader("Accept-Language", "en-US")
    .withHeader("X-Client-Version", "1.0.0")
    .execute();
```

#### POST/PUT Requests

```java
// POST with JSON body
Mono<User> created = client.post("/users", User.class)
    .withBody(new CreateUserRequest("John Doe", "john@example.com"))
    .execute();

// PUT with path parameter
Mono<User> updated = client.put("/users/{id}", User.class)
    .withPathParam("id", "123")
    .withBody(updateRequest)
    .execute();

// PATCH request
Mono<User> patched = client.patch("/users/{id}", User.class)
    .withPathParam("id", "123")
    .withBody(Map.of("status", "active"))
    .execute();
```

#### Streaming Responses

```java
// Server-Sent Events
Flux<Event> events = client.stream("/events", Event.class)
    .doOnNext(event -> log.info("Received: {}", event))
    .onErrorContinue((error, item) -> log.warn("Stream error: {}", error.getMessage()));

// Process streaming data
events.bufferTimeout(100, Duration.ofSeconds(5))
    .flatMap(this::processBatch)
    .subscribe();
```

### gRPC Client Examples

#### Basic gRPC Setup

```java
import com.firefly.common.client.ServiceClient;
import com.firefly.common.client.impl.GrpcServiceClientImpl;
import com.example.grpc.PaymentServiceGrpc;
import com.example.grpc.PaymentServiceGrpc.PaymentServiceStub;

@Service
public class PaymentService {
    
    private final GrpcServiceClientImpl<PaymentServiceStub> grpcClient;
    
    public PaymentService() {
        ServiceClient client = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("payment-service:9090")
            .usePlaintext()
            .timeout(Duration.ofSeconds(30))
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
            .build();
        
        this.grpcClient = (GrpcServiceClientImpl<PaymentServiceStub>) client;
    }
    
    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        return grpcClient.executeWithCircuitBreaker(
            Mono.fromCallable(() -> grpcClient.getStub().processPayment(request))
        );
    }
    
    public Flux<TransactionEvent> streamTransactions(String accountId) {
        return grpcClient.executeStreamWithCircuitBreaker(
            // gRPC streaming call implementation
            createTransactionStream(accountId)
        );
    }
}
```

## ⚙️ Configuration

### Application Properties

Comprehensive configuration options in `application.yml`:

```yaml
firefly:
  service-client:
    enabled: true
    default-timeout: 30s
    environment: DEVELOPMENT  # DEVELOPMENT, TESTING, PRODUCTION
    
    # Global default headers
    default-headers:
      User-Agent: "MyApp/1.0"
      X-Client-Version: "1.0.0"
    
    # REST Configuration
    rest:
      max-connections: 100
      max-idle-time: 5m
      max-life-time: 30m
      pending-acquire-timeout: 10s
      response-timeout: 30s
      connect-timeout: 10s
      read-timeout: 30s
      compression-enabled: true
      logging-enabled: false
      follow-redirects: true
      max-in-memory-size: 1048576  # 1MB
      max-retries: 3
      default-content-type: "application/json"
      default-accept-type: "application/json"
    
    # gRPC Configuration  
    grpc:
      keep-alive-time: 5m
      keep-alive-timeout: 30s
      keep-alive-without-calls: true
      max-inbound-message-size: 4194304  # 4MB
      max-inbound-metadata-size: 8192    # 8KB
      call-timeout: 30s
      retry-enabled: true
      use-plaintext-by-default: true
      compression-enabled: true
      max-concurrent-streams: 100
    
    # Circuit Breaker Configuration
    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50.0
      minimum-number-of-calls: 5
      sliding-window-size: 10
      wait-duration-in-open-state: 60s
      permitted-number-of-calls-in-half-open-state: 3
      max-wait-duration-in-half-open-state: 30s
      call-timeout: 10s
      slow-call-duration-threshold: 5s
      slow-call-rate-threshold: 100.0
      automatic-transition-from-open-to-half-open-enabled: true

    # Retry Configuration
    retry:
      enabled: true
      max-attempts: 3
      initial-interval: 1s
      multiplier: 2.0
      max-interval: 30s
    
    # Metrics Configuration
    metrics:
      enabled: true
      collect-detailed-metrics: false
      histogram-buckets: [0.001, 0.01, 0.1, 1, 10]
    
    # Security Configuration
    security:
      tls-enabled: false
      trust-store-path: ""
      trust-store-password: ""
      key-store-path: ""
      key-store-password: ""
```

### Java Configuration

```java
@Configuration
public class ServiceClientConfig {
    
    @Bean
    public ServiceClient customerServiceClient() {
        return ServiceClient.rest("customer-service")
            .baseUrl("http://customer-service:8080")
            .timeout(Duration.ofSeconds(30))
            .defaultHeader("Authorization", "Bearer ${auth.token}")
            .jsonContentType()
            .maxConnections(50)
            .build();
    }
    
    @Bean
    public ServiceClient orderServiceClient() {
        return ServiceClient.rest("order-service")
            .baseUrl("https://order-service.example.com")
            .timeout(Duration.ofSeconds(45))
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    @Bean
    public ServiceClient notificationServiceClient() {
        return ServiceClient.grpc("notification-service", NotificationServiceStub.class)
            .address("notification-service:9090")
            .usePlaintext()
            .timeout(Duration.ofSeconds(15))
            .stubFactory(channel -> NotificationServiceGrpc.newStub(channel))
            .build();
    }
    
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

## 🔥 Advanced Features

### Circuit Breaker Management

```java
@Service
public class CircuitBreakerMonitorService {
    
    private final CircuitBreakerManager circuitBreakerManager;
    
    public CircuitBreakerMonitorService(CircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
    }
    
    public CircuitBreakerState getServiceState(String serviceName) {
        return circuitBreakerManager.getState(serviceName);
    }
    
    public CircuitBreakerMetrics getServiceMetrics(String serviceName) {
        var metrics = circuitBreakerManager.getMetrics(serviceName);
        log.info("Service {}: Total calls: {}, Success rate: {}%", 
            serviceName, metrics.getTotalCalls(), 
            metrics.getSuccessRate() * 100);
        return metrics;
    }
    
    public void forceOpenCircuitBreaker(String serviceName) {
        circuitBreakerManager.forceOpen(serviceName);
    }
    
    public void resetCircuitBreaker(String serviceName) {
        circuitBreakerManager.reset(serviceName);
    }
}
```

### Error Handling Strategies

```java
import com.firefly.common.client.exception.*;

public Mono<User> getUserWithAdvancedErrorHandling(String userId) {
    return userClient.get("/users/{id}", User.class)
        .withPathParam("id", userId)
        .withTimeout(Duration.ofSeconds(10))
        .execute()
        .onErrorMap(ServiceNotFoundException.class, 
            ex -> new UserNotFoundException("User not found: " + userId, ex))
        .onErrorMap(ServiceUnavailableException.class,
            ex -> new ServiceTemporarilyUnavailableException("User service unavailable", ex))
        .onErrorMap(ServiceAuthenticationException.class,
            ex -> new UnauthorizedException("Authentication failed", ex))
        .onErrorMap(CircuitBreakerOpenException.class,
            ex -> new ServiceDegradedException("User service circuit breaker is open", ex))
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof ServiceUnavailableException))
        .doOnError(error -> log.error("Failed to get user {}: {}", userId, error.getMessage()))
        .doOnSuccess(user -> log.debug("Successfully retrieved user: {}", userId));
}
```

### Health Monitoring

```java
@Component
public class ServiceHealthMonitor {
    
    private final List<ServiceClient> serviceClients;
    
    public ServiceHealthMonitor(List<ServiceClient> serviceClients) {
        this.serviceClients = serviceClients;
    }
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void monitorServiceHealth() {
        serviceClients.parallelStream()
            .forEach(client -> {
                client.healthCheck()
                    .doOnSuccess(v -> log.info("Service {} is healthy", client.getServiceName()))
                    .doOnError(error -> log.warn("Service {} health check failed: {}", 
                        client.getServiceName(), error.getMessage()))
                    .subscribe();
            });
    }
    
    public Mono<Map<String, Boolean>> getAllServiceHealth() {
        return Flux.fromIterable(serviceClients)
            .flatMap(client -> 
                client.healthCheck()
                    .map(v -> Map.entry(client.getServiceName(), true))
                    .onErrorReturn(Map.entry(client.getServiceName(), false))
            )
            .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
```

### Parallel Service Calls

```java
public Mono<UserProfile> getUserProfile(String userId) {
    Mono<User> userMono = userService.get("/users/{id}", User.class)
        .withPathParam("id", userId)
        .execute();
    
    Mono<List<Order>> ordersMono = orderService.get("/orders", new TypeReference<List<Order>>() {})
        .withQueryParam("userId", userId)
        .withQueryParam("limit", 10)
        .execute();
    
    Mono<Preferences> preferencesMono = preferencesService.get("/preferences/{userId}", Preferences.class)
        .withPathParam("userId", userId)
        .execute()
        .onErrorReturn(Preferences.defaultPreferences()); // Graceful degradation
    
    Mono<List<Notification>> notificationsMono = notificationService.get("/notifications", new TypeReference<List<Notification>>() {})
        .withQueryParam("userId", userId)
        .withQueryParam("unread", true)
        .execute()
        .onErrorReturn(Collections.emptyList()); // Graceful degradation
    
    return Mono.zip(userMono, ordersMono, preferencesMono, notificationsMono)
        .map(tuple -> UserProfile.builder()
            .user(tuple.getT1())
            .recentOrders(tuple.getT2())
            .preferences(tuple.getT3())
            .notifications(tuple.getT4())
            .build());
}
```

## 🏗️ Architecture

The library follows a layered architecture designed for scalability and maintainability:

```
┌─────────────────────────────────────────────────────┐
│                Application Layer                     │
├─────────────────────────────────────────────────────┤
│              ServiceClient Interface                │
├─────────────────────────────────────────────────────┤
│   REST Client Impl    │    gRPC Client Impl        │
├─────────────────────────────────────────────────────┤
│          Circuit Breaker & Resilience Layer        │
├─────────────────────────────────────────────────────┤
│   WebClient (Reactor)  │  ManagedChannel (gRPC)    │
├─────────────────────────────────────────────────────┤
│              Network Transport Layer                │
└─────────────────────────────────────────────────────┘
```

### Key Components

- **ServiceClient Interface** (`com.firefly.common.client.ServiceClient`)
- **REST Implementation** (`com.firefly.common.client.impl.RestServiceClientImpl`)
- **gRPC Implementation** (`com.firefly.common.client.impl.GrpcServiceClientImpl`)
- **Circuit Breaker** (`com.firefly.common.resilience.CircuitBreakerManager`)
- **Configuration** (`com.firefly.common.config.ServiceClientProperties`)
- **Builder Pattern** (`com.firefly.common.client.builder.*`)

## 📚 Documentation

Comprehensive documentation is available in the `/docs` directory:

- **[📖 Overview](docs/OVERVIEW.md)** - Comprehensive overview of all components
- **[🚀 Quick Start Guide](docs/QUICKSTART.md)** - Get started quickly with examples
- **[🏗️ Architecture Guide](docs/ARCHITECTURE.md)** - Detailed architecture documentation
- **[⚙️ Configuration Reference](docs/CONFIGURATION.md)** - Complete configuration options
- **[🧪 Testing Guide](docs/TESTING.md)** - Testing strategies and utilities

## 🧪 Testing

### Unit Testing with MockWebServer

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    private MockWebServer mockWebServer;
    private ServiceClient serviceClient;
    private UserService userService;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        serviceClient = ServiceClient.rest("user-service")
            .baseUrl(mockWebServer.url("/").toString())
            .build();
            
        userService = new UserService(serviceClient);
    }
    
    @Test
    void shouldGetUser() throws Exception {
        // Given
        User expectedUser = new User("123", "John Doe", "john@example.com");
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expectedUser)));
        
        // When
        Mono<User> result = userService.getUser("123");
        
        // Then
        StepVerifier.create(result)
            .assertNext(user -> {
                assertThat(user.getId()).isEqualTo("123");
                assertThat(user.getName()).isEqualTo("John Doe");
                assertThat(user.getEmail()).isEqualTo("john@example.com");
            })
            .verifyComplete();
        
        // Verify request
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/users/123");
        assertThat(request.getMethod()).isEqualTo("GET");
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
```

### Integration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.service-client.circuit-breaker.minimum-number-of-calls=2",
    "firefly.service-client.rest.logging-enabled=true",
    "firefly.service-client.environment=TESTING"
})
class ServiceClientIntegrationTest {
    
    @Autowired
    private ServiceClient serviceClient;
    
    @Test
    void shouldPerformHealthCheck() {
        StepVerifier.create(serviceClient.healthCheck())
            .verifyComplete();
    }
    
    @Test
    void shouldHandleCircuitBreakerOpen() {
        // Test circuit breaker behavior
        assertThat(serviceClient.isReady()).isTrue();
        assertThat(serviceClient.getClientType()).isEqualTo(ClientType.REST);
    }
}
```

## 📊 Metrics and Observability

The library provides comprehensive metrics integration with Micrometer for production observability.

### Available Metrics

```java
// Request metrics
service.client.requests.success{service, client.type}      // Success counter
service.client.requests.failure{service, client.type}      // Failure counter
service.client.requests.duration{service, client.type}     // Request duration timer

// Circuit breaker metrics
service.client.circuit.breaker.state{service, client.type}                    // Current state gauge
service.client.circuit.breaker.transitions{service, from.state, to.state}     // State transitions

// Error metrics
service.client.errors{service, client.type, error.type}    // Error type tracking
```

### Metrics Configuration

```yaml
firefly:
  service-client:
    metrics:
      enabled: true                  # Enable metrics collection (default: true)
      detailed-metrics: false        # Include detailed metrics (default: false)
      histogram-enabled: true        # Include histogram metrics (default: true)
```

Metrics are automatically exposed when `MeterRegistry` is available in your Spring context and can be visualized in Prometheus, Grafana, or any Micrometer-compatible monitoring system.

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Follow** existing code style and patterns
4. **Add** comprehensive tests for new features
5. **Update** documentation for any API changes
6. **Commit** your changes (`git commit -m 'Add amazing feature'`)
7. **Push** to the branch (`git push origin feature/amazing-feature`)
8. **Open** a Pull Request

### Development Guidelines

- Use reactive programming patterns consistently
- Ensure proper error handling and logging
- Add JavaDoc for public APIs
- Include integration tests for new features
- Follow Spring Boot best practices
- Maintain backward compatibility

## 📄 License

This library is developed and maintained by **Firefly Software Solutions Inc** and is released under the Apache License 2.0.

**Copyright © 2025 Firefly Software Solutions Inc**

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## 🏢 About Firefly Software Solutions Inc

**Firefly Software Solutions Inc** is a leading provider of enterprise-grade software solutions specializing in:

- 🏗️ **Microservice Architecture** - Building scalable, resilient distributed systems
- ⚡ **Reactive Programming** - High-performance, non-blocking applications
- 🛡️ **Enterprise Integration** - Secure, reliable service communication
- 📊 **Observability Solutions** - Comprehensive monitoring and analytics
- ☁️ **Cloud-Native Technologies** - Modern containerized and serverless applications

For more information about our products and services, visit [getfirefly.io](https://getfirefly.io)

---

**Built with ❤️ by the Firefly Software Solutions Team**

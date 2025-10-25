# Firefly Common Client Library

[![Maven Central](https://img.shields.io/badge/Maven-1.0.0--SNAPSHOT-blue)](https://maven.apache.org)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green)](https://spring.io/projects/spring-boot)
[![Reactive](https://img.shields.io/badge/Reactive-WebFlux-purple)](https://projectreactor.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A comprehensive, reactive service communication framework for microservice architectures developed by **Firefly Software Solutions Inc**. Provides unified REST, gRPC, and SOAP client interfaces with built-in resilience patterns, circuit breakers, and comprehensive observability.

> **Developed by [Firefly Software Solutions Inc](https://getfirefly.io)** - Building enterprise-grade solutions for modern microservice architectures.

## 🚀 Features

### Core Capabilities
- **🔗 Protocol-Specific Interfaces**: Natural APIs for REST (HTTP verbs), gRPC (streaming), and SOAP (operations)
- **⚡ Reactive Programming**: Non-blocking operations with Spring WebFlux and Project Reactor
- **🛡️ Circuit Breaker**: Advanced resilience patterns with automatic recovery
- **💊 Health Checks**: Built-in service health monitoring and diagnostics
- **📊 Observability**: Metrics, tracing, and logging integration
- **🔄 Full Streaming Support**: Server-Sent Events, gRPC unary/server/client/bidirectional streaming
- **🎯 Type Safety**: Protocol-specific types prevent misuse and provide compile-time validation
- **🌐 SOAP/WSDL Support**: Modern reactive API for legacy SOAP services with dynamic invocation

### Advanced Features
- **🏗️ Builder Pattern**: Fluent API for client configuration
- **🔌 Interceptors**: Extensible request/response processing pipeline
- **⚙️ Auto-Configuration**: Zero-config Spring Boot integration
- **🌍 Environment Profiles**: Development, testing, and production optimizations
- **🔐 Security**: Certificate pinning, mTLS, API key management, JWT validation, secrets encryption
- **🧪 Testing Support**: Comprehensive testing utilities, mocks, and WireMock integration
- **🎭 Chaos Engineering**: Fault injection for resilience testing (latency, errors, timeouts) ⭐ **NEW**
- **💾 HTTP Caching**: ETag-based validation, Cache-Control directives, TTL expiration ⭐ **NEW**
- **🔍 Service Discovery**: Kubernetes, Eureka, Consul, static configuration support ⭐ **NEW**
- **🔄 Request Deduplication**: Idempotency keys and request fingerprinting ⭐ **NEW**
- **⚖️ Load Balancing**: Round Robin, Weighted, Least Connections, Sticky Session, Zone-Aware ⭐ **NEW**
- **🪝 Webhook Client**: Event-driven integrations with signature verification ⭐ **NEW**
- **🔌 Plugin System**: Extensible SPI for custom functionality ⭐ **NEW**

### Enterprise Helpers
- **🔷 GraphQL Client**: Query caching, automatic retry, batch operations, Java Time API support
- **🔑 OAuth2 Client**: Multi-scope token caching, automatic refresh, retry with exponential backoff
- **📤 Multipart Upload**: Progress tracking, chunked uploads, parallel uploads, file validation
- **🔌 WebSocket Client**: Automatic reconnection, heartbeat, message queuing, binary support

### Observability & Security
- **📊 Performance Metrics**: Request tracking, latency monitoring, throughput analysis
- **🏥 Health Indicators**: Spring Boot Actuator integration, service health monitoring
- **🔒 Certificate Pinning**: SHA-256 hash validation to prevent MITM attacks
- **🔐 Rate Limiting**: Client-side rate limiting with Token Bucket, Fixed Window, Sliding Window strategies

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Installation](#-installation)
- [Basic Usage](#-basic-usage)
- [Enterprise Helpers](#-enterprise-helpers)
- [Security Features](#-security-features)
- [Observability](#-observability)
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
import com.firefly.common.client.RestClient;
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final RestClient userClient;

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
import com.firefly.common.client.RestClient;
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class UserService {

    private final RestClient userClient;

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
import com.firefly.common.client.GrpcClient;
import com.firefly.common.client.ServiceClient;
import com.example.grpc.PaymentServiceGrpc;
import com.example.grpc.PaymentServiceGrpc.PaymentServiceStub;

@Service
public class PaymentService {

    private final GrpcClient<PaymentServiceStub> paymentClient;

    public PaymentService() {
        this.paymentClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("localhost:9090")                                      // Required: gRPC service address
            .usePlaintext()                                                 // Optional: Use for development (no TLS)
            .timeout(Duration.ofSeconds(30))                                // Optional: Call timeout
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))   // Required: How to create the stub
            .build();
    }

    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        // Use native gRPC unary operation
        return paymentClient.unary(stub -> stub.processPayment(request));
    }
}
```

### Step 6b: Create Your First SOAP Client (Optional)

If you need to integrate with legacy SOAP/WSDL services, here's how to set up a SOAP client with a modern reactive API:

```java
import com.firefly.common.client.SoapClient;
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;

@Service
public class WeatherService {

    private final SoapClient weatherClient;

    public WeatherService() {
        this.weatherClient = ServiceClient.soap("weather-service")
            .wsdlUrl("http://www.webservicex.net/globalweather.asmx?WSDL")  // Required: WSDL URL
            .timeout(Duration.ofSeconds(30))                                  // Optional: Request timeout
            .build();
    }

    public Mono<WeatherResponse> getWeather(String city, String country) {
        WeatherRequest request = new WeatherRequest();
        request.setCityName(city);
        request.setCountryName(country);

        return weatherClient.invokeAsync("GetWeather", request, WeatherResponse.class);
    }
}
```

#### SOAP Client with Authentication

```java
SoapClient secureSOAPClient = ServiceClient.soap("payment-service")
    .wsdlUrl("https://secure.example.com/payment?wsdl")
    .credentials("api-user", "secret-password")          // WS-Security username/password
    .timeout(Duration.ofSeconds(45))
    .enableMtom()                                        // Enable for large binary transfers
    .header("X-API-Key", "your-api-key")                // Custom HTTP headers
    .build();
```

### Step 7: Common Configuration Patterns

#### Pattern 1: Service with Authentication

```java
RestClient authenticatedClient = ServiceClient.rest("secure-service")
    .baseUrl("https://api.example.com")
    .defaultHeader("Authorization", "Bearer your-token-here")
    .defaultHeader("X-API-Key", "your-api-key")
    .jsonContentType()
    .build();
```

#### Pattern 2: High-Performance Service

```java
RestClient highPerfClient = ServiceClient.rest("high-perf-service")
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
import com.firefly.common.client.GrpcClient;
import com.firefly.common.client.ServiceClient;
import com.example.grpc.PaymentServiceGrpc;
import com.example.grpc.PaymentServiceGrpc.PaymentServiceStub;

@Service
public class PaymentService {

    private final GrpcClient<PaymentServiceStub> grpcClient;

    public PaymentService() {
        this.grpcClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("payment-service:9090")
            .usePlaintext()
            .timeout(Duration.ofSeconds(30))
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
            .build();
    }

    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        // Use native gRPC unary operation
        return grpcClient.unary(stub -> stub.processPayment(request));
    }

    public Flux<TransactionEvent> streamTransactions(String accountId) {
        // Use native gRPC server streaming
        return grpcClient.serverStream(stub ->
            stub.streamTransactions(AccountRequest.newBuilder()
                .setAccountId(accountId)
                .build())
        );
    }
}
```

## 🚀 Enterprise Helpers

The library includes production-ready helpers for common integration patterns.

### GraphQL Client Helper

Enterprise-grade GraphQL client with caching, retry, and batch operations:

```java
import com.firefly.common.client.graphql.GraphQLClientHelper;
import com.firefly.common.client.graphql.GraphQLConfig;

@Service
public class GraphQLService {

    private final GraphQLClientHelper graphql;

    public GraphQLService() {
        GraphQLConfig config = GraphQLConfig.builder()
            .timeout(Duration.ofSeconds(30))
            .enableCache(true)
            .cacheExpiration(Duration.ofMinutes(5))
            .enableRetry(true)
            .maxRetries(3)
            .build();

        this.graphql = new GraphQLClientHelper(
            "https://api.example.com/graphql",
            config
        );
    }

    public User getUser(String userId) {
        String query = """
            query GetUser($id: ID!) {
                user(id: $id) {
                    id
                    name
                    email
                }
            }
            """;

        Map<String, Object> variables = Map.of("id", userId);
        return graphql.query(query, variables, User.class).block();
    }

    // Batch operations
    public Map<String, User> batchGetUsers(String... userIds) {
        String query = """
            query GetUsers($ids: [ID!]!) {
                users(ids: $ids) {
                    id
                    name
                    email
                }
            }
            """;

        Map<String, Object> variables = Map.of("ids", userIds);
        return graphql.batchQuery(query, variables, "users", User.class).block();
    }
}
```

**Features**: Query caching, automatic retry, batch operations, Java Time API support, smart error handling.

📖 **[Complete GraphQL Guide](docs/GRAPHQL_HELPER.md)**

### OAuth2 Client Helper

Production-ready OAuth2 client with multi-scope token caching and automatic refresh:

```java
import com.firefly.common.client.oauth2.OAuth2ClientHelper;
import com.firefly.common.client.oauth2.OAuth2Config;

@Service
public class OAuth2Service {

    private final OAuth2ClientHelper oauth2;

    public OAuth2Service() {
        OAuth2Config config = OAuth2Config.builder()
            .timeout(Duration.ofSeconds(30))
            .enableRetry(true)
            .maxRetries(3)
            .tokenExpirationBuffer(120) // Refresh 2 minutes before expiration
            .build();

        this.oauth2 = new OAuth2ClientHelper(
            "https://auth.example.com/oauth/token",
            "client-id",
            "client-secret",
            config
        );
    }

    public String getAccessToken() {
        return oauth2.getToken("read write").getAccessToken();
    }

    public String getIdToken() {
        return oauth2.getToken("openid profile email").getIdToken();
    }
}
```

**Features**: Multi-scope token caching, automatic refresh, retry with exponential backoff, ID token support.

📖 **[Complete OAuth2 Guide](docs/OAUTH2_HELPER.md)**

### Multipart Upload Helper

Enterprise file upload with progress tracking, chunked uploads, and parallel processing:

```java
import com.firefly.common.client.multipart.MultipartUploadHelper;
import com.firefly.common.client.multipart.MultipartConfig;

@Service
public class FileUploadService {

    private final MultipartUploadHelper upload;

    public FileUploadService() {
        MultipartConfig config = MultipartConfig.builder()
            .timeout(Duration.ofMinutes(5))
            .enableProgressTracking(true)
            .enableCompression(true)
            .chunkSize(5 * 1024 * 1024) // 5MB chunks
            .maxFileSize(100 * 1024 * 1024) // 100MB max
            .build();

        this.upload = new MultipartUploadHelper(
            "https://upload.example.com",
            config
        );
    }

    public UploadResponse uploadFile(File file) {
        return upload.uploadFile("/upload", file, UploadResponse.class, progress -> {
            System.out.println("Progress: " + progress.getPercentage() + "%");
        }).block();
    }

    // Chunked upload for large files
    public UploadResponse uploadLargeFile(File file) {
        return upload.uploadFileChunked("/upload/chunked", file, UploadResponse.class,
            progress -> log.info("Uploaded: {} MB", progress.getBytesUploaded() / 1024 / 1024)
        ).block();
    }

    // Parallel upload
    public List<UploadResponse> uploadMultipleFiles(List<File> files) {
        return upload.uploadFilesParallel("/upload", files, UploadResponse.class,
            progress -> log.info("Total progress: {}%", progress.getPercentage())
        ).block();
    }
}
```

**Features**: Progress tracking, chunked uploads, parallel uploads, file validation, compression, cancellation.

📖 **[Complete Multipart Upload Guide](docs/MULTIPART_HELPER.md)**

### WebSocket Client Helper

Production-ready WebSocket client with automatic reconnection and heartbeat:

```java
import com.firefly.common.client.websocket.WebSocketClientHelper;
import com.firefly.common.client.websocket.WebSocketConfig;

@Service
public class WebSocketService {

    private final WebSocketClientHelper ws;

    public WebSocketService() {
        WebSocketConfig config = WebSocketConfig.builder()
            .enableReconnection(true)
            .reconnectionDelay(Duration.ofSeconds(5))
            .maxReconnectionAttempts(10)
            .enableHeartbeat(true)
            .heartbeatInterval(Duration.ofSeconds(30))
            .enableMessageQueue(true)
            .build();

        this.ws = new WebSocketClientHelper(
            "wss://api.example.com/ws",
            config
        );
    }

    public void connect() {
        ws.connect(
            message -> log.info("Received: {}", message),
            error -> log.error("Error: {}", error.getMessage())
        );
    }

    public void sendMessage(String message) {
        ws.sendMessage(message);
    }
}
```

**Features**: Automatic reconnection, heartbeat/ping-pong, message queuing, binary messages, compression.

📖 **[Complete WebSocket Guide](docs/WEBSOCKET_HELPER.md)**

## 🔒 Security Features

Enterprise-grade security features for production deployments.

### Certificate Pinning

Prevent MITM attacks with SHA-256 certificate pinning:

```java
import com.firefly.common.client.security.CertificatePinningManager;

CertificatePinningManager pinning = CertificatePinningManager.builder()
    .addPin("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .addPin("api.example.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=") // Backup pin
    .strictMode(true)
    .build();

SSLContext sslContext = pinning.createSslContext();
```

### API Key Management

Secure API key management with rotation and expiration:

```java
import com.firefly.common.client.security.ApiKeyManager;

// Static API key
ApiKeyManager keyManager = ApiKeyManager.simple("service-name", "api-key-12345");

// Dynamic API key with rotation
ApiKeyManager keyManager = ApiKeyManager.builder()
    .serviceName("user-service")
    .apiKeySupplier(() -> vaultClient.getSecret("user-service-api-key"))
    .rotationInterval(Duration.ofHours(1))
    .autoRotate(true)
    .build();

String currentKey = keyManager.getCurrentApiKey();
```

### JWT Validation

Validate JWT tokens with signature verification and claims validation:

```java
import com.firefly.common.client.security.JwtValidator;

JwtValidator validator = JwtValidator.builder()
    .secret("your-secret-key")
    .issuer("https://auth.example.com")
    .audience("api.example.com")
    .validateExpiration(true)
    .validateSignature(true)
    .build();

JwtClaims claims = validator.validate(jwtToken);
String userId = claims.getSubject();
```

### Secrets Encryption

AES-256-GCM encryption for sensitive data:

```java
import com.firefly.common.client.security.SecretsEncryptionManager;

SecretsEncryptionManager encryption = SecretsEncryptionManager.builder()
    .masterKey("your-32-byte-master-key-here!!")
    .build();

// Encrypt/decrypt
String encrypted = encryption.encrypt("my-api-key-12345");
String decrypted = encryption.decrypt(encrypted);

// Store secrets
encryption.storeSecret("payment-api-key", "sk_live_12345");
String apiKey = encryption.getSecret("payment-api-key");
```

### Client-Side Rate Limiting

Prevent overwhelming downstream services:

```java
import com.firefly.common.client.security.ClientSideRateLimiter;

ClientSideRateLimiter rateLimiter = ClientSideRateLimiter.builder()
    .serviceName("payment-service")
    .maxRequestsPerSecond(10.0)
    .maxConcurrentRequests(50)
    .strategy(RateLimitStrategy.TOKEN_BUCKET)
    .build();

if (rateLimiter.tryAcquire()) {
    try {
        makeApiCall();
    } finally {
        rateLimiter.release();
    }
}
```

📖 **[Complete Security Guide](docs/SECURITY.md)**

## 📊 Observability

Comprehensive observability features for production monitoring.

### Performance Metrics

Track request performance with Micrometer integration:

```java
import com.firefly.common.client.metrics.PerformanceMetricsCollector;

PerformanceMetricsCollector metrics = new PerformanceMetricsCollector(meterRegistry);

// Record request
metrics.recordRequest("user-service", "GET", "/api/users", duration, 200, responseSize);

// Get statistics
RequestStats stats = metrics.getRequestStats("user-service");
log.info("Total requests: {}, Success rate: {}%",
    stats.getTotalRequests(),
    stats.getSuccessRate() * 100);
```

### Health Indicators

Spring Boot Actuator integration for health monitoring:

```java
import com.firefly.common.client.health.ServiceClientHealthIndicator;

// Automatically exposed via Spring Boot Actuator
// Access at: /actuator/health/serviceClient

@Component
public class CustomHealthIndicator extends ServiceClientHealthIndicator {

    public CustomHealthIndicator(List<ServiceClient> clients) {
        super(clients);
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        // Custom health check logic
        super.doHealthCheck(builder);
        builder.withDetail("custom-metric", "value");
    }
}
```

### Request/Response Logging

Advanced logging with sensitive data masking:

```java
import com.firefly.common.client.interceptor.RequestResponseLoggingInterceptor;

RequestResponseLoggingInterceptor logging = RequestResponseLoggingInterceptor.builder()
    .logLevel(LogLevel.FULL)
    .maskSensitiveHeaders(true)
    .sensitiveHeaders(List.of("Authorization", "X-API-Key"))
    .maxBodySize(1024)
    .build();
```

📖 **[Complete Observability Guide](docs/OBSERVABILITY.md)**

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

    # SOAP Configuration
    soap:
      default-timeout: 30s
      connection-timeout: 10s
      receive-timeout: 30s
      mtom-enabled: false                # Enable for large binary transfers
      schema-validation-enabled: true
      message-logging-enabled: false     # Enable for debugging
      max-message-size: 10485760         # 10MB
      ws-addressing-enabled: false
      ws-security-enabled: false
      soap-version: "1.1"                # 1.1 or 1.2
      wsdl-cache-enabled: true
      wsdl-cache-expiration: 1h
      follow-redirects: true

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
    public RestClient customerServiceClient() {
        return ServiceClient.rest("customer-service")
            .baseUrl("http://customer-service:8080")
            .timeout(Duration.ofSeconds(30))
            .defaultHeader("Authorization", "Bearer ${auth.token}")
            .jsonContentType()
            .maxConnections(50)
            .build();
    }
    
    @Bean
    public RestClient orderServiceClient() {
        return ServiceClient.rest("order-service")
            .baseUrl("https://order-service.example.com")
            .timeout(Duration.ofSeconds(45))
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    @Bean
    public GrpcClient<NotificationServiceStub> notificationServiceClient() {
        return ServiceClient.grpc("notification-service", NotificationServiceStub.class)
            .address("notification-service:9090")
            .usePlaintext()
            .timeout(Duration.ofSeconds(15))
            .stubFactory(channel -> NotificationServiceGrpc.newStub(channel))
            .build();
    }

    @Bean
    public SoapClient legacyPaymentServiceClient() {
        return ServiceClient.soap("legacy-payment-service")
            .wsdlUrl("http://legacy-payment.example.com/service?wsdl")
            .credentials("api-user", "secret-password")
            .timeout(Duration.ofSeconds(45))
            .enableMtom()
            .header("X-API-Key", "${payment.api.key}")
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

### Getting Started
- **[📖 Documentation Home](docs/README.md)** - Start here for complete documentation
- **[🚀 Quick Start Guide](docs/QUICK_START.md)** - Get started in 5 minutes
- **[🔄 Migration Guide](docs/MIGRATION_GUIDE.md)** - Upgrade from older versions

### Client Guides
- **[🔴 REST Client Guide](docs/REST_CLIENT.md)** - Complete guide for HTTP/REST services
- **[🟢 gRPC Client Guide](docs/GRPC_CLIENT.md)** - Complete guide for gRPC services
- **[🔵 SOAP Client Guide](docs/SOAP_CLIENT.md)** - Complete guide for SOAP/WSDL services

### Helper Guides
- **[🔷 GraphQL Helper](docs/GRAPHQL_HELPER.md)** - GraphQL client with caching and retry
- **[🔑 OAuth2 Helper](docs/OAUTH2_HELPER.md)** - OAuth2 token management
- **[📤 Multipart Upload Helper](docs/MULTIPART_HELPER.md)** - File upload with progress tracking
- **[🔌 WebSocket Helper](docs/WEBSOCKET_HELPER.md)** - WebSocket client with reconnection

### Advanced Topics
- **[⚙️ Configuration Reference](docs/CONFIGURATION.md)** - Complete configuration options
- **[🔒 Security Guide](docs/SECURITY.md)** - Certificate pinning, mTLS, JWT, encryption
- **[📊 Observability Guide](docs/OBSERVABILITY.md)** - Metrics, tracing, health checks
- **[🧪 Integration Testing](docs/INTEGRATION_TESTING.md)** - WireMock and test containers
- **[⭐ Advanced Features](docs/ADVANCED_FEATURES.md)** - Chaos engineering, caching, service discovery, plugins **NEW**

### Examples
- **[💡 Complete Example](examples/complete-example/)** - Full working example with all features

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

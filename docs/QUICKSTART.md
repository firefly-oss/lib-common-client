# Quick Start Guide - lib-common-client

## Installation

### Maven Dependency

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Auto-Configuration

The library auto-configures when detected on the classpath. Simply add `@SpringBootApplication`:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ ServiceClient components are automatically available
    }
}
```

## First Time Setup Guide

This section walks you through creating your first service client step-by-step.

### Understanding the Basics

Before creating a client, understand these key concepts:

1. **Client Type**: Choose REST for HTTP services or gRPC for Protocol Buffer services
2. **Builder Pattern**: Use fluent API to configure your client
3. **Reactive Programming**: All operations return `Mono<T>` or `Flux<T>` for non-blocking execution
4. **Circuit Breaker**: Automatically enabled for resilience

### Your First REST Client - Step by Step

#### Step 1: Create a Service Class

```java
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class UserService {

    private final ServiceClient userClient;

    // Constructor where we'll build our client
    public UserService() {
        // We'll add client configuration here
    }
}
```

#### Step 2: Build the Client with Required Properties

```java
public UserService() {
    this.userClient = ServiceClient.rest("user-service")  // Give your client a name
        .baseUrl("http://localhost:8080")                 // REQUIRED: Where is the service?
        .build();                                         // Build the client
}
```

**What each property means:**
- `rest("user-service")`: Creates a REST client named "user-service" (used for metrics and logging)
- `baseUrl("http://localhost:8080")`: **Required** - The base URL of your service
- `build()`: Finalizes and creates the client instance

#### Step 3: Add Optional Configuration

```java
public UserService() {
    this.userClient = ServiceClient.rest("user-service")
        .baseUrl("http://localhost:8080")
        .timeout(Duration.ofSeconds(30))                  // How long to wait for responses
        .jsonContentType()                                // Set Content-Type: application/json
        .defaultHeader("X-Client-Id", "my-app")          // Add custom headers to all requests
        .maxConnections(50)                               // Connection pool size
        .build();
}
```

**Optional properties explained:**
- `timeout()`: Maximum time to wait for a response (default: 30 seconds)
- `jsonContentType()`: Automatically sets `Content-Type: application/json` header
- `defaultHeader()`: Adds a header to every request made by this client
- `maxConnections()`: Size of the connection pool (default: 100)

#### Step 4: Make Your First Request

```java
public Mono<User> getUser(String userId) {
    return userClient.get("/users/{id}", User.class)     // GET request, expect User response
        .withPathParam("id", userId)                      // Replace {id} with actual userId
        .execute();                                       // Execute and return Mono<User>
}
```

**Request building explained:**
- `get("/users/{id}", User.class)`: HTTP GET to `/users/{id}`, deserialize response to `User`
- `withPathParam("id", userId)`: Replace `{id}` placeholder with actual value
- `execute()`: Send the request and return a reactive `Mono<User>`

#### Step 5: Handle the Response

```java
// In your controller or another service
public void example() {
    userService.getUser("123")
        .subscribe(user -> {
            System.out.println("Got user: " + user.getName());
        });

    // Or use it in a reactive chain
    userService.getUser("123")
        .map(User::getName)
        .flatMap(name -> doSomethingElse(name))
        .subscribe();
}
```

### Your First gRPC Client - Step by Step

#### Step 1: Ensure You Have gRPC Stubs

Make sure you've generated your gRPC stubs from `.proto` files. You should have:
- `PaymentServiceGrpc` class (generated)
- `PaymentServiceStub` class (generated)
- Request/Response message classes

#### Step 2: Create the gRPC Client

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
            .address("localhost:9090")                                      // REQUIRED: gRPC service address
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))   // REQUIRED: How to create stub
            .build();

        this.paymentClient = (GrpcServiceClientImpl<PaymentServiceStub>) client;
    }
}
```

**Required gRPC properties:**
- `grpc("payment-service", PaymentServiceStub.class)`: Create gRPC client with stub type
- `address("localhost:9090")`: **Required** - gRPC service host and port
- `stubFactory()`: **Required** - Function that creates your gRPC stub from a channel

#### Step 3: Add Optional gRPC Configuration

```java
public PaymentService() {
    ServiceClient client = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
        .address("localhost:9090")
        .usePlaintext()                                                 // Disable TLS (dev only!)
        .timeout(Duration.ofSeconds(30))                                // Call timeout
        .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
        .build();

    this.paymentClient = (GrpcServiceClientImpl<PaymentServiceStub>) client;
}
```

**Optional gRPC properties:**
- `usePlaintext()`: Disables TLS - **only use in development**
- `useTransportSecurity()`: Enables TLS - **use in production**
- `timeout()`: Maximum time for gRPC calls

### Configuration via application.yml

Instead of configuring each client programmatically, you can set global defaults:

#### Basic Configuration

```yaml
firefly:
  service-client:
    enabled: true                    # Enable the library
    default-timeout: 30s             # Default timeout for all clients
    environment: DEVELOPMENT         # DEVELOPMENT, TESTING, or PRODUCTION
```

#### REST Configuration

```yaml
firefly:
  service-client:
    rest:
      max-connections: 100           # Connection pool size
      response-timeout: 30s          # How long to wait for responses
      connect-timeout: 10s           # How long to wait to establish connection
      compression-enabled: true      # Enable gzip compression
      logging-enabled: true          # Log requests/responses (useful for debugging)
      default-content-type: "application/json"
      default-accept-type: "application/json"
```

**When to change these:**
- Increase `max-connections` for high-traffic services (e.g., 200-500)
- Increase `response-timeout` for slow services (e.g., 60s)
- Enable `logging-enabled` during development for debugging
- Disable `compression-enabled` if your service doesn't support it

#### gRPC Configuration

```yaml
firefly:
  service-client:
    grpc:
      keep-alive-time: 5m            # Send keep-alive ping every 5 minutes
      keep-alive-timeout: 30s        # Wait 30s for keep-alive response
      max-inbound-message-size: 4194304   # 4MB max message size
      call-timeout: 30s              # Default call timeout
      use-plaintext-by-default: true # Use plaintext (dev only!)
```

#### Circuit Breaker Configuration

The circuit breaker is automatically enabled. Customize it:

```yaml
firefly:
  service-client:
    circuit-breaker:
      enabled: true                  # Enable circuit breaker
      failure-rate-threshold: 50.0   # Open circuit after 50% failures
      minimum-number-of-calls: 5     # Need at least 5 calls before evaluating
      wait-duration-in-open-state: 60s  # Wait 60s before trying again
```

**Understanding circuit breaker:**
- When `failure-rate-threshold` is exceeded, the circuit "opens"
- When open, requests fail immediately without calling the service
- After `wait-duration-in-open-state`, it tries again (half-open state)
- If successful, circuit closes; if failed, stays open

### Property Reference Table

#### Essential Properties Quick Reference

| Property | Type | Required? | Default | Description |
|----------|------|-----------|---------|-------------|
| **REST Client** |
| `baseUrl()` | String | ✅ Yes | None | Base URL of the service |
| `timeout()` | Duration | No | 30s | Request timeout |
| `jsonContentType()` | - | No | - | Sets JSON content type |
| `maxConnections()` | int | No | 100 | Connection pool size |
| `defaultHeader()` | String, String | No | - | Add default header |
| **gRPC Client** |
| `address()` | String | ✅ Yes | None | Service host:port |
| `stubFactory()` | Function | ✅ Yes | None | Creates gRPC stub |
| `usePlaintext()` | - | No | false | Disable TLS |
| `timeout()` | Duration | No | 30s | Call timeout |

## Basic REST Client

### Simple GET Request

```java
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
public class UserService {
    
    private final ServiceClient userClient;
    
    public UserService() {
        this.userClient = ServiceClient.rest("user-service")
            .baseUrl("http://user-service:8080")
            .timeout(Duration.ofSeconds(30))
            .build();
    }
    
    public Mono<User> getUser(String userId) {
        return userClient.get("/users/{id}", User.class)
            .withPathParam("id", userId)
            .execute();
    }
}
```

### POST Request with Body

```java
public Mono<User> createUser(CreateUserRequest request) {
    return userClient.post("/users", User.class)
        .withBody(request)
        .withHeader("X-Request-ID", UUID.randomUUID().toString())
        .execute();
}
```

### Query Parameters and Headers

```java
public Mono<List<User>> searchUsers(String query, int limit) {
    return userClient.get("/users/search", new TypeReference<List<User>>() {})
        .withQueryParam("q", query)
        .withQueryParam("limit", limit)
        .withHeader("Accept", "application/json")
        .execute();
}
```

## Basic gRPC Client

### gRPC Client Setup

First, ensure you have your gRPC stubs generated:

```java
import com.firefly.common.client.ServiceClient;
import com.example.grpc.PaymentServiceGrpc;
import com.example.grpc.PaymentServiceGrpc.PaymentServiceStub;

@Service
public class PaymentService {
    
    private final ServiceClient paymentClient;
    
    public PaymentService() {
        this.paymentClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("payment-service:9090")
            .usePlaintext()
            .timeout(Duration.ofSeconds(30))
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
            .build();
    }
}
```

### Using gRPC Client

```java
import com.firefly.common.client.impl.GrpcServiceClientImpl;

public Mono<PaymentResponse> processPayment(PaymentRequest request) {
    GrpcServiceClientImpl<PaymentServiceStub> grpcClient = 
        (GrpcServiceClientImpl<PaymentServiceStub>) paymentClient;
    
    return grpcClient.executeWithCircuitBreaker(
        Mono.fromCallable(() -> {
            // Synchronous gRPC call wrapped in Mono
            return grpcClient.getStub().processPayment(request);
        })
    );
}
```

## Configuration

### Application Properties

Create `application.yml` with client configuration:

```yaml
firefly:
  service-client:
    enabled: true
    default-timeout: 30s
    environment: DEVELOPMENT
    
    # REST Configuration
    rest:
      max-connections: 100
      response-timeout: 30s
      connect-timeout: 10s
      read-timeout: 30s
      compression-enabled: true
      logging-enabled: true
      default-content-type: application/json
      default-accept-type: application/json
      follow-redirects: true
      max-in-memory-size: 1048576  # 1MB
      max-retries: 3
    
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
      wait-duration-in-open-state: 60s
      sliding-window-size: 10
      minimum-number-of-calls: 5
      slow-call-rate-threshold: 100.0
      slow-call-duration-threshold: 60s
      permitted-number-of-calls-in-half-open-state: 3
      max-wait-duration-in-half-open-state: 30s
      automatic-transition-from-open-to-half-open-enabled: true
    
    # Global Default Headers
    default-headers:
      User-Agent: "MyApp/1.0"
      X-Client-Version: "1.0.0"
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
            .build();
    }
    
    @Bean  
    public ServiceClient notificationServiceClient() {
        return ServiceClient.grpc("notification-service", NotificationServiceStub.class)
            .address("notification-service:9090")
            .usePlaintext()
            .stubFactory(channel -> NotificationServiceGrpc.newStub(channel))
            .build();
    }
}
```

## Advanced Features

### Circuit Breaker

Circuit breaker is automatically enabled. Monitor its state:

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
        return circuitBreakerManager.getMetrics(serviceName);
    }
}
```

### Custom Circuit Breaker Configuration

```java
@Configuration
public class CustomCircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerConfig highAvailabilityCircuitBreaker() {
        return CircuitBreakerConfig.builder()
            .failureRateThreshold(30.0)
            .minimumNumberOfCalls(3)
            .slidingWindowSize(5)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(2)
            .callTimeout(Duration.ofSeconds(5))
            .build();
    }
}
```

### Health Checks

```java
@Component
public class ServiceHealthChecker {
    
    private final ServiceClient serviceClient;
    
    public Mono<Boolean> checkServiceHealth() {
        return serviceClient.healthCheck()
            .map(v -> true)
            .onErrorReturn(false);
    }
    
    public boolean isServiceReady() {
        return serviceClient.isReady();
    }
}
```

### Streaming Responses

```java
public Flux<Event> streamEvents() {
    return serviceClient.stream("/events", Event.class)
        .doOnNext(event -> log.info("Received event: {}", event))
        .onErrorContinue((error, item) -> 
            log.warn("Error processing event: {}", error.getMessage()));
}
```

### Error Handling

```java
import com.firefly.common.client.exception.*;

public Mono<User> getUserWithErrorHandling(String userId) {
    return userClient.get("/users/{id}", User.class)
        .withPathParam("id", userId)
        .execute()
        .onErrorMap(ServiceNotFoundException.class, 
            ex -> new UserNotFoundException("User not found: " + userId))
        .onErrorMap(ServiceUnavailableException.class,
            ex -> new ServiceTemporarilyUnavailableException("User service unavailable"))
        .retry(3)
        .timeout(Duration.ofSeconds(10));
}
```

## Testing

### Unit Testing with MockWebServer

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    private MockWebServer mockWebServer;
    private ServiceClient serviceClient;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        serviceClient = ServiceClient.rest("user-service")
            .baseUrl(mockWebServer.url("/").toString())
            .build();
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
        Mono<User> result = serviceClient.get("/users/{id}", User.class)
            .withPathParam("id", "123")
            .execute();
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedUser)
            .verifyComplete();
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
    "firefly.service-client.rest.logging-enabled=true"
})
class ServiceClientIntegrationTest {
    
    @Autowired
    private ServiceClient serviceClient;
    
    @Test
    void shouldPerformHealthCheck() {
        StepVerifier.create(serviceClient.healthCheck())
            .verifyComplete();
    }
}
```

## Common Patterns

### Service Facade Pattern

```java
@Service
public class OrderServiceFacade {
    
    private final ServiceClient customerService;
    private final ServiceClient inventoryService;
    private final ServiceClient paymentService;
    
    public Mono<OrderSummary> createOrder(CreateOrderRequest request) {
        return validateCustomer(request.getCustomerId())
            .then(checkInventory(request.getItems()))
            .then(processPayment(request.getPayment()))
            .then(createOrderRecord(request))
            .map(this::toOrderSummary);
    }
    
    private Mono<Customer> validateCustomer(String customerId) {
        return customerService.get("/customers/{id}", Customer.class)
            .withPathParam("id", customerId)
            .execute();
    }
    
    private Mono<InventoryCheck> checkInventory(List<OrderItem> items) {
        return inventoryService.post("/inventory/check", InventoryCheck.class)
            .withBody(new InventoryCheckRequest(items))
            .execute();
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
        .execute();
    
    Mono<Preferences> preferencesMono = preferencesService.get("/preferences/{userId}", Preferences.class)
        .withPathParam("userId", userId)
        .execute();
    
    return Mono.zip(userMono, ordersMono, preferencesMono)
        .map(tuple -> new UserProfile(tuple.getT1(), tuple.getT2(), tuple.getT3()));
}
```

## Next Steps

1. **Read the [Overview](OVERVIEW.md)** for detailed architecture information
2. **Check the [Architecture Guide](ARCHITECTURE.md)** for implementation details  
3. **See the [Configuration Reference](CONFIGURATION.md)** for all available options
4. **Review the main [README](../README.md)** for comprehensive examples

## Troubleshooting

### Common Issues

**Circuit Breaker Opening Frequently**
- Check failure rate threshold in configuration
- Increase minimum number of calls
- Verify service stability

**Connection Pool Exhaustion**
- Increase `max-connections` setting
- Check for connection leaks
- Verify proper resource cleanup

**gRPC Connection Issues**
- Verify service address and port
- Check TLS/plaintext configuration
- Ensure stub factory is correctly configured

**Timeout Issues**
- Adjust timeout values in configuration
- Check network latency
- Consider using streaming for large responses
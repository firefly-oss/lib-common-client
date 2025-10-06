# Testing Guide

Comprehensive testing guide for the Firefly Common Client Library.

## Table of Contents

- [Overview](#overview)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [Testing REST Clients](#testing-rest-clients)
- [Testing gRPC Clients](#testing-grpc-clients)
- [Testing Circuit Breaker](#testing-circuit-breaker)
- [Testing with Testcontainers](#testing-with-testcontainers)
- [Best Practices](#best-practices)

## Overview

The library provides comprehensive testing support for both REST and gRPC clients. This guide covers:

- Unit testing with mocks
- Integration testing with real services
- Testing resilience patterns (circuit breaker, retry)
- Performance testing
- Contract testing

## Unit Testing

### Testing REST Clients with MockWebServer

Use OkHttp's `MockWebServer` for testing REST clients:

```java
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import com.firefly.common.client.ServiceClient;
import reactor.test.StepVerifier;

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
            .timeout(Duration.ofSeconds(5))
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
    
    @Test
    void shouldHandleNotFound() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("User not found"));
        
        // When
        Mono<User> result = userService.getUser("999");
        
        // Then
        StepVerifier.create(result)
            .expectError(ServiceNotFoundException.class)
            .verify();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
```

### Testing with Reactor Test

Use `StepVerifier` for testing reactive streams:

```java
import reactor.test.StepVerifier;
import java.time.Duration;

@Test
void shouldStreamEvents() {
    // Given
    Flux<Event> eventStream = serviceClient.stream("/events", Event.class);
    
    // When & Then
    StepVerifier.create(eventStream.take(3))
        .expectNextMatches(event -> event.getType().equals("START"))
        .expectNextMatches(event -> event.getType().equals("PROCESSING"))
        .expectNextMatches(event -> event.getType().equals("COMPLETE"))
        .verifyComplete();
}

@Test
void shouldHandleTimeout() {
    // Given
    Mono<User> slowRequest = serviceClient.get("/slow-endpoint", User.class)
        .withTimeout(Duration.ofMillis(100))
        .execute();
    
    // When & Then
    StepVerifier.create(slowRequest)
        .expectError(TimeoutException.class)
        .verify();
}
```

## Integration Testing

### Spring Boot Integration Tests

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
    
    @Autowired
    private CircuitBreakerManager circuitBreakerManager;
    
    @Test
    void shouldPerformHealthCheck() {
        StepVerifier.create(serviceClient.healthCheck())
            .verifyComplete();
    }
    
    @Test
    void shouldHaveCorrectConfiguration() {
        assertThat(serviceClient.isReady()).isTrue();
        assertThat(serviceClient.getClientType()).isEqualTo(ClientType.REST);
    }
    
    @Test
    void shouldHaveCircuitBreakerConfigured() {
        CircuitBreakerState state = circuitBreakerManager.getState("test-service");
        assertThat(state).isEqualTo(CircuitBreakerState.CLOSED);
    }
}
```

### Testing with Test Configuration

```java
@TestConfiguration
public class TestServiceClientConfig {
    
    @Bean
    @Primary
    public ServiceClient testServiceClient() {
        return ServiceClient.rest("test-service")
            .baseUrl("http://localhost:8080")
            .timeout(Duration.ofSeconds(5))
            .build();
    }
    
    @Bean
    public CircuitBreakerConfig testCircuitBreakerConfig() {
        return CircuitBreakerConfig.builder()
            .failureRateThreshold(50.0)
            .minimumNumberOfCalls(2)
            .slidingWindowSize(5)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .build();
    }
}
```

## Testing REST Clients

### Testing POST Requests

```java
@Test
void shouldCreateUser() throws Exception {
    // Given
    CreateUserRequest request = new CreateUserRequest("Jane Doe", "jane@example.com");
    User expectedUser = new User("456", "Jane Doe", "jane@example.com");
    
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(201)
        .setHeader("Content-Type", "application/json")
        .setBody(objectMapper.writeValueAsString(expectedUser)));
    
    // When
    Mono<User> result = userService.createUser(request);
    
    // Then
    StepVerifier.create(result)
        .assertNext(user -> {
            assertThat(user.getId()).isEqualTo("456");
            assertThat(user.getName()).isEqualTo("Jane Doe");
        })
        .verifyComplete();
    
    // Verify request body
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    assertThat(recordedRequest.getBody().readUtf8())
        .contains("Jane Doe")
        .contains("jane@example.com");
}
```

### Testing Query Parameters

```java
@Test
void shouldSearchUsersWithQueryParams() throws Exception {
    // Given
    List<User> users = List.of(
        new User("1", "John", "john@example.com"),
        new User("2", "Jane", "jane@example.com")
    );
    
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(objectMapper.writeValueAsString(users)));
    
    // When
    Flux<User> result = userService.searchUsers("john");
    
    // Then
    StepVerifier.create(result)
        .expectNextCount(2)
        .verifyComplete();
    
    // Verify query parameters
    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).contains("q=john");
}
```

## Testing gRPC Clients

### Testing with Mock gRPC Stubs

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PaymentServiceGrpc.PaymentServiceStub mockStub;
    
    @Mock
    private ManagedChannel mockChannel;
    
    private GrpcServiceClientImpl<PaymentServiceGrpc.PaymentServiceStub> grpcClient;
    
    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.defaultConfig();
        CircuitBreakerManager circuitBreakerManager = new CircuitBreakerManager(config);
        
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.isTerminated()).thenReturn(false);
        
        grpcClient = new GrpcServiceClientImpl<>(
            "payment-service",
            PaymentServiceGrpc.PaymentServiceStub.class,
            "localhost:9090",
            Duration.ofSeconds(30),
            mockChannel,
            mockStub,
            circuitBreakerManager
        );
    }
    
    @Test
    void shouldProcessPayment() {
        // Given
        PaymentRequest request = PaymentRequest.newBuilder()
            .setAmount(100.0)
            .setCurrency("USD")
            .build();
        
        // When
        Mono<PaymentResponse> result = grpcClient.executeWithCircuitBreaker(
            Mono.just(PaymentResponse.newBuilder().setSuccess(true).build())
        );
        
        // Then
        StepVerifier.create(result)
            .assertNext(response -> assertThat(response.getSuccess()).isTrue())
            .verifyComplete();
    }
}
```

## Testing Circuit Breaker

### Testing Circuit Breaker State Transitions

```java
@Test
void shouldOpenCircuitBreakerAfterFailures() {
    // Given
    CircuitBreakerConfig config = CircuitBreakerConfig.builder()
        .failureRateThreshold(50.0)
        .minimumNumberOfCalls(3)
        .slidingWindowSize(5)
        .waitDurationInOpenState(Duration.ofMillis(100))
        .build();
    
    CircuitBreakerManager manager = new CircuitBreakerManager(config);
    String serviceName = "test-service";
    
    // When - Execute failing calls
    for (int i = 0; i < 3; i++) {
        StepVerifier.create(
            manager.executeWithCircuitBreaker(serviceName,
                () -> Mono.error(new RuntimeException("Failure")))
        ).expectError(RuntimeException.class).verify();
    }
    
    // Then - Circuit should be OPEN
    CircuitBreakerState state = manager.getState(serviceName);
    assertThat(state).isEqualTo(CircuitBreakerState.OPEN);
    
    // And - Next call should be rejected
    StepVerifier.create(
        manager.executeWithCircuitBreaker(serviceName,
            () -> Mono.just("Should not execute"))
    ).expectError(CircuitBreakerOpenException.class).verify();
}
```

### Testing Circuit Breaker Recovery

```java
@Test
void shouldRecoverFromOpenState() throws InterruptedException {
    // Given - Circuit is OPEN
    // ... (open the circuit as in previous test)
    
    // When - Wait for transition to HALF_OPEN
    Thread.sleep(150); // Wait longer than waitDurationInOpenState
    
    // Then - Circuit should allow test calls
    StepVerifier.create(
        manager.executeWithCircuitBreaker(serviceName,
            () -> Mono.just("RECOVERY"))
    )
    .expectNext("RECOVERY")
    .verifyComplete();
    
    // And - Circuit should transition to CLOSED after successful calls
    CircuitBreakerState finalState = manager.getState(serviceName);
    assertThat(finalState).isIn(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED);
}
```

## Testing with Testcontainers

### Integration Testing with Real Services

```java
@Testcontainers
@SpringBootTest
class ServiceClientContainerTest {
    
    @Container
    static GenericContainer<?> mockServer = new GenericContainer<>("mockserver/mockserver:latest")
        .withExposedPorts(1080);
    
    private ServiceClient serviceClient;
    
    @BeforeEach
    void setUp() {
        String baseUrl = String.format("http://%s:%d",
            mockServer.getHost(),
            mockServer.getMappedPort(1080));
        
        serviceClient = ServiceClient.rest("test-service")
            .baseUrl(baseUrl)
            .build();
    }
    
    @Test
    void shouldConnectToRealService() {
        // Test with real containerized service
        StepVerifier.create(serviceClient.healthCheck())
            .verifyComplete();
    }
}
```

## Best Practices

### 1. Use StepVerifier for Reactive Tests

Always use `StepVerifier` for testing `Mono` and `Flux`:

```java
StepVerifier.create(mono)
    .expectNext(expectedValue)
    .verifyComplete();
```

### 2. Test Error Scenarios

Test both success and failure paths:

```java
@Test
void shouldHandleServiceUnavailable() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(503));
    
    StepVerifier.create(serviceClient.get("/endpoint", String.class).execute())
        .expectError(ServiceUnavailableException.class)
        .verify();
}
```

### 3. Verify Request Details

Always verify that requests are constructed correctly:

```java
RecordedRequest request = mockWebServer.takeRequest();
assertThat(request.getHeader("Authorization")).isEqualTo("Bearer token");
assertThat(request.getPath()).isEqualTo("/expected/path");
```

### 4. Test Timeouts

Test timeout behavior explicitly:

```java
@Test
void shouldTimeoutSlowRequests() {
    mockWebServer.enqueue(new MockResponse()
        .setBodyDelay(5, TimeUnit.SECONDS));
    
    Mono<String> result = serviceClient.get("/slow", String.class)
        .withTimeout(Duration.ofSeconds(1))
        .execute();
    
    StepVerifier.create(result)
        .expectError(TimeoutException.class)
        .verify();
}
```

### 5. Clean Up Resources

Always clean up resources in `@AfterEach`:

```java
@AfterEach
void tearDown() throws IOException {
    if (mockWebServer != null) {
        mockWebServer.shutdown();
    }
    if (serviceClient != null) {
        serviceClient.shutdown();
    }
}
```

### 6. Use Test Profiles

Create separate test configurations:

```yaml
# application-test.yml
firefly:
  service-client:
    environment: TESTING
    rest:
      logging-enabled: true
      max-connections: 10
    circuit-breaker:
      minimum-number-of-calls: 2
      wait-duration-in-open-state: 100ms
```

### 7. Test Parallel Operations

Test concurrent request handling:

```java
@Test
void shouldHandleParallelRequests() {
    // Given
    for (int i = 0; i < 10; i++) {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("Response " + i));
    }
    
    // When
    Flux<String> results = Flux.range(0, 10)
        .flatMap(i -> serviceClient.get("/endpoint", String.class).execute());
    
    // Then
    StepVerifier.create(results)
        .expectNextCount(10)
        .verifyComplete();
}
```

---

**Copyright © 2025 Firefly Software Solutions Inc**


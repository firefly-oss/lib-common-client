package com.firefly.common.client.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for HttpErrorMapper.
 */
@DisplayName("HttpErrorMapper Tests")
class HttpErrorMapperTest {

    private ClientResponse mockResponse;
    private String serviceName;
    private String endpoint;
    private String method;
    private String requestId;
    private Instant startTime;

    @BeforeEach
    void setUp() {
        mockResponse = mock(ClientResponse.class);
        serviceName = "test-service";
        endpoint = "/api/test";
        method = "GET";
        requestId = "req-123";
        startTime = Instant.now().minusMillis(100);
    }

    @Test
    @DisplayName("Should map 400 Bad Request to ServiceValidationException")
    void shouldMap400ToServiceValidationException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Invalid input\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceValidationException.class);
                ServiceValidationException exception = (ServiceValidationException) throwable;
                assertThat(exception.getErrorContext().getServiceName()).isEqualTo(serviceName);
                assertThat(exception.getErrorContext().getEndpoint()).isEqualTo(endpoint);
                assertThat(exception.getErrorContext().getHttpStatusCode()).isEqualTo(400);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 401 Unauthorized to ServiceAuthenticationException")
    void shouldMap401ToServiceAuthenticationException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Unauthorized\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceAuthenticationException.class);
                assertThat(throwable.getMessage()).contains("Unauthorized");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 403 Forbidden to ServiceAuthenticationException")
    void shouldMap403ToServiceAuthenticationException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.FORBIDDEN);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Forbidden\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceAuthenticationException.class);
                assertThat(throwable.getMessage()).contains("Forbidden");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 404 Not Found to ServiceNotFoundException")
    void shouldMap404ToServiceNotFoundException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Not found\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceNotFoundException.class);
                assertThat(throwable.getMessage()).contains("Not found");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 408 Request Timeout to ServiceTimeoutException")
    void shouldMap408ToServiceTimeoutException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.REQUEST_TIMEOUT);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Timeout\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceTimeoutException.class);
                assertThat(throwable).isInstanceOf(RetryableError.class);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 409 Conflict to ServiceConflictException")
    void shouldMap409ToServiceConflictException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.CONFLICT);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Conflict\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceConflictException.class);
                assertThat(throwable).isNotInstanceOf(RetryableError.class);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 422 Unprocessable Entity to ServiceUnprocessableEntityException")
    void shouldMap422ToServiceUnprocessableEntityException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Validation failed\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceUnprocessableEntityException.class);
                assertThat(throwable).isInstanceOf(ServiceValidationException.class);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 429 Too Many Requests to ServiceRateLimitException")
    void shouldMap429ToServiceRateLimitException() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "60");
        ClientResponse.Headers responseHeaders = mock(ClientResponse.Headers.class);
        when(responseHeaders.asHttpHeaders()).thenReturn(headers);
        
        when(mockResponse.statusCode()).thenReturn(HttpStatus.TOO_MANY_REQUESTS);
        when(mockResponse.headers()).thenReturn(responseHeaders);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Rate limited\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceRateLimitException.class);
                assertThat(throwable).isInstanceOf(RetryableError.class);
                ServiceRateLimitException exception = (ServiceRateLimitException) throwable;
                assertThat(exception.getRetryAfterSeconds()).isEqualTo(60);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 500 Internal Server Error to ServiceInternalErrorException")
    void shouldMap500ToServiceInternalErrorException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Internal error\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceInternalErrorException.class);
                assertThat(throwable).isInstanceOf(RetryableError.class);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 502 Bad Gateway to ServiceTemporarilyUnavailableException")
    void shouldMap502ToServiceTemporarilyUnavailableException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Bad gateway\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceTemporarilyUnavailableException.class);
                assertThat(throwable).isInstanceOf(RetryableError.class);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 503 Service Unavailable to ServiceTemporarilyUnavailableException")
    void shouldMap503ToServiceTemporarilyUnavailableException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Service unavailable\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceTemporarilyUnavailableException.class);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should map 504 Gateway Timeout to ServiceTemporarilyUnavailableException")
    void shouldMap504ToServiceTemporarilyUnavailableException() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.GATEWAY_TIMEOUT);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Gateway timeout\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                assertThat(throwable).isInstanceOf(ServiceTemporarilyUnavailableException.class);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should include error context in mapped exceptions")
    void shouldIncludeErrorContextInMappedExceptions() {
        // Given
        when(mockResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(mockResponse.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("{\"error\":\"Not found\"}"));

        // When
        Mono<Throwable> result = HttpErrorMapper.mapHttpError(
            mockResponse, serviceName, endpoint, method, requestId, startTime
        );

        // Then
        StepVerifier.create(result)
            .assertNext(throwable -> {
                ServiceClientException exception = (ServiceClientException) throwable;
                ErrorContext context = exception.getErrorContext();
                
                assertThat(context.getServiceName()).isEqualTo(serviceName);
                assertThat(context.getEndpoint()).isEqualTo(endpoint);
                assertThat(context.getMethod()).isEqualTo(method);
                assertThat(context.getRequestId()).isEqualTo(requestId);
                assertThat(context.getHttpStatusCode()).isEqualTo(404);
                assertThat(context.hasElapsedTime()).isTrue();
            })
            .verifyComplete();
    }
}


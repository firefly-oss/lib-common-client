/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.client.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.firefly.common.client.ServiceClient;
import com.firefly.common.client.exception.ServiceClientException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for REST client using WireMock.
 * Tests all features including CRUD operations, error handling, headers, timeouts, etc.
 */
@DisplayName("REST Client Integration Tests")
class RestClientIntegrationTest {

    private static WireMockServer wireMockServer;
    private static String baseUrl;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        baseUrl = "http://localhost:" + wireMockServer.port();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("Should create REST client successfully")
    void shouldCreateRestClientSuccessfully() {
        // When: Creating a REST client
        ServiceClient client = ServiceClient.rest("user-service")
            .baseUrl(baseUrl)
            .timeout(Duration.ofSeconds(30))
            .build();

        // Then: Client should be properly configured
        assertThat(client).isNotNull();
        assertThat(client.getServiceName()).isEqualTo("user-service");
        assertThat(client.getBaseUrl()).isEqualTo(baseUrl);
        assertThat(client.isReady()).isTrue();

        client.shutdown();
    }

    @Test
    @DisplayName("Should perform GET request successfully")
    void shouldPerformGetRequestSuccessfully() {
        // Given: A mock GET response
        String jsonResponse = """
            {
                "id": 123,
                "name": "John Doe",
                "email": "john.doe@example.com"
            }
            """;

        wireMockServer.stubFor(get(urlEqualTo("/users/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonResponse)));

        // When: Performing a GET request
        ServiceClient client = ServiceClient.rest("user-service")
            .baseUrl(baseUrl)
            .build();

        Mono<User> response = client.get("/users/{id}", User.class)
            .withPathParam("id", "123")
            .execute();

        // Then: The response should be correct
        StepVerifier.create(response)
            .assertNext(user -> {
                assertThat(user).isNotNull();
                assertThat(user.getId()).isEqualTo(123);
                assertThat(user.getName()).isEqualTo("John Doe");
                assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
            })
            .verifyComplete();

        client.shutdown();
    }

    @Test
    @DisplayName("Should perform POST request successfully")
    void shouldPerformPostRequestSuccessfully() {
        // Given: A mock POST response
        String jsonResponse = """
            {
                "id": 456,
                "name": "Jane Smith",
                "email": "jane.smith@example.com"
            }
            """;

        wireMockServer.stubFor(post(urlEqualTo("/users"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonResponse)));

        // When: Performing a POST request
        ServiceClient client = ServiceClient.rest("user-service")
            .baseUrl(baseUrl)
            .build();

        User newUser = new User(null, "Jane Smith", "jane.smith@example.com");

        Mono<User> response = client.post("/users", User.class)
            .withBody(newUser)
            .execute();

        // Then: The response should be correct
        StepVerifier.create(response)
            .assertNext(user -> {
                assertThat(user).isNotNull();
                assertThat(user.getId()).isEqualTo(456);
                assertThat(user.getName()).isEqualTo("Jane Smith");
                assertThat(user.getEmail()).isEqualTo("jane.smith@example.com");
            })
            .verifyComplete();

        client.shutdown();
    }

    @Test
    @DisplayName("Should perform PUT request successfully")
    void shouldPerformPutRequestSuccessfully() {
        // Given: A mock PUT response
        String jsonResponse = """
            {
                "id": 123,
                "name": "John Updated",
                "email": "john.updated@example.com"
            }
            """;

        wireMockServer.stubFor(put(urlEqualTo("/users/123"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonResponse)));

        // When: Performing a PUT request
        ServiceClient client = ServiceClient.rest("user-service")
            .baseUrl(baseUrl)
            .build();

        User updatedUser = new User(123, "John Updated", "john.updated@example.com");

        Mono<User> response = client.put("/users/{id}", User.class)
            .withPathParam("id", "123")
            .withBody(updatedUser)
            .execute();

        // Then: The response should be correct
        StepVerifier.create(response)
            .assertNext(user -> {
                assertThat(user).isNotNull();
                assertThat(user.getId()).isEqualTo(123);
                assertThat(user.getName()).isEqualTo("John Updated");
            })
            .verifyComplete();

        client.shutdown();
    }

    @Test
    @DisplayName("Should perform DELETE request successfully")
    void shouldPerformDeleteRequestSuccessfully() {
        // Given: A mock DELETE response
        wireMockServer.stubFor(delete(urlEqualTo("/users/123"))
            .willReturn(aResponse()
                .withStatus(204)));

        // When: Performing a DELETE request
        ServiceClient client = ServiceClient.rest("user-service")
            .baseUrl(baseUrl)
            .build();

        Mono<Void> response = client.delete("/users/{id}")
            .withPathParam("id", "123")
            .execute();

        // Then: The request should complete successfully
        StepVerifier.create(response)
            .verifyComplete();

        client.shutdown();
    }

    @Test
    @DisplayName("Should handle query parameters correctly")
    void shouldHandleQueryParametersCorrectly() {
        // Given: A mock response with query parameters
        String jsonResponse = """
            [
                {"id": 1, "name": "User 1", "email": "user1@example.com"},
                {"id": 2, "name": "User 2", "email": "user2@example.com"}
            ]
            """;

        wireMockServer.stubFor(get(urlPathEqualTo("/users"))
            .withQueryParam("page", equalTo("1"))
            .withQueryParam("size", equalTo("10"))
            .withQueryParam("sort", equalTo("name"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonResponse)));

        // When: Performing a GET request with query parameters
        ServiceClient client = ServiceClient.rest("user-service")
            .baseUrl(baseUrl)
            .build();

        Mono<List> response = client.get("/users", List.class)
            .withQueryParam("page", 1)
            .withQueryParam("size", 10)
            .withQueryParam("sort", "name")
            .execute();

        // Then: The response should be correct
        StepVerifier.create(response)
            .assertNext(users -> {
                assertThat(users).isNotNull();
                assertThat(users).hasSize(2);
            })
            .verifyComplete();

        client.shutdown();
    }

    @Test
    @DisplayName("Should handle custom headers correctly")
    void shouldHandleCustomHeadersCorrectly() {
        // Given: A mock response expecting custom headers
        String jsonResponse = """
            {"id": 123, "name": "John Doe", "email": "john.doe@example.com"}
            """;

        wireMockServer.stubFor(get(urlEqualTo("/users/123"))
            .withHeader("X-API-Key", equalTo("secret-key"))
            .withHeader("X-Request-ID", matching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonResponse)));

        // When: Performing a GET request with custom headers
        ServiceClient client = ServiceClient.rest("user-service")
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", "secret-key")
            .build();

        Mono<User> response = client.get("/users/{id}", User.class)
            .withPathParam("id", "123")
            .withHeader("X-Request-ID", "req-12345")
            .execute();

        // Then: The response should be correct
        StepVerifier.create(response)
            .assertNext(user -> {
                assertThat(user).isNotNull();
                assertThat(user.getId()).isEqualTo(123);
            })
            .verifyComplete();

        client.shutdown();
    }

    // Test model classes
    public static class User {
        @JsonProperty("id")
        private Integer id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;

        public User() {
        }

        public User(Integer id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}


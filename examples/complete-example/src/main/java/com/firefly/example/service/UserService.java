package com.firefly.example.service;

import com.firefly.common.client.ServiceClient;
import com.firefly.common.client.rest.RestClient;
import com.firefly.example.model.User;
import com.firefly.example.model.CreateUserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Example service demonstrating REST client usage with all features:
 * - Circuit breaker
 * - Retry with exponential backoff
 * - Request/response logging
 * - Error handling
 */
@Slf4j
@Service
public class UserService {

    private final RestClient userClient;

    public UserService() {
        this.userClient = ServiceClient.rest("user-service")
            .baseUrl("https://jsonplaceholder.typicode.com")
            .timeout(Duration.ofSeconds(30))
            .build();
        
        log.info("UserService initialized with REST client");
    }

    /**
     * Get a user by ID.
     */
    public User getUser(Long userId) {
        log.info("Fetching user with ID: {}", userId);
        
        return userClient.get("/users/{id}")
            .pathVariable("id", userId)
            .retrieve(User.class)
            .block();
    }

    /**
     * Get all users.
     */
    public List<User> getAllUsers() {
        log.info("Fetching all users");
        
        return userClient.get("/users")
            .retrieveList(User.class)
            .block();
    }

    /**
     * Create a new user.
     */
    public User createUser(CreateUserRequest request) {
        log.info("Creating user: {}", request.getName());
        
        return userClient.post("/users")
            .body(request)
            .retrieve(User.class)
            .block();
    }

    /**
     * Update an existing user.
     */
    public User updateUser(Long userId, CreateUserRequest request) {
        log.info("Updating user with ID: {}", userId);
        
        return userClient.put("/users/{id}")
            .pathVariable("id", userId)
            .body(request)
            .retrieve(User.class)
            .block();
    }

    /**
     * Delete a user.
     */
    public void deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);
        
        userClient.delete("/users/{id}")
            .pathVariable("id", userId)
            .execute()
            .block();
    }

    /**
     * Search users with query parameters.
     */
    public List<User> searchUsers(String query) {
        log.info("Searching users with query: {}", query);
        
        return userClient.get("/users")
            .queryParam("q", query)
            .retrieveList(User.class)
            .block();
    }
}


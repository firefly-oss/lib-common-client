package com.firefly.example.service;

import com.firefly.common.client.graphql.GraphQLClientHelper;
import com.firefly.common.client.graphql.GraphQLConfig;
import com.firefly.example.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Example service demonstrating GraphQL client usage with:
 * - Query caching
 * - Automatic retry
 * - Error handling
 * - Variables support
 */
@Slf4j
@Service
public class GraphQLExampleService {

    private final GraphQLClientHelper graphqlClient;

    public GraphQLExampleService() {
        GraphQLConfig config = GraphQLConfig.builder()
            .timeout(Duration.ofSeconds(30))
            .enableCache(true)
            .cacheExpiration(Duration.ofMinutes(5))
            .enableRetry(true)
            .maxRetries(3)
            .build();

        this.graphqlClient = new GraphQLClientHelper(
            "https://api.example.com/graphql",
            config
        );
        
        log.info("GraphQLExampleService initialized");
    }

    /**
     * Query a user by ID using GraphQL.
     */
    public User getUser(String userId) {
        log.info("Fetching user via GraphQL with ID: {}", userId);
        
        String query = """
            query GetUser($id: ID!) {
                user(id: $id) {
                    id
                    name
                    email
                    username
                }
            }
            """;
        
        Map<String, Object> variables = Map.of("id", userId);
        
        return graphqlClient.query(query, variables, User.class).block();
    }

    /**
     * Create a user using GraphQL mutation.
     */
    public User createUser(String name, String email) {
        log.info("Creating user via GraphQL: {}", name);
        
        String mutation = """
            mutation CreateUser($name: String!, $email: String!) {
                createUser(input: {name: $name, email: $email}) {
                    id
                    name
                    email
                }
            }
            """;
        
        Map<String, Object> variables = Map.of(
            "name", name,
            "email", email
        );
        
        return graphqlClient.mutate(mutation, variables, User.class).block();
    }

    /**
     * Batch query multiple users.
     */
    public Map<String, User> batchGetUsers(String... userIds) {
        log.info("Batch fetching {} users via GraphQL", userIds.length);
        
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
        
        return graphqlClient.batchQuery(query, variables, "users", User.class).block();
    }
}


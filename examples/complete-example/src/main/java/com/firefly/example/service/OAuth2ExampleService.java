package com.firefly.example.service;

import com.firefly.common.client.oauth2.OAuth2ClientHelper;
import com.firefly.common.client.oauth2.OAuth2Config;
import com.firefly.common.client.oauth2.OAuth2Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Example service demonstrating OAuth2 client usage with:
 * - Multi-scope token caching
 * - Automatic token refresh
 * - Retry with exponential backoff
 * - ID token support
 */
@Slf4j
@Service
public class OAuth2ExampleService {

    private final OAuth2ClientHelper oauth2Client;

    public OAuth2ExampleService(
        @Value("${oauth2.token-endpoint:https://auth.example.com/oauth/token}") String tokenEndpoint,
        @Value("${oauth2.client-id:demo-client}") String clientId,
        @Value("${oauth2.client-secret:demo-secret}") String clientSecret
    ) {
        OAuth2Config config = OAuth2Config.builder()
            .timeout(Duration.ofSeconds(30))
            .enableRetry(true)
            .maxRetries(3)
            .retryBackoff(Duration.ofSeconds(1))
            .tokenExpirationBuffer(120) // Refresh 2 minutes before expiration
            .build();

        this.oauth2Client = new OAuth2ClientHelper(
            tokenEndpoint,
            clientId,
            clientSecret,
            config
        );
        
        log.info("OAuth2ExampleService initialized");
    }

    /**
     * Get an access token with default scope.
     */
    public String getAccessToken() {
        log.info("Getting OAuth2 access token");
        
        OAuth2Token token = oauth2Client.getToken();
        return token.getAccessToken();
    }

    /**
     * Get an access token with specific scopes.
     */
    public String getAccessTokenWithScopes(String... scopes) {
        log.info("Getting OAuth2 access token with scopes: {}", String.join(" ", scopes));
        
        OAuth2Token token = oauth2Client.getToken(String.join(" ", scopes));
        return token.getAccessToken();
    }

    /**
     * Get ID token (for OpenID Connect).
     */
    public String getIdToken() {
        log.info("Getting OAuth2 ID token");
        
        OAuth2Token token = oauth2Client.getToken("openid profile email");
        return token.getIdToken();
    }

    /**
     * Check if a valid token exists for a scope.
     */
    public boolean hasValidToken(String scope) {
        return oauth2Client.hasValidToken(scope);
    }

    /**
     * Clear token cache.
     */
    public void clearCache() {
        log.info("Clearing OAuth2 token cache");
        oauth2Client.clearCache();
    }

    /**
     * Get cache statistics.
     */
    public int getCacheSize() {
        return oauth2Client.getCacheSize();
    }
}


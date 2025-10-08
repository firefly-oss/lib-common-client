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

package com.firefly.common.client.builder;

import com.firefly.common.client.ServiceClient;
import com.firefly.common.client.exception.WsdlParsingException;
import com.firefly.common.client.impl.SoapServiceClientImpl;
import com.firefly.common.resilience.CircuitBreakerManager;
import lombok.extern.slf4j.Slf4j;

import javax.xml.namespace.QName;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for SOAP service clients with best-in-class developer experience.
 * 
 * <p>This builder simplifies SOAP client creation by providing:
 * <ul>
 *   <li>Automatic WSDL parsing and service discovery</li>
 *   <li>Fluent API for configuration</li>
 *   <li>Support for WS-Security and authentication</li>
 *   <li>MTOM/XOP attachment handling</li>
 *   <li>Reactive API over traditional SOAP</li>
 *   <li>Built-in resilience patterns</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple SOAP client from WSDL
 * ServiceClient client = ServiceClient.soap("weather-service")
 *     .wsdlUrl("http://www.webservicex.net/globalweather.asmx?WSDL")
 *     .build();
 *
 * // SOAP client with authentication
 * ServiceClient client = ServiceClient.soap("payment-service")
 *     .wsdlUrl("https://payment.example.com/service?wsdl")
 *     .username("api-user")
 *     .password("secret")
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // SOAP client with custom namespace and service
 * ServiceClient client = ServiceClient.soap("custom-service")
 *     .wsdlUrl("http://example.com/service?wsdl")
 *     .serviceName(new QName("http://example.com/", "CustomService"))
 *     .portName(new QName("http://example.com/", "CustomPort"))
 *     .build();
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class SoapClientBuilder {

    private final String serviceName;
    private String wsdlUrl;
    private QName serviceQName;
    private QName portQName;
    private Duration timeout = Duration.ofSeconds(30);
    private String username;
    private String password;
    private boolean mtomEnabled = false;
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, String> customHeaders = new HashMap<>();
    private CircuitBreakerManager circuitBreakerManager;
    private String endpointAddress;
    private boolean validateSchema = true;

    /**
     * Creates a new SOAP client builder.
     *
     * @param serviceName the logical service name for metrics and logging
     */
    public SoapClientBuilder(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        this.serviceName = serviceName.trim();
        log.debug("Created SOAP client builder for service '{}'", this.serviceName);
    }

    /**
     * Sets the WSDL URL for service discovery.
     * 
     * <p>The WSDL will be parsed to discover available operations, message formats,
     * and endpoint addresses. This is the primary way to configure a SOAP client.
     *
     * @param wsdlUrl the WSDL URL (can be HTTP, HTTPS, or file:// URL)
     * @return this builder
     */
    public SoapClientBuilder wsdlUrl(String wsdlUrl) {
        if (wsdlUrl == null || wsdlUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("WSDL URL cannot be null or empty");
        }
        this.wsdlUrl = wsdlUrl.trim();
        return this;
    }

    /**
     * Sets the service QName for WSDL service selection.
     * 
     * <p>Use this when the WSDL defines multiple services and you need to
     * specify which one to use. If not set, the first service in the WSDL
     * will be used.
     *
     * @param serviceQName the service qualified name
     * @return this builder
     */
    public SoapClientBuilder serviceName(QName serviceQName) {
        this.serviceQName = serviceQName;
        return this;
    }

    /**
     * Sets the port QName for WSDL port selection.
     * 
     * <p>Use this when the service defines multiple ports and you need to
     * specify which one to use. If not set, the first port will be used.
     *
     * @param portQName the port qualified name
     * @return this builder
     */
    public SoapClientBuilder portName(QName portQName) {
        this.portQName = portQName;
        return this;
    }

    /**
     * Sets the request timeout.
     *
     * @param timeout the timeout duration
     * @return this builder
     */
    public SoapClientBuilder timeout(Duration timeout) {
        if (timeout == null || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets the username for WS-Security authentication.
     *
     * @param username the username
     * @return this builder
     */
    public SoapClientBuilder username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the password for WS-Security authentication.
     *
     * @param password the password
     * @return this builder
     */
    public SoapClientBuilder password(String password) {
        this.password = password;
        return this;
    }

    /**
     * Sets both username and password for authentication.
     *
     * @param username the username
     * @param password the password
     * @return this builder
     */
    public SoapClientBuilder credentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    /**
     * Enables MTOM (Message Transmission Optimization Mechanism) for efficient
     * binary data transfer.
     *
     * @return this builder
     */
    public SoapClientBuilder enableMtom() {
        this.mtomEnabled = true;
        return this;
    }

    /**
     * Disables MTOM (enabled by default for large payloads).
     *
     * @return this builder
     */
    public SoapClientBuilder disableMtom() {
        this.mtomEnabled = false;
        return this;
    }

    /**
     * Adds a custom SOAP property.
     *
     * @param key the property key
     * @param value the property value
     * @return this builder
     */
    public SoapClientBuilder property(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    /**
     * Adds a custom HTTP header to all requests.
     *
     * @param name the header name
     * @param value the header value
     * @return this builder
     */
    public SoapClientBuilder header(String name, String value) {
        this.customHeaders.put(name, value);
        return this;
    }

    /**
     * Sets the circuit breaker manager for resilience.
     *
     * @param circuitBreakerManager the circuit breaker manager
     * @return this builder
     */
    public SoapClientBuilder circuitBreakerManager(CircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
        return this;
    }

    /**
     * Overrides the endpoint address from the WSDL.
     * 
     * <p>Use this to point to a different environment (dev, staging, prod)
     * while using the same WSDL.
     *
     * @param endpointAddress the endpoint address
     * @return this builder
     */
    public SoapClientBuilder endpointAddress(String endpointAddress) {
        this.endpointAddress = endpointAddress;
        return this;
    }

    /**
     * Enables XML schema validation for requests and responses.
     *
     * @return this builder
     */
    public SoapClientBuilder enableSchemaValidation() {
        this.validateSchema = true;
        return this;
    }

    /**
     * Disables XML schema validation (useful for development).
     *
     * @return this builder
     */
    public SoapClientBuilder disableSchemaValidation() {
        this.validateSchema = false;
        return this;
    }

    /**
     * Builds the SOAP service client.
     *
     * @return a configured SOAP service client
     * @throws WsdlParsingException if WSDL parsing fails
     */
    public ServiceClient build() {
        validateConfiguration();
        
        log.info("Building SOAP service client for service '{}' with WSDL '{}'", 
                serviceName, wsdlUrl);
        
        return new SoapServiceClientImpl(
            serviceName,
            wsdlUrl,
            serviceQName,
            portQName,
            timeout,
            username,
            password,
            mtomEnabled,
            properties,
            customHeaders,
            circuitBreakerManager,
            endpointAddress,
            validateSchema
        );
    }

    /**
     * Validates the builder configuration.
     */
    private void validateConfiguration() {
        if (wsdlUrl == null || wsdlUrl.isEmpty()) {
            throw new IllegalStateException("WSDL URL must be set");
        }
        
        // Validate WSDL URL format
        try {
            new URL(wsdlUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid WSDL URL: " + wsdlUrl, e);
        }
        
        log.debug("SOAP client configuration validated for service '{}'", serviceName);
    }
}


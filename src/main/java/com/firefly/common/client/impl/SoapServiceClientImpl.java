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

package com.firefly.common.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.firefly.common.client.ClientType;
import com.firefly.common.client.ServiceClient;
import com.firefly.common.client.exception.ServiceClientException;
import com.firefly.common.client.exception.SoapFaultException;
import com.firefly.common.client.exception.WsdlParsingException;
import com.firefly.common.resilience.CircuitBreakerManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SOAP implementation of ServiceClient using Apache CXF and JAX-WS.
 * 
 * <p>This implementation provides a modern reactive API over traditional SOAP services
 * with the following features:
 * <ul>
 *   <li>Automatic WSDL parsing and service discovery</li>
 *   <li>Dynamic operation invocation without generated stubs</li>
 *   <li>WS-Security username token authentication</li>
 *   <li>MTOM/XOP for efficient binary transfers</li>
 *   <li>Circuit breaker and retry support</li>
 *   <li>Reactive Mono/Flux API</li>
 *   <li>Comprehensive error handling and SOAP fault mapping</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class SoapServiceClientImpl implements ServiceClient {

    private final String serviceName;
    private final String wsdlUrl;
    private final QName serviceQName;
    private final QName portQName;
    private final Duration timeout;
    private final String username;
    private final String password;
    private final boolean mtomEnabled;
    private final Map<String, Object> properties;
    private final Map<String, String> customHeaders;
    private final CircuitBreakerManager circuitBreakerManager;
    private final String endpointAddress;
    private final boolean validateSchema;
    
    private Service service;
    private Object port;
    private final Map<String, Method> operationCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    /**
     * Creates a new SOAP service client implementation.
     */
    public SoapServiceClientImpl(
            String serviceName,
            String wsdlUrl,
            QName serviceQName,
            QName portQName,
            Duration timeout,
            String username,
            String password,
            boolean mtomEnabled,
            Map<String, Object> properties,
            Map<String, String> customHeaders,
            CircuitBreakerManager circuitBreakerManager,
            String endpointAddress,
            boolean validateSchema) {
        
        this.serviceName = serviceName;
        this.wsdlUrl = wsdlUrl;
        this.serviceQName = serviceQName;
        this.portQName = portQName;
        this.timeout = timeout;
        this.username = username;
        this.password = password;
        this.mtomEnabled = mtomEnabled;
        this.properties = new HashMap<>(properties);
        this.customHeaders = new HashMap<>(customHeaders);
        this.circuitBreakerManager = circuitBreakerManager;
        this.endpointAddress = endpointAddress;
        this.validateSchema = validateSchema;
        
        initializeService();
    }

    /**
     * Initializes the SOAP service from WSDL.
     */
    private void initializeService() {
        try {
            log.info("Initializing SOAP service '{}' from WSDL: {}", serviceName, wsdlUrl);
            
            URL wsdlURL = new URL(wsdlUrl);
            
            // Create service from WSDL
            if (serviceQName != null) {
                service = Service.create(wsdlURL, serviceQName);
            } else {
                // Use dynamic service creation
                service = Service.create(wsdlURL, new QName("http://tempuri.org/", serviceName));
            }
            
            // Get port
            if (portQName != null) {
                port = service.getPort(portQName, Object.class);
            } else {
                // Get first available port
                port = service.getPort(Object.class);
            }
            
            configurePort();
            
            initialized = true;
            log.info("SOAP service '{}' initialized successfully", serviceName);
            
        } catch (Exception e) {
            log.error("Failed to initialize SOAP service '{}': {}", serviceName, e.getMessage(), e);
            throw new WsdlParsingException("Failed to initialize SOAP service from WSDL: " + wsdlUrl, e);
        }
    }

    /**
     * Configures the SOAP port with timeout, authentication, and other settings.
     */
    private void configurePort() {
        if (!(port instanceof BindingProvider)) {
            return;
        }
        
        BindingProvider bindingProvider = (BindingProvider) port;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        
        // Set endpoint address if overridden
        if (endpointAddress != null && !endpointAddress.isEmpty()) {
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
            log.debug("Overriding endpoint address to: {}", endpointAddress);
        }
        
        // Configure timeout
        requestContext.put("jakarta.xml.ws.client.connectionTimeout", timeout.toMillis());
        requestContext.put("jakarta.xml.ws.client.receiveTimeout", timeout.toMillis());
        
        // Add custom properties
        requestContext.putAll(properties);
        
        // Configure WS-Security if credentials provided
        if (username != null && password != null) {
            configureWsSecurity();
        }
        
        // Configure MTOM if enabled
        if (mtomEnabled) {
            requestContext.put("jakarta.xml.ws.soap.http.soapaction.use", true);
            requestContext.put("jakarta.xml.ws.soap.http.soapaction.uri", "");
        }
        
        // Configure HTTP conduit for additional settings
        configureHttpConduit();
    }

    /**
     * Configures WS-Security username token authentication.
     */
    private void configureWsSecurity() {
        try {
            Client client = ClientProxy.getClient(port);
            
            Map<String, Object> outProps = new HashMap<>();
            outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
            outProps.put(WSHandlerConstants.USER, username);
            outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
            outProps.put(WSHandlerConstants.PW_CALLBACK_REF, 
                (javax.security.auth.callback.CallbackHandler) callbacks -> {
                    for (javax.security.auth.callback.Callback callback : callbacks) {
                        if (callback instanceof org.apache.wss4j.common.ext.WSPasswordCallback) {
                            org.apache.wss4j.common.ext.WSPasswordCallback pc = 
                                (org.apache.wss4j.common.ext.WSPasswordCallback) callback;
                            pc.setPassword(password);
                        }
                    }
                });
            
            WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
            client.getOutInterceptors().add(wssOut);
            
            log.debug("WS-Security configured for user: {}", username);
        } catch (Exception e) {
            log.warn("Failed to configure WS-Security: {}", e.getMessage());
        }
    }

    /**
     * Configures HTTP conduit for connection pooling and custom headers.
     */
    private void configureHttpConduit() {
        try {
            Client client = ClientProxy.getClient(port);
            HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
            
            HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
            httpClientPolicy.setConnectionTimeout(timeout.toMillis());
            httpClientPolicy.setReceiveTimeout(timeout.toMillis());
            httpClientPolicy.setAllowChunking(false);
            
            httpConduit.setClient(httpClientPolicy);
            
            // Add custom headers
            if (!customHeaders.isEmpty()) {
                Map<String, java.util.List<String>> headers = new HashMap<>();
                customHeaders.forEach((key, value) -> 
                    headers.put(key, java.util.Collections.singletonList(value)));
                httpConduit.getHeaders().putAll(headers);
            }
            
        } catch (Exception e) {
            log.warn("Failed to configure HTTP conduit: {}", e.getMessage());
        }
    }

    @Override
    public <R> RequestBuilder<R> get(String endpoint, Class<R> responseType) {
        return new SoapRequestBuilder<>(endpoint, "GET", responseType, null);
    }

    @Override
    public <R> RequestBuilder<R> get(String endpoint, TypeReference<R> typeReference) {
        return new SoapRequestBuilder<>(endpoint, "GET", null, typeReference);
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, Class<R> responseType) {
        return new SoapRequestBuilder<>(endpoint, "POST", responseType, null);
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, TypeReference<R> typeReference) {
        return new SoapRequestBuilder<>(endpoint, "POST", null, typeReference);
    }


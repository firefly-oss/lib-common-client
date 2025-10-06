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

package com.firefly.common.client.metrics;

import com.firefly.common.client.ClientType;
import com.firefly.common.resilience.CircuitBreakerState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer metrics integration for ServiceClient.
 * 
 * <p>This class provides comprehensive metrics collection for service client operations:
 * <ul>
 *   <li>Request counters (total, success, failure)</li>
 *   <li>Request duration timers</li>
 *   <li>Circuit breaker state gauges</li>
 *   <li>Error rate tracking</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class ServiceClientMetrics {

    private static final String METRIC_PREFIX = "service.client";
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, ServiceMetrics> serviceMetricsMap;
    
    /**
     * Creates a new ServiceClientMetrics instance.
     *
     * @param meterRegistry the Micrometer meter registry
     */
    public ServiceClientMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.serviceMetricsMap = new ConcurrentHashMap<>();
        log.info("Initialized ServiceClient metrics with Micrometer");
    }
    
    /**
     * Records a successful request.
     *
     * @param serviceName the service name
     * @param clientType the client type (REST or gRPC)
     * @param endpoint the endpoint path
     * @param duration the request duration
     */
    public void recordSuccess(String serviceName, ClientType clientType, String endpoint, Duration duration) {
        ServiceMetrics metrics = getOrCreateMetrics(serviceName, clientType);
        metrics.successCounter.increment();
        metrics.requestTimer.record(duration);
        
        log.debug("Recorded successful request for service '{}' to endpoint '{}' in {}ms", 
            serviceName, endpoint, duration.toMillis());
    }
    
    /**
     * Records a failed request.
     *
     * @param serviceName the service name
     * @param clientType the client type (REST or gRPC)
     * @param endpoint the endpoint path
     * @param duration the request duration
     * @param errorType the error type/class name
     */
    public void recordFailure(String serviceName, ClientType clientType, String endpoint, 
                             Duration duration, String errorType) {
        ServiceMetrics metrics = getOrCreateMetrics(serviceName, clientType);
        metrics.failureCounter.increment();
        
        // Record error-specific counter
        Counter errorCounter = Counter.builder(METRIC_PREFIX + ".errors")
            .tag("service", serviceName)
            .tag("client.type", clientType.name())
            .tag("error.type", errorType)
            .description("Total number of errors by type")
            .register(meterRegistry);
        errorCounter.increment();
        
        if (duration != null) {
            metrics.requestTimer.record(duration);
        }
        
        log.debug("Recorded failed request for service '{}' to endpoint '{}': {}", 
            serviceName, endpoint, errorType);
    }
    
    /**
     * Records circuit breaker state.
     *
     * @param serviceName the service name
     * @param state the circuit breaker state
     */
    public void recordCircuitBreakerState(String serviceName, CircuitBreakerState state) {
        ServiceMetrics metrics = serviceMetricsMap.get(serviceName);
        if (metrics != null) {
            metrics.circuitBreakerStateValue.set(state.ordinal());
            log.debug("Updated circuit breaker state for service '{}' to {}", serviceName, state);
        }
    }
    
    /**
     * Records a circuit breaker state transition.
     *
     * @param serviceName the service name
     * @param fromState the previous state
     * @param toState the new state
     */
    public void recordCircuitBreakerTransition(String serviceName, CircuitBreakerState fromState, 
                                              CircuitBreakerState toState) {
        Counter transitionCounter = Counter.builder(METRIC_PREFIX + ".circuit.breaker.transitions")
            .tag("service", serviceName)
            .tag("from.state", fromState.name())
            .tag("to.state", toState.name())
            .description("Circuit breaker state transitions")
            .register(meterRegistry);
        transitionCounter.increment();
        
        recordCircuitBreakerState(serviceName, toState);
        
        log.info("Circuit breaker transition for service '{}': {} -> {}", serviceName, fromState, toState);
    }
    
    /**
     * Gets or creates metrics for a service.
     */
    private ServiceMetrics getOrCreateMetrics(String serviceName, ClientType clientType) {
        return serviceMetricsMap.computeIfAbsent(serviceName, 
            name -> new ServiceMetrics(name, clientType, meterRegistry));
    }
    
    /**
     * Container for service-specific metrics.
     */
    private static class ServiceMetrics {
        private final Counter successCounter;
        private final Counter failureCounter;
        private final Timer requestTimer;
        private final AtomicInteger circuitBreakerStateValue;
        
        ServiceMetrics(String serviceName, ClientType clientType, MeterRegistry meterRegistry) {
            // Success counter
            this.successCounter = Counter.builder(METRIC_PREFIX + ".requests.success")
                .tag("service", serviceName)
                .tag("client.type", clientType.name())
                .description("Total number of successful requests")
                .register(meterRegistry);
            
            // Failure counter
            this.failureCounter = Counter.builder(METRIC_PREFIX + ".requests.failure")
                .tag("service", serviceName)
                .tag("client.type", clientType.name())
                .description("Total number of failed requests")
                .register(meterRegistry);
            
            // Request duration timer
            this.requestTimer = Timer.builder(METRIC_PREFIX + ".requests.duration")
                .tag("service", serviceName)
                .tag("client.type", clientType.name())
                .description("Request duration in milliseconds")
                .register(meterRegistry);
            
            // Circuit breaker state gauge
            this.circuitBreakerStateValue = new AtomicInteger(CircuitBreakerState.CLOSED.ordinal());
            Gauge.builder(METRIC_PREFIX + ".circuit.breaker.state", circuitBreakerStateValue, AtomicInteger::get)
                .tag("service", serviceName)
                .tag("client.type", clientType.name())
                .description("Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
                .register(meterRegistry);
        }
    }
    
    /**
     * Gets the total number of requests for a service.
     *
     * @param serviceName the service name
     * @return the total request count, or 0 if no metrics exist
     */
    public long getTotalRequests(String serviceName) {
        ServiceMetrics metrics = serviceMetricsMap.get(serviceName);
        if (metrics == null) {
            return 0;
        }
        return (long) (metrics.successCounter.count() + metrics.failureCounter.count());
    }
    
    /**
     * Gets the success rate for a service.
     *
     * @param serviceName the service name
     * @return the success rate (0.0 to 1.0), or 0.0 if no metrics exist
     */
    public double getSuccessRate(String serviceName) {
        ServiceMetrics metrics = serviceMetricsMap.get(serviceName);
        if (metrics == null) {
            return 0.0;
        }
        
        double total = metrics.successCounter.count() + metrics.failureCounter.count();
        if (total == 0) {
            return 0.0;
        }
        
        return metrics.successCounter.count() / total;
    }
    
    /**
     * Gets the current circuit breaker state for a service.
     *
     * @param serviceName the service name
     * @return the circuit breaker state, or CLOSED if no metrics exist
     */
    public CircuitBreakerState getCircuitBreakerState(String serviceName) {
        ServiceMetrics metrics = serviceMetricsMap.get(serviceName);
        if (metrics == null) {
            return CircuitBreakerState.CLOSED;
        }
        
        int stateValue = metrics.circuitBreakerStateValue.get();
        return CircuitBreakerState.values()[stateValue];
    }
}


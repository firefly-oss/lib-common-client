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

import io.micrometer.core.instrument.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Advanced performance metrics collector for ServiceClient.
 * 
 * <p>This collector provides comprehensive performance metrics including:
 * <ul>
 *   <li>Request throughput (requests per second)</li>
 *   <li>Response time percentiles (p50, p95, p99)</li>
 *   <li>Concurrent request tracking</li>
 *   <li>Payload size metrics</li>
 *   <li>Connection pool metrics</li>
 *   <li>Retry and timeout metrics</li>
 *   <li>Cache hit/miss rates</li>
 * </ul>
 *
 * <p>All metrics are exposed through Micrometer and can be exported to
 * Prometheus, Grafana, or any other monitoring system.
 *
 * <p>Example usage:
 * <pre>{@code
 * PerformanceMetricsCollector collector = new PerformanceMetricsCollector(meterRegistry);
 * 
 * // Record a request
 * collector.recordRequest("user-service", "GET", "/users/123", 
 *     Duration.ofMillis(150), 200, 1024);
 * 
 * // Get metrics summary
 * PerformanceMetricsSummary summary = collector.getSummary("user-service");
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class PerformanceMetricsCollector {

    private static final String METRIC_PREFIX = "service.client.performance";
    
    private final MeterRegistry meterRegistry;
    private final Map<String, ServicePerformanceMetrics> metricsMap;
    private final Instant startTime;

    /**
     * Creates a new PerformanceMetricsCollector.
     *
     * @param meterRegistry the Micrometer meter registry
     */
    public PerformanceMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.metricsMap = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
        log.info("Initialized PerformanceMetricsCollector");
    }

    /**
     * Records a completed request.
     *
     * @param serviceName the service name
     * @param method the HTTP method
     * @param endpoint the endpoint path
     * @param duration the request duration
     * @param statusCode the HTTP status code
     * @param responseSize the response size in bytes
     */
    public void recordRequest(String serviceName, String method, String endpoint,
                              Duration duration, int statusCode, long responseSize) {
        ServicePerformanceMetrics metrics = getOrCreateMetrics(serviceName);
        
        // Record request
        metrics.requestCounter.increment();
        metrics.requestTimer.record(duration);
        
        // Record response size
        metrics.responseSizeDistribution.record(responseSize);
        
        // Record by status code
        if (statusCode >= 200 && statusCode < 300) {
            metrics.successCounter.increment();
        } else if (statusCode >= 400 && statusCode < 500) {
            metrics.clientErrorCounter.increment();
        } else if (statusCode >= 500) {
            metrics.serverErrorCounter.increment();
        }
        
        // Update concurrent requests gauge
        metrics.lastRequestTime.set(System.currentTimeMillis());
        
        log.debug("Recorded request for service '{}': {} {} -> {} [{}ms, {}bytes]",
            serviceName, method, endpoint, statusCode, duration.toMillis(), responseSize);
    }

    /**
     * Records a request start (for tracking concurrent requests).
     *
     * @param serviceName the service name
     */
    public void recordRequestStart(String serviceName) {
        ServicePerformanceMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.concurrentRequests.incrementAndGet();
    }

    /**
     * Records a request end (for tracking concurrent requests).
     *
     * @param serviceName the service name
     */
    public void recordRequestEnd(String serviceName) {
        ServicePerformanceMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.concurrentRequests.decrementAndGet();
    }

    /**
     * Records a retry attempt.
     *
     * @param serviceName the service name
     * @param attemptNumber the retry attempt number
     */
    public void recordRetry(String serviceName, int attemptNumber) {
        ServicePerformanceMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.retryCounter.increment();
        
        log.debug("Recorded retry for service '{}': attempt {}", serviceName, attemptNumber);
    }

    /**
     * Records a timeout.
     *
     * @param serviceName the service name
     */
    public void recordTimeout(String serviceName) {
        ServicePerformanceMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.timeoutCounter.increment();
        
        log.debug("Recorded timeout for service '{}'", serviceName);
    }

    /**
     * Records a cache hit.
     *
     * @param serviceName the service name
     * @param cacheType the cache type (e.g., "query", "token")
     */
    public void recordCacheHit(String serviceName, String cacheType) {
        Counter.builder(METRIC_PREFIX + ".cache.hits")
            .tag("service", serviceName)
            .tag("cache.type", cacheType)
            .description("Cache hit count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records a cache miss.
     *
     * @param serviceName the service name
     * @param cacheType the cache type (e.g., "query", "token")
     */
    public void recordCacheMiss(String serviceName, String cacheType) {
        Counter.builder(METRIC_PREFIX + ".cache.misses")
            .tag("service", serviceName)
            .tag("cache.type", cacheType)
            .description("Cache miss count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records connection pool usage.
     *
     * @param serviceName the service name
     * @param activeConnections number of active connections
     * @param idleConnections number of idle connections
     * @param maxConnections maximum connections
     */
    public void recordConnectionPool(String serviceName, int activeConnections, 
                                    int idleConnections, int maxConnections) {
        Gauge.builder(METRIC_PREFIX + ".connection.pool.active", () -> activeConnections)
            .tag("service", serviceName)
            .description("Active connections in pool")
            .register(meterRegistry);
        
        Gauge.builder(METRIC_PREFIX + ".connection.pool.idle", () -> idleConnections)
            .tag("service", serviceName)
            .description("Idle connections in pool")
            .register(meterRegistry);
        
        Gauge.builder(METRIC_PREFIX + ".connection.pool.max", () -> maxConnections)
            .tag("service", serviceName)
            .description("Maximum connections in pool")
            .register(meterRegistry);
    }

    /**
     * Gets a performance metrics summary for a service.
     *
     * @param serviceName the service name
     * @return the metrics summary
     */
    public PerformanceMetricsSummary getSummary(String serviceName) {
        ServicePerformanceMetrics metrics = metricsMap.get(serviceName);
        if (metrics == null) {
            return null;
        }

        long totalRequests = (long) metrics.requestCounter.count();
        long successfulRequests = (long) metrics.successCounter.count();
        long clientErrors = (long) metrics.clientErrorCounter.count();
        long serverErrors = (long) metrics.serverErrorCounter.count();
        long retries = (long) metrics.retryCounter.count();
        long timeouts = (long) metrics.timeoutCounter.count();
        
        double avgDuration = metrics.requestTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
        double maxDuration = metrics.requestTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS);
        
        Duration uptime = Duration.between(startTime, Instant.now());
        double requestsPerSecond = totalRequests / (double) uptime.getSeconds();

        return PerformanceMetricsSummary.builder()
            .serviceName(serviceName)
            .totalRequests(totalRequests)
            .successfulRequests(successfulRequests)
            .clientErrors(clientErrors)
            .serverErrors(serverErrors)
            .retries(retries)
            .timeouts(timeouts)
            .averageDurationMs(avgDuration)
            .maxDurationMs(maxDuration)
            .requestsPerSecond(requestsPerSecond)
            .successRate(totalRequests > 0 ? (double) successfulRequests / totalRequests * 100.0 : 0.0)
            .errorRate(totalRequests > 0 ? (double) (clientErrors + serverErrors) / totalRequests * 100.0 : 0.0)
            .build();
    }

    /**
     * Gets or creates metrics for a service.
     */
    private ServicePerformanceMetrics getOrCreateMetrics(String serviceName) {
        return metricsMap.computeIfAbsent(serviceName, 
            name -> new ServicePerformanceMetrics(name, meterRegistry));
    }

    /**
     * Container for service-specific performance metrics.
     */
    private static class ServicePerformanceMetrics {
        private final Counter requestCounter;
        private final Counter successCounter;
        private final Counter clientErrorCounter;
        private final Counter serverErrorCounter;
        private final Counter retryCounter;
        private final Counter timeoutCounter;
        private final Timer requestTimer;
        private final DistributionSummary responseSizeDistribution;
        private final AtomicLong concurrentRequests;
        private final AtomicLong lastRequestTime;

        ServicePerformanceMetrics(String serviceName, MeterRegistry meterRegistry) {
            this.requestCounter = Counter.builder(METRIC_PREFIX + ".requests.total")
                .tag("service", serviceName)
                .description("Total number of requests")
                .register(meterRegistry);

            this.successCounter = Counter.builder(METRIC_PREFIX + ".requests.success")
                .tag("service", serviceName)
                .description("Number of successful requests (2xx)")
                .register(meterRegistry);

            this.clientErrorCounter = Counter.builder(METRIC_PREFIX + ".requests.client.errors")
                .tag("service", serviceName)
                .description("Number of client errors (4xx)")
                .register(meterRegistry);

            this.serverErrorCounter = Counter.builder(METRIC_PREFIX + ".requests.server.errors")
                .tag("service", serviceName)
                .description("Number of server errors (5xx)")
                .register(meterRegistry);

            this.retryCounter = Counter.builder(METRIC_PREFIX + ".retries")
                .tag("service", serviceName)
                .description("Number of retry attempts")
                .register(meterRegistry);

            this.timeoutCounter = Counter.builder(METRIC_PREFIX + ".timeouts")
                .tag("service", serviceName)
                .description("Number of timeouts")
                .register(meterRegistry);

            this.requestTimer = Timer.builder(METRIC_PREFIX + ".request.duration")
                .tag("service", serviceName)
                .description("Request duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

            this.responseSizeDistribution = DistributionSummary.builder(METRIC_PREFIX + ".response.size")
                .tag("service", serviceName)
                .description("Response size in bytes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

            this.concurrentRequests = new AtomicLong(0);
            Gauge.builder(METRIC_PREFIX + ".concurrent.requests", concurrentRequests, AtomicLong::get)
                .tag("service", serviceName)
                .description("Number of concurrent requests")
                .register(meterRegistry);

            this.lastRequestTime = new AtomicLong(System.currentTimeMillis());
            Gauge.builder(METRIC_PREFIX + ".last.request.time", lastRequestTime, AtomicLong::get)
                .tag("service", serviceName)
                .description("Timestamp of last request")
                .register(meterRegistry);
        }
    }

    /**
     * Performance metrics summary.
     */
    @Data
    @Builder
    public static class PerformanceMetricsSummary {
        private final String serviceName;
        private final long totalRequests;
        private final long successfulRequests;
        private final long clientErrors;
        private final long serverErrors;
        private final long retries;
        private final long timeouts;
        private final double averageDurationMs;
        private final double maxDurationMs;
        private final double requestsPerSecond;
        private final double successRate;
        private final double errorRate;
    }
}


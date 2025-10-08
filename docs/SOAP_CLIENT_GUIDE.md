# SOAP Client Guide

## Overview

The Firefly SOAP Client provides a modern, reactive API for consuming SOAP/WSDL web services with best-in-class developer experience. It wraps traditional JAX-WS and Apache CXF functionality in a fluent, type-safe interface that integrates seamlessly with the rest of the ServiceClient framework.

## Key Features

- **🚀 Modern Reactive API** - Mono/Flux-based reactive programming model
- **📝 Automatic WSDL Parsing** - No need to generate stub classes
- **🔒 WS-Security Support** - Built-in username token authentication
- **📦 MTOM/XOP Support** - Efficient binary data transfer
- **🔄 Circuit Breaker Integration** - Resilience patterns built-in
- **📊 Observability** - Metrics, logging, and tracing out of the box
- **⚡ Fluent Builder API** - Intuitive, discoverable configuration
- **🎯 Type Safety** - Strong typing with JAXB support

## Quick Start

### Basic SOAP Client

```java
import com.firefly.common.client.ServiceClient;
import reactor.core.publisher.Mono;

// Create a simple SOAP client
ServiceClient client = ServiceClient.soap("weather-service")
    .wsdlUrl("http://www.webservicex.net/globalweather.asmx?WSDL")
    .build();

// Invoke an operation
Mono<WeatherResponse> response = client.post("GetWeather", WeatherResponse.class)
    .withBody(weatherRequest)
    .execute();
```

### SOAP Client with Authentication

```java
ServiceClient client = ServiceClient.soap("secure-service")
    .wsdlUrl("https://secure.example.com/service?wsdl")
    .username("api-user")
    .password("secret-password")
    .timeout(Duration.ofSeconds(30))
    .build();
```

### SOAP Client with Custom Configuration

```java
import javax.xml.namespace.QName;

ServiceClient client = ServiceClient.soap("payment-service")
    .wsdlUrl("http://example.com/payment?wsdl")
    .serviceName(new QName("http://example.com/", "PaymentService"))
    .portName(new QName("http://example.com/", "PaymentPort"))
    .timeout(Duration.ofSeconds(45))
    .credentials("api-user", "secret")
    .enableMtom()
    .header("X-API-Key", "12345")
    .endpointAddress("https://prod.example.com/payment")
    .build();
```

## Configuration Options

### Builder Methods

| Method | Description | Default |
|--------|-------------|---------|
| `wsdlUrl(String)` | WSDL URL for service discovery | Required |
| `serviceName(QName)` | Service QName from WSDL | First service |
| `portName(QName)` | Port QName from WSDL | First port |
| `timeout(Duration)` | Request timeout | 30 seconds |
| `username(String)` | WS-Security username | None |
| `password(String)` | WS-Security password | None |
| `credentials(String, String)` | Set both username and password | None |
| `enableMtom()` | Enable MTOM for binary data | Disabled |
| `disableMtom()` | Disable MTOM | Disabled |
| `header(String, String)` | Add custom HTTP header | None |
| `property(String, Object)` | Add custom SOAP property | None |
| `endpointAddress(String)` | Override endpoint from WSDL | From WSDL |
| `enableSchemaValidation()` | Enable XML schema validation | Enabled |
| `disableSchemaValidation()` | Disable schema validation | Enabled |
| `circuitBreakerManager(...)` | Set circuit breaker manager | None |

### Application Properties

```yaml
firefly:
  service-client:
    soap:
      default-timeout: 30s
      connection-timeout: 10s
      receive-timeout: 30s
      mtom-enabled: false
      schema-validation-enabled: true
      message-logging-enabled: false
      max-message-size: 10485760  # 10MB
      ws-addressing-enabled: false
      ws-security-enabled: false
      soap-version: "1.1"
      wsdl-cache-enabled: true
      wsdl-cache-expiration: 1h
      follow-redirects: true
```

## Usage Examples

### Example 1: Simple Calculator Service

```java
@Service
public class CalculatorService {
    
    private final ServiceClient calculatorClient;
    
    public CalculatorService() {
        this.calculatorClient = ServiceClient.soap("calculator")
            .wsdlUrl("http://www.dneonline.com/calculator.asmx?WSDL")
            .build();
    }
    
    public Mono<Integer> add(int a, int b) {
        AddRequest request = new AddRequest();
        request.setIntA(a);
        request.setIntB(b);
        
        return calculatorClient.post("Add", AddResponse.class)
            .withBody(request)
            .execute()
            .map(AddResponse::getAddResult);
    }
}
```

### Example 2: Document Upload with MTOM

```java
@Service
public class DocumentService {
    
    private final ServiceClient documentClient;
    
    public DocumentService() {
        this.documentClient = ServiceClient.soap("document-service")
            .wsdlUrl("http://example.com/documents?wsdl")
            .enableMtom()  // Enable for efficient binary transfer
            .timeout(Duration.ofMinutes(2))
            .build();
    }
    
    public Mono<String> uploadDocument(String fileName, byte[] content) {
        UploadRequest request = new UploadRequest();
        request.setFileName(fileName);
        request.setContent(content);
        
        return documentClient.post("UploadDocument", UploadResponse.class)
            .withBody(request)
            .execute()
            .map(UploadResponse::getDocumentId);
    }
}
```

### Example 3: Secure Service with WS-Security

```java
@Configuration
public class SoapClientConfig {
    
    @Value("${payment.service.username}")
    private String username;
    
    @Value("${payment.service.password}")
    private String password;
    
    @Bean
    public ServiceClient paymentServiceClient() {
        return ServiceClient.soap("payment-service")
            .wsdlUrl("https://secure.example.com/payment?wsdl")
            .credentials(username, password)
            .timeout(Duration.ofSeconds(30))
            .header("X-API-Version", "2.0")
            .build();
    }
}
```

### Example 4: Multi-Environment Configuration

```java
@Configuration
@Profile("production")
public class ProductionSoapConfig {
    
    @Bean
    public ServiceClient orderServiceClient() {
        return ServiceClient.soap("order-service")
            .wsdlUrl("http://dev.example.com/orders?wsdl")  // Dev WSDL
            .endpointAddress("https://prod.example.com/orders")  // Prod endpoint
            .credentials("prod-user", "prod-password")
            .enableSchemaValidation()
            .build();
    }
}
```

### Example 5: With Circuit Breaker

```java
@Service
public class ResilientSoapService {
    
    private final ServiceClient soapClient;
    private final CircuitBreakerManager circuitBreakerManager;
    
    public ResilientSoapService(CircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
        this.soapClient = ServiceClient.soap("external-service")
            .wsdlUrl("http://external.example.com/service?wsdl")
            .circuitBreakerManager(circuitBreakerManager)
            .timeout(Duration.ofSeconds(10))
            .build();
    }
    
    public Mono<Response> callExternalService(Request request) {
        return soapClient.post("ProcessRequest", Response.class)
            .withBody(request)
            .execute()
            .doOnError(error -> log.error("Service call failed", error))
            .onErrorResume(error -> Mono.just(createFallbackResponse()));
    }
}
```

## Error Handling

### SOAP Fault Handling

```java
import com.firefly.common.client.exception.SoapFaultException;

soapClient.post("ValidateData", ValidationResponse.class)
    .withBody(request)
    .execute()
    .onErrorResume(error -> {
        if (error instanceof SoapFaultException) {
            SoapFaultException soapFault = (SoapFaultException) error;
            
            if (soapFault.isClientFault()) {
                log.warn("Client error: {}", soapFault.getFaultString());
                return Mono.error(new ValidationException(soapFault.getFaultString()));
            } else if (soapFault.isServerFault()) {
                log.error("Server error: {}", soapFault.getFaultString());
                return Mono.error(new ServiceUnavailableException());
            }
        }
        return Mono.error(error);
    });
```

### WSDL Parsing Errors

```java
import com.firefly.common.client.exception.WsdlParsingException;

try {
    ServiceClient client = ServiceClient.soap("service")
        .wsdlUrl("http://invalid-wsdl-url")
        .build();
} catch (WsdlParsingException e) {
    log.error("Failed to parse WSDL: {}", e.getMessage());
    // Handle WSDL parsing failure
}
```

## Best Practices

### 1. Reuse Client Instances

```java
// ✅ Good - Create once, reuse many times
@Bean
public ServiceClient weatherClient() {
    return ServiceClient.soap("weather")
        .wsdlUrl("http://example.com/weather?wsdl")
        .build();
}

// ❌ Bad - Creating new client for each request
public Mono<Weather> getWeather() {
    ServiceClient client = ServiceClient.soap("weather")
        .wsdlUrl("http://example.com/weather?wsdl")
        .build();
    // ...
}
```

### 2. Use Appropriate Timeouts

```java
// For quick operations
ServiceClient quickService = ServiceClient.soap("quick-service")
    .wsdlUrl("...")
    .timeout(Duration.ofSeconds(5))
    .build();

// For long-running operations
ServiceClient batchService = ServiceClient.soap("batch-service")
    .wsdlUrl("...")
    .timeout(Duration.ofMinutes(5))
    .build();
```

### 3. Enable MTOM for Large Payloads

```java
// Enable MTOM when transferring files or large binary data
ServiceClient fileService = ServiceClient.soap("file-service")
    .wsdlUrl("...")
    .enableMtom()
    .build();
```

### 4. Use Environment-Specific Configuration

```yaml
# application-dev.yml
firefly:
  service-client:
    soap:
      message-logging-enabled: true
      schema-validation-enabled: false

# application-prod.yml
firefly:
  service-client:
    soap:
      message-logging-enabled: false
      schema-validation-enabled: true
```

## Troubleshooting

### Common Issues

1. **WSDL Not Found**
   - Verify the WSDL URL is accessible
   - Check network connectivity and firewall rules
   - Ensure the service is running

2. **Authentication Failures**
   - Verify username and password are correct
   - Check if WS-Security is required by the service
   - Review service-specific authentication requirements

3. **Timeout Errors**
   - Increase timeout for slow services
   - Check network latency
   - Verify service is responding

4. **SOAP Faults**
   - Review fault details in exception
   - Check request payload validity
   - Verify operation name and parameters

## Migration from Traditional JAX-WS

### Before (Traditional JAX-WS)

```java
// Generate stub classes with wsimport
PaymentService service = new PaymentService();
PaymentPort port = service.getPaymentPort();

// Configure manually
((BindingProvider) port).getRequestContext()
    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

// Blocking call
PaymentResponse response = port.processPayment(request);
```

### After (Firefly SOAP Client)

```java
// No stub generation needed
ServiceClient client = ServiceClient.soap("payment-service")
    .wsdlUrl("http://example.com/payment?wsdl")
    .endpointAddress(endpoint)
    .build();

// Reactive call
Mono<PaymentResponse> response = client.post("ProcessPayment", PaymentResponse.class)
    .withBody(request)
    .execute();
```

## Next Steps

- Review [Configuration Guide](CONFIGURATION.md) for advanced settings
- Check [Architecture Guide](ARCHITECTURE.md) for implementation details
- See [Testing Guide](TESTING.md) for testing strategies


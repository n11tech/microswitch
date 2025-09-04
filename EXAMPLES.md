# Microswitch Examples

This document provides comprehensive examples of using Microswitch in real-world scenarios.

## Table of Contents

- [Basic Setup](#basic-setup)
- [E-Commerce Payment Processing](#e-commerce-payment-processing)
- [User Authentication System](#user-authentication-system)
- [Data Analytics Pipeline](#data-analytics-pipeline)
- [Microservice Communication](#microservice-communication)
- [Configuration Examples](#configuration-examples)

## Basic Setup

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.n11.development</groupId>
    <artifactId>microswitch</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Enable Auto-Configuration

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourpackage", "com.microswitch"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. Basic Configuration

```yaml
microswitch:
  enabled: true
  services:
    my-service:
      enabled: true
      canary:
        primary-percentage: 80
        algorithm: random
```

## E-Commerce Payment Processing

### Scenario: Testing New Payment Provider

```java
@Service
@Slf4j
public class PaymentService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    @Autowired
    private LegacyPaymentProvider legacyProvider;
    
    @Autowired
    private NewPaymentProvider newProvider;
    
    public PaymentResult processPayment(PaymentRequest request) {
        return deploymentManager.canary(
            () -> processWithLegacyProvider(request),
            () -> processWithNewProvider(request),
            "payment-processing"
        );
    }
    
    private PaymentResult processWithLegacyProvider(PaymentRequest request) {
        log.info("Processing payment with legacy provider: {}", request.getAmount());
        return legacyProvider.charge(request);
    }
    
    private PaymentResult processWithNewProvider(PaymentRequest request) {
        log.info("Processing payment with new provider: {}", request.getAmount());
        return newProvider.processPayment(request);
    }
}
```

### Configuration

```yaml
microswitch:
  services:
    payment-processing:
      enabled: true
      canary:
        primary-percentage: 95  # Start with 5% on new provider
        algorithm: sequence     # Predictable distribution
```

## User Authentication System

### Scenario: Migrating from LDAP to OAuth

```java
@Service
public class AuthenticationService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    @Autowired
    private LdapAuthenticator ldapAuth;
    
    @Autowired
    private OAuthAuthenticator oauthAuth;
    
    public AuthResult authenticate(String username, String password) {
        // Shadow deployment: LDAP result is returned, OAuth runs in parallel for comparison
        return deploymentManager.shadow(
            () -> authenticateWithLDAP(username, password),
            () -> authenticateWithOAuth(username, password),
            "user-authentication"
        );
    }
    
    private AuthResult authenticateWithLDAP(String username, String password) {
        return ldapAuth.authenticate(username, password);
    }
    
    private AuthResult authenticateWithOAuth(String username, String password) {
        return oauthAuth.authenticate(username, password);
    }
}
```

### Configuration

```yaml
microswitch:
  services:
    user-authentication:
      enabled: true
      shadow:
        weight: 5  # Run OAuth shadow every 5th authentication
```

## Data Analytics Pipeline

### Scenario: New Analytics Algorithm

```java
@Service
public class AnalyticsService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public AnalyticsReport generateUserReport(String userId) {
        return deploymentManager.blueGreen(
            () -> generateReportV1(userId),
            () -> generateReportV2(userId),
            "analytics-report"
        );
    }
    
    private AnalyticsReport generateReportV1(String userId) {
        // Legacy algorithm
        return legacyAnalytics.generateReport(userId);
    }
    
    private AnalyticsReport generateReportV2(String userId) {
        // New ML-based algorithm
        return mlAnalytics.generateReport(userId);
    }
}
```

### Configuration with TTL

```yaml
microswitch:
  services:
    analytics-report:
      enabled: true
      blue-green:
        primary-percentage: 50  # 50/50 split initially
        ttl: 7200              # After 2 hours, switch completely to V2
```

## Microservice Communication

### Scenario: API Version Migration

```java
@RestController
public class UserController {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    @Autowired
    private UserServiceV1 userServiceV1;
    
    @Autowired
    private UserServiceV2 userServiceV2;
    
    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable String id) {
        return deploymentManager.canary(
            () -> getUserV1(id),
            () -> getUserV2(id),
            "user-api"
        );
    }
    
    private UserResponse getUserV1(String id) {
        User user = userServiceV1.findById(id);
        return UserResponse.fromV1(user);
    }
    
    private UserResponse getUserV2(String id) {
        UserV2 user = userServiceV2.findById(id);
        return UserResponse.fromV2(user);
    }
}
```

## Configuration Examples

### Multi-Service Configuration

```yaml
microswitch:
  enabled: true
  services:
    # Critical service - conservative rollout
    payment-service:
      enabled: true
      canary:
        primary-percentage: 98
        algorithm: sequence
    
    # Analytics service - aggressive rollout
    analytics-service:
      enabled: true
      canary:
        primary-percentage: 70
        algorithm: random
    
    # Authentication - shadow testing
    auth-service:
      enabled: true
      shadow:
        weight: 10
    
    # Reporting - blue/green with TTL
    reporting-service:
      enabled: true
      blue-green:
        primary-percentage: 80
        ttl: 3600  # 1 hour
```

### Environment-Specific Configuration

#### Development
```yaml
microswitch:
  services:
    test-service:
      enabled: true
      shadow:
        weight: 1  # Every request for maximum testing
```

#### Staging
```yaml
microswitch:
  services:
    test-service:
      enabled: true
      canary:
        primary-percentage: 50
        algorithm: random
```

#### Production
```yaml
microswitch:
  services:
    test-service:
      enabled: true
      canary:
        primary-percentage: 95
        algorithm: sequence
```

## Advanced Patterns

### Error Handling with Fallback

```java
@Service
public class ResilientService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public Result processData(String data) {
        try {
            return deploymentManager.canary(
                () -> processV1(data),
                () -> processV2(data),
                "data-processing"
            );
        } catch (Exception e) {
            log.error("Both versions failed, using fallback", e);
            return fallbackProcessor.process(data);
        }
    }
}
```

### Conditional Deployment

```java
@Service
public class ConditionalService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    @Autowired
    private FeatureToggleService featureToggle;
    
    public String processRequest(Request request) {
        if (featureToggle.isEnabled("new-algorithm", request.getUserId())) {
            return deploymentManager.canary(
                () -> processOld(request),
                () -> processNew(request),
                "conditional-service"
            );
        }
        return processOld(request);
    }
}
```

### Metrics Monitoring

```java
@Component
public class DeploymentMonitor {
    
    @EventListener
    @Async
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Microswitch metrics available at /actuator/prometheus");
        log.info("Microswitch config available at /actuator/microswitch");
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkMetrics() {
        // Custom monitoring logic
        // Check error rates, performance metrics, etc.
    }
}
```

## Best Practices

### 1. Service Key Naming
```java
// ✅ Good
"user-registration"
"payment-processing"
"order-fulfillment"
"email-notification"

// ❌ Bad
"service1"
"test"
"temp"
"my-service"
```

### 2. Gradual Rollout Strategy
```yaml
# Week 1: Shadow testing
shadow:
  weight: 10

# Week 2: Small canary
canary:
  primary-percentage: 95
  algorithm: sequence

# Week 3: Larger canary
canary:
  primary-percentage: 80
  algorithm: random

# Week 4: Blue/green with TTL
blue-green:
  primary-percentage: 50
  ttl: 3600
```

### 3. Testing Strategy
```java
@Test
void testDeploymentStrategy() {
    // Test both paths
    String result1 = service.processData("test");
    String result2 = service.processData("test");
    
    // Verify metrics
    verify(deploymentMetrics, atLeastOnce())
        .recordSuccess(eq("service-key"), anyString(), eq("canary"));
}
```

## Troubleshooting

### Common Issues and Solutions

1. **Bean not found**
   ```java
   @ComponentScan(basePackages = {"com.yourpackage", "com.microswitch"})
   ```

2. **Metrics not appearing**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,microswitch
   ```

3. **Configuration not loading**
   - Check YAML syntax
   - Verify service keys match
   - Ensure microswitch.enabled=true

For more examples and patterns, see the [main documentation](README.md).

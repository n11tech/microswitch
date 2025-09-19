# 🚀 Microswitch — Deployment Strategy Library

Microswitch is a lightweight library for Java/Spring Boot that lets you apply deployment strategies programmatically: Canary, Shadow, and Blue/Green. It provides a single, minimal public API and hides all internals via JPMS.

This README explains how to add Microswitch to your project, configure it, and use it safely in production.

## Table of Contents

 - [Features](#features)
 - [Requirements](#requirements)
 - [Installation](#installation)
 - [Quick Start](#quick-start)
 - [Configuration](#configuration)
 - [Public API & Module Boundaries](#public-api--module-boundaries)
 - [JPMS Module-Info Usage](#jpms-module-info-usage)
 - [Metrics & Actuator](#metrics--actuator)
 - [Examples](#examples)
 - [Troubleshooting](#troubleshooting)
 - [Contributing & Governance](#contributing--governance)
 - [Security](#security)
 - [License (Apache-2.0)](#license-apache-2-0)

## Features

- Canary, Shadow, and Blue/Green strategies
- Lazy evaluation (only the selected Supplier executes)
- Optional Prometheus/Micrometer metrics
- YAML-based configuration
- Spring Boot auto-configuration
- Actuator endpoint for runtime visibility

## Requirements

- Java 21+
- Spring Boot 3.5.5+

## Installation

Maven

```xml
<dependency>
  <groupId>com.n11.development</groupId>
  <artifactId>microswitch</artifactId>
  <version>1.2.2</version>
  <scope>compile</scope>
</dependency>
```

Gradle

```gradle
implementation 'com.n11.development:microswitch:1.2.2'
```

## Quick Start

1) Configure application properties (application.yml)

```yaml
microswitch:
  enabled: true                       # master switch
  services:
    user-service:
      enabled: true
      activeStrategy: canary          # NEW in v1.1.0: configuration-driven strategy selection
      canary:
        percentage: 80/20             # stable/experimental split
        algorithm: SEQUENCE           # AlgorithmType enum (e.g., SEQUENCE, RANDOM)
      blueGreen:
        weight: 1/0                   # 1/0 → primary, 0/1 → secondary
        ttl: 60000                    # milliseconds
      shadow:
        stable: primary               # returns result from this path
        mirror: secondary             # mirrors this path when triggered
        mirrorPercentage: 10          # mirror every 10% of calls
        comparator:                   # NEW in v1.2.2: nested comparator tuning
          mode: disable               # enable/disable deep comparison
          maxCollectionElements: 10000
          maxCompareTimeMillis: 200
          enableSamplingOnHuge: true
          stride: 100
```

2) Inject and use `DeploymentManager`

```java
@Service
public class UserService {

  private final DeploymentManager deploymentManager;

  public UserService(DeploymentManager deploymentManager) {
    this.deploymentManager = deploymentManager;
  }

  public User createUser(String name) {
    // NEW in v1.1.0: Configuration-driven strategy selection
    return deploymentManager.execute(
        () -> createUserV1(name),  // stable
        () -> createUserV2(name),  // experimental
        "user-service"             // uses activeStrategy from config
    );
    
    // Legacy approach (still supported but deprecated)
    // return deploymentManager.canary(() -> createUserV1(name), () -> createUserV2(name), "user-service");
  }

  private User createUserV1(String name) { return new User(name, "v1"); }
  private User createUserV2(String name) { return new User(name, "v2"); }
}
```

## Deployment Strategies

### Canary
Route a percentage of traffic to the experimental version.

```java
// %80 stable, %20 experimental
String result = deploymentManager.canary(
    this::stableMethod,
    this::experimentalMethod,
    "service-key"
);
```

### Shadow
Execute the experimental path in the background, but always return the stable result to callers. Useful for validating parity and measuring performance.

```java
// Stable döner, experimental paralel çalışır
Integer result = deploymentManager.shadow(
    this::stableMethod,
    this::experimentalMethod,
    "service-key"
);
```

### Blue/Green
Choose between two versions using a binary weight selector and/or a TTL cutoff for full switchover.
The weight accepts only two forms:

- `1/0` → route to primary
- `0/1` → route to secondary

```java
// Primary percentage veya TTL tabanlı seçim
String result = deploymentManager.blueGreen(
    this::blueMethod,
    this::greenMethod,
    "service-key"
);
```

## Release Notes

### Version 1.2.2 (Latest)

#### New in 1.2.2 — Shadow Deep Comparator Tuning & WARN Visibility

1) Comparator tuning for large data structures (Shadow strategy)

```yaml
microswitch:
  services:
    order-service:
      shadow:
        comparator:
          mode: enable               # enable/disable deep comparison
          maxCollectionElements: 10000   # switch to sampling for very large lists
          maxCompareTimeMillis: 200      # time budget in ms; returns early when exceeded
          enableSamplingOnHuge: true     # enable sampling mode for huge lists
          stride: 100                    # sampling step for lists
```

- Large lists: head/tail + stride sampling prevents O(n) deep scans
- Time budget: early return if the total comparison time exceeds the configured budget

2) Operational visibility with WARN logs

- Sampling activated: `Deep comparison sampling activated for large list (size=...)`
- Time budget exceeded: `Deep comparison time budget exceeded (>X ms) at ... after Y ms; returning early`

These logs make performance-protection behavior observable in production without extra instrumentation.

3) Backward compatibility

- Legacy `shadow.comparator: enable|disable` still works and maps to `shadow.comparator.mode`
- New nested fields are optional and come with safe defaults

#### Improvements carried over from 1.1.2

- Enhanced constructor injection in auto-configuration
- Improved logging and configuration validation

#### Bug Fixes & Improvements

##### 1. Enhanced Constructor Injection
Improved dependency injection reliability by replacing field injection with constructor injection in `MicroswitchAutoConfiguration`:

```java
// Before: Field injection with @Autowired
@Autowired
private InitializerConfiguration properties;

// After: Constructor injection (more reliable)
public MicroswitchAutoConfiguration(InitializerConfiguration properties) {
    this.properties = properties;
}
```

**Benefits:**
- More reliable dependency injection
- Better testability and immutability
- Follows Spring Boot best practices
- Eliminates potential null pointer exceptions

##### 2. Improved Logging and Error Handling
Enhanced logging throughout the library for better debugging and monitoring:

- Added detailed initialization logging based on enabled/disabled state
- Improved error messages for configuration validation
- Enhanced debug logging for strategy execution paths
- Better exception handling with more descriptive error messages

##### 3. Configuration Validation Improvements
Strengthened configuration validation and error reporting:

- Better validation messages for invalid strategy configurations
- Improved handling of missing or malformed configuration sections
- Enhanced fallback mechanisms for edge cases

#### Benefits
- **Better Reliability**: Constructor injection eliminates potential initialization issues
- **Enhanced Debugging**: Improved logging helps identify configuration and execution issues
- **Stronger Validation**: Better error messages and validation for configuration problems
- **Maintainability**: Code follows Spring Boot best practices

### Version 1.1.2

#### New Features

##### 1. Execution Logging
Detailed execution logging to track service key, strategy, and method execution:

```yaml
microswitch:
  logger: enable  # Enable detailed execution logging (default: disable)
```

When enabled, logs include:
- `[MICROSWITCH-EXEC] Starting execution - Service: 'X', Strategy: 'Y'`
- `[MICROSWITCH-EXEC] Executing - Service: 'X', Strategy: 'Y', Method: 'stable/experimental'`
- `[MICROSWITCH-EXEC] Completed - Service: 'X', Strategy: 'Y', Method: 'Z' (Success)`
- `[MICROSWITCH-EXEC] Failed - Service: 'X', Strategy: 'Y', Method: 'Z' - Error: ...`

##### 2. Deep Object Comparison for Shadow Strategy
Configurable deep object comparison for shadow traffic validation:

```yaml
microswitch:
  services:
    order-service:
      shadow:
        comparator: enable  # Enable deep object comparison (default: disable)
```

When enabled:
- Compares objects by field values rather than reference equality
- Supports nested objects, collections, maps, and arrays
- Uses HYBRID strategy: checks for overridden equals() first, falls back to field comparison
- Automatically ignores common tracking fields (timestamp, requestId, traceId)

##### 3. Bean Availability When Disabled
Fixed dependency injection issues when microswitch is disabled:

```yaml
microswitch:
  enabled: false  # DeploymentManager bean is still available for injection
```

**Problem Solved**: Previously, when `microswitch.enabled=false`, the `DeploymentManager` bean was not created, causing Spring Boot applications to fail with:
```
Parameter 2 of constructor required a bean of type 'DeploymentManager' that could not be found
```

**Solution**: The `DeploymentManager` bean is now always created regardless of the `enabled` setting:
- When `enabled: true`: Full functionality with all strategies available
- When `enabled: false`: Bean is available but strategies are disabled internally
- Applications can safely inject `DeploymentManager` without startup failures
- Runtime logging clearly indicates the enabled/disabled state

#### Benefits
- **Better Observability**: Track exactly which service, strategy, and method are executing
- **Accurate Shadow Validation**: Compare actual object content, not just references
- **Reliable Dependency Injection**: No startup failures when microswitch is disabled
- **Zero Code Changes**: All features are configuration-driven
- **Performance Optimized**: No impact when disabled (default)

### Version 1.1.0

- **Configuration-Driven Deployment**: New `execute()` method for runtime strategy switching
- **Deprecated Legacy Methods**: `canary()`, `shadow()`, `blueGreen()` marked for removal in v2.0.0
- **Active Strategy Configuration**: Added `activeStrategy` field to service configuration

## Configuration-Driven Deployment (New in v1.1.0)

Starting with version 1.1.0, Microswitch supports configuration-driven strategy selection using the new `execute()` method. This allows you to change deployment strategies at runtime without code changes.

### Benefits

- **Runtime Strategy Switching**: Change strategies via configuration updates without redeployment
- **Centralized Management**: All strategy decisions in one place (application.yml)
- **Operational Flexibility**: Switch from canary to blue-green to shadow based on operational needs
- **A/B Testing**: Easy strategy comparison for the same service

### Usage

```java
// Configuration-driven approach (recommended)
String result = deploymentManager.execute(
    this::stableMethod,
    this::experimentalMethod,
    "service-key"  // Strategy determined by activeStrategy in config
);
```

### Configuration

```yaml
microswitch:
  services:
    payment-service:
      activeStrategy: canary    # Options: canary, shadow, blueGreen
      canary:
        percentage: 90/10
      shadow:
        mirrorPercentage: 20
      blueGreen:
        weight: 1/0
        ttl: 300000
```

### Migration from Legacy Methods

The legacy strategy-specific methods (`canary()`, `shadow()`, `blueGreen()`) are still supported but deprecated:

```java
// ❌ Deprecated (will be removed in v2.0.0)
deploymentManager.canary(stable, experimental, "service");

// ✅ Recommended (configuration-driven)
deploymentManager.execute(stable, experimental, "service");
```

## Configuration

### Full example

```yaml
microswitch:
  enabled: true
  services:
    payment-service:
      enabled: true
      canary:
        percentage: 90/10          # 90% stable, 10% canary
        algorithm: SEQUENCE        # AlgorithmType enum value
      blueGreen:
        weight: 1/0                # 1/0 → primary, 0/1 → secondary
        ttl: 7200000               # 2 hours in ms
      shadow:
        stable: primary
        mirror: secondary
        mirrorPercentage: 20       # mirror 20% of the time
        comparator:
          mode: disable            # deep object comparison (enable/disable)
          maxCollectionElements: 10000
          maxCompareTimeMillis: 200
          enableSamplingOnHuge: true
          stride: 100

    user-service:
      enabled: false               # disabled — only stable executes
```

### Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `microswitch.logger` | **NEW v1.1.1**: Enable/disable detailed execution logging | `disable` |
| `microswitch.enabled` | Master switch for the library | `true` |
| `services.<key>.enabled` | Whether the service key is active | `true` |
| `services.<key>.activeStrategy` | **NEW v1.1.0**: Active strategy for configuration-driven deployment (`canary`, `shadow`, `blueGreen`) | `canary` |
| `services.<key>.canary.percentage` | Stable/experimental split in slash format (e.g., `80/20`) or a single number meaning stable percentage | `100` |
| `services.<key>.canary.algorithm` | AlgorithmType enum value (e.g., `SEQUENCE`, `RANDOM`) | `SEQUENCE` |
| `services.<key>.blueGreen.weight` | Binary selector in slash format: `1/0` (primary) or `0/1` (secondary) | `1/0` |
| `services.<key>.blueGreen.ttl` | Time to live in milliseconds for route stickiness or switchover logic | `null` |
| `services.<key>.shadow.stable` | Which method is considered stable (`primary` or `secondary`) | `primary` |
| `services.<key>.shadow.mirror` | Which method is mirrored (`primary` or `secondary`) | `secondary` |
| `services.<key>.shadow.mirrorPercentage` | Percentage of calls that will trigger a mirror execution (0–100) | `0` |
| `services.<key>.shadow.comparator.mode` | **v1.2.2**: Enable/disable deep object comparison for shadow validation | `disable` |
| `services.<key>.shadow.comparator.maxCollectionElements` | **v1.2.2**: Threshold to activate sampling for large lists | `10000` |
| `services.<key>.shadow.comparator.maxCompareTimeMillis` | **v1.2.2**: Time budget for deep comparison (ms) | `200` |
| `services.<key>.shadow.comparator.enableSamplingOnHuge` | **v1.2.2**: Enable sampling mode for huge lists | `true` |
| `services.<key>.shadow.comparator.stride` | **v1.2.2**: Sampling step for list comparison | `100` |

## Metrics & Actuator

### Prometheus/Micrometer

If Micrometer is present and a `MeterRegistry` bean exists, Microswitch publishes counters per strategy/service/version automatically.

```
# HELP microswitch_success_total
# TYPE microswitch_success_total counter
microswitch_success_total{service="user-service",version="stable",strategy="canary"} 85
microswitch_success_total{service="user-service",version="experimental",strategy="canary"} 15

# HELP microswitch_error_total
# TYPE microswitch_error_total counter
microswitch_error_total{service="user-service",version="stable",strategy="canary"} 3
microswitch_error_total{service="user-service",version="experimental",strategy="canary"} 2
```

### Prometheus setup (recommended)

Microswitch does not expose a custom Prometheus endpoint. Instead, use Spring Boot Actuator’s built-in `/actuator/prometheus` endpoint. This keeps the library backend-agnostic and simpler for developers.

1) Add dependencies in the consuming app:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
  <scope>runtime</scope>
</dependency>
```

2) Expose the Prometheus endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

3) Configure Prometheus to scrape your service:

```yaml
scrape_configs:
  - job_name: "microswitch"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["localhost:8080"]
```

### Automatic metrics handling

Microswitch automatically provides a `DeploymentMetrics` bean in all scenarios:

- **With `MeterRegistry`** (when Actuator + Prometheus registry are present): Real metrics are recorded and exposed at `/actuator/prometheus`
- **Without `MeterRegistry`**: A no-operation implementation is used that safely ignores metric calls without errors

This ensures `DeploymentStrategyExecutor` never encounters null metrics, maintaining reliability across all deployment configurations.

4) Example PromQL queries using Microswitch counters

Counters are registered as `microswitch.success` / `microswitch.error` and exposed to Prometheus as `microswitch_success_total` / `microswitch_error_total` with tags `service`, `version`, `strategy`.

```promql
# Per-service, per-strategy success rate (last 5 minutes)
sum by(service, strategy) (
  rate(microswitch_success_total[5m])
) /
sum by(service, strategy) (
  rate(microswitch_success_total[5m]) + rate(microswitch_error_total[5m])
)

# Canary-only success ratio
sum by(service) (
  rate(microswitch_success_total{strategy="canary"}[5m])
) /
sum by(service) (
  rate(microswitch_success_total{strategy="canary"}[5m]) + rate(microswitch_error_total{strategy="canary"}[5m])
)

# Shadow accuracy across stable/experimental
sum by(service) (
  rate(microswitch_success_total{strategy="shadow",version=~"stable|experimental"}[5m])
) /
sum by(service) (
  rate(microswitch_success_total{strategy="shadow",version=~"stable|experimental"}[5m]) +
  rate(microswitch_error_total{strategy="shadow",version=~"stable|experimental"}[5m])
)
```

### Actuator Endpoint

```bash
# View current configuration
GET /actuator/microswitch

# Example response
{
  "services": {
    "user-service": {
      "enabled": true,
      "canary": {
        "primaryPercentage": 80,
        "algorithm": "random"
      }
    }
  }
}
```

## Public API & Module Boundaries

Microswitch exposes a single public API surface: `com.microswitch.infrastructure.manager.DeploymentManager`.

With JPMS, only this package is exported. Internal packages (e.g., `domain`, `application`, `infrastructure`) are not exported and may change without notice. Even on the classpath, you should only call `DeploymentManager`.

- Exported: `com.microswitch.infrastructure.manager`
- Opened (for reflective access):
  - `com.microswitch.application.config` — Spring Boot auto-configuration
  - `com.microswitch.infrastructure.external` — Actuator endpoints

If you use JPMS (module-path), add Microswitch to your `module-info.java`:

```java
module your.app.module {
  requires spring.boot;
  requires spring.boot.autoconfigure;
  requires com.n11.development.microswitch; // Microswitch public API
  // ... other requires
}
```

### DeploymentManager

```java
public class DeploymentManager {
    
    // Configuration-driven deployment (NEW in v1.1.0)
    public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey);
    
    // Legacy strategy-specific methods (deprecated in v1.1.0)
    @Deprecated(since = "1.1.0", forRemoval = true)
    public <R> R canary(Supplier<R> stable, Supplier<R> experimental, String serviceKey);
    
    @Deprecated(since = "1.1.0", forRemoval = true)
    public <R> R shadow(Supplier<R> stable, Supplier<R> experimental, String serviceKey);
    
    @Deprecated(since = "1.1.0", forRemoval = true)
    public <R> R blueGreen(Supplier<R> blue, Supplier<R> green, String serviceKey);
}
```

### Supplier usage

```java
// Method reference (recommended with new execute method)
deploymentManager.execute(this::methodV1, this::methodV2, "service");

// Lambda expression (configuration-driven)
deploymentManager.execute(
    () -> processPayment(request),
    () -> processPaymentNew(request),
    "payment"
);

// Legacy approach (deprecated)
// deploymentManager.canary(this::methodV1, this::methodV2, "service");
```

## Examples

### E-Commerce Payment

```java
@Service
@Slf4j
public class PaymentService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public PaymentResult processPayment(PaymentRequest request) {
        // Configuration-driven approach (v1.1.0+)
        return deploymentManager.execute(
            () -> processWithOldProvider(request),
            () -> processWithNewProvider(request),
            "payment-processing"  // Strategy determined by activeStrategy in config
        );
    }
    
    private PaymentResult processWithOldProvider(PaymentRequest request) {
        log.info("Processing with legacy provider");
        // Legacy payment logic
        return PaymentResult.success("OLD_PROVIDER");
    }
    
    private PaymentResult processWithNewProvider(PaymentRequest request) {
        log.info("Processing with new provider");
        // New payment logic
        return PaymentResult.success("NEW_PROVIDER");
    }
}
```

### User Authentication

```java
@Service
public class AuthService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public boolean authenticateUser(String username, String password) {
        // Configuration-driven approach (v1.1.0+)
        return deploymentManager.execute(
            () -> authenticateWithLDAP(username, password),
            () -> authenticateWithOAuth(username, password),
            "user-auth"  // Strategy determined by activeStrategy in config
        );
    }
    
    // Shadow: returns LDAP result, mirrors OAuth in background
    private boolean authenticateWithLDAP(String username, String password) {
        // LDAP authentication
        return ldapService.authenticate(username, password);
    }
    
    private boolean authenticateWithOAuth(String username, String password) {
        // OAuth authentication (shadow testing)
        return oauthService.authenticate(username, password);
    }
}
```

### Analytics

```java
@Service
public class AnalyticsService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public AnalyticsReport generateReport(String userId) {
        // Configuration-driven approach (v1.1.0+)
        return deploymentManager.execute(
            () -> generateReportV1(userId),
            () -> generateReportV2(userId),
            "analytics-report"  // Strategy determined by activeStrategy in config
        );
    }
    
    // Blue/Green: fully switches to V2 after TTL
}
```

## Best Practices

### 1) Service key naming
```java
// ✅ İyi
"user-registration"
"payment-processing"
"order-fulfillment"

// ❌ Kötü
"service1"
"test"
"temp"
```

### 2) Use configuration-driven approach
```java
// ✅ Recommended - Configuration-driven (v1.1.0+)
deploymentManager.execute(this::methodV1, this::methodV2, "service");

// ❌ Deprecated - Strategy-specific methods
deploymentManager.canary(this::methodV1, this::methodV2, "service");
```

### 3) Prefer method references
```java
// ✅ Good - Lazy evaluation
deploymentManager.execute(this::methodV1, this::methodV2, "service");

// ❌ Bad - Eager evaluation
deploymentManager.execute(() -> methodV1(), () -> methodV2(), "service");
```

### 4) Error handling
```java
public Result processData(String data) {
    try {
        return deploymentManager.execute(
            () -> processV1(data),
            () -> processV2(data),
            "data-processing"
        );
    } catch (Exception e) {
        log.error("Deployment failed, using fallback", e);
        return processV1(data); // Fallback to stable
    }
}
```

### 5) Monitoring
```java
// Check metrics and configuration
@EventListener
public void onApplicationReady(ApplicationReadyEvent event) {
    log.info("Microswitch metrics at /actuator/prometheus");
    log.info("Microswitch config at /actuator/microswitch");
}
```

## Troubleshooting

### 1) Bean not found
```
Error: No qualifying bean of type 'DeploymentManager'
```

**Solution 1**: If you're using an older version (< v1.1.1), add `com.n11.development` into your `@ComponentScan`:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourpackage", "com.n11.development"})
public class Application {
    // ...
}
```

**Solution 2**: If you're using v1.1.1+ and have `microswitch.enabled=false`, this issue is automatically resolved. The `DeploymentManager` bean is now always created regardless of the enabled setting. Simply ensure you have the correct version:

```xml
<dependency>
  <groupId>com.n11.development</groupId>
  <artifactId>microswitch</artifactId>
  <version>1.1.2</version>
</dependency>
```

#### 2. Konfigürasyon Yüklenmiyor
**Çözüm**: `application.yml` dosyasında `microswitch` konfigürasyonunu kontrol edin.

#### 3. Metrics Görünmüyor
**Çözüm**: Micrometer ve Actuator dependencies'lerini ekleyin:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Debug Modu

```yaml
logging:
  level:
    com.n11.development: DEBUG
```

### Health Check

```java
@Component
public class MicroswitchHealthCheck {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    @EventListener
    public void checkHealth(ApplicationReadyEvent event) {
        try {
            String result = deploymentManager.execute(
                () -> "healthy",
                () -> "healthy",
                "health-check"
            );
            log.info("Microswitch health check: {}", result);
        } catch (Exception e) {
            log.error("Microswitch health check failed", e);
        }
    }
}
```

## JPMS Module-Info Usage

```java
module com.example.microswitch {
    requires com.n11.development.microswitch;
    // ...
}
```

## Performance Notes

- Lazy evaluation: only the selected Supplier executes
- Minimal overhead added to your call path
- Thread-safe by design within strategy execution paths

## Contributing & Governance

Please see:

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## Security

See [SECURITY.md](SECURITY.md) for how to report vulnerabilities.

## License (Apache-2.0)

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details, or visit:

https://www.apache.org/licenses/LICENSE-2.0

## Support

- Email: development@n11.com
- Issues: https://github.com/n11-development/microswitch/issues
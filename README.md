# Microswitch

Microswitch is a lightweight Java library for Spring Boot applications that allows you to dynamically switch between different method implementations based on configurable deployment strategies. This is particularly useful for implementing patterns like Canary Releases, Blue-Green Deployments, and Shadow Testing for microservice calls or feature toggles directly within your application logic.

## Features

*   **Multiple Deployment Strategies:** Supports Canary, Blue-Green, and Shadow strategies out-of-the-box.
*   **Externalized Configuration:** Define and modify deployment strategies using simple YAML files without changing your code.
*   **Dynamic Reloading:** Leverages Spring Cloud's `@RefreshScope` to update strategies on-the-fly via the `/actuator/refresh` endpoint.
*   **Fluent API:** An intuitive and easy-to-use API for executing your chosen strategy.

## How to Use

### 1. Add the Dependency

First, build the project and install it to your local Maven repository:

```bash
mvn clean install
```

Then, add the following dependency to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.n11.development</groupId>
    <artifactId>microswitch</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Create Configuration Files

In your Spring Boot application, create a directory named `emb-deployer` in your `src/main/resources` folder. Inside this directory, you will define your deployment strategies in YAML files. The library loads the appropriate configuration based on the active Spring profile.

The path of the configuration file should follow this pattern: `src/main/resources/emb-deployer/{domain-name}/emb-deploy-{profile-name}.yml`.

For example, to configure a **Canary Release** for a "get-basket" service in the "basket" domain for the `dev` profile, you would create the file `src/main/resources/emb-deployer/basket/emb-deploy-dev.yml` with the following content:

```yaml
version: v1
kind: EmbeddedDeployment
domain: basket
platform: dev
embeddedServices:
  get-basket:
    metadata:
      name: get-basket
    spec:
      deployment:
        strategy: canary
        canaryPercentage: 30/70  # 30% of traffic to the new version, 70% to the old
        algorithm: random        # or "sequence"
        enabled: true
```

### 2.1. Update `application.yml`

For the library to work correctly, you need to add the following configuration to your main `application.yml` file.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: refresh

emb-deployer:
  refresh-version: 1.0.0
```

*   `management.endpoints.web.exposure.include: refresh`: This is required to enable the dynamic reloading feature. It exposes the `/actuator/refresh` endpoint, which you can call to make the library reload its configuration without restarting the application.
*   `emb-deployer.refresh-version`: This property is used by the library to track configuration changes. You should update this version number whenever you modify your deployment strategy files to ensure the changes are reloaded.

### 3. Use in Your Code

Inject the `IEmbeddedDeploy` bean into your service and use its fluent API to execute your methods based on the configured strategy.

```java
import com.n11.development.infrastructure.IEmbeddedDeploy;
import com.n11.development.infrastructure.strategy.StrategyType;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.function.Function;

@Service
public class MyBasketService {

    private final IEmbeddedDeploy embeddedDeploy;

    public MyBasketService(IEmbeddedDeploy embeddedDeploy) {
        this.embeddedDeploy = embeddedDeploy;
    }

    // This is your old, stable method
    public String getBasketFromLegacyService(String basketId) {
        // ... logic to call the old service
        return "Response from LEGACY basket service for ID: " + basketId;
    }

    // This is your new method with the changes you want to test
    public String getBasketFromNewService(String basketId) {
        // ... logic to call the new service
        return "Response from NEW basket service for ID: " + basketId;
    }

    /**
     * This method will use Microswitch to decide which of the above methods to call.
     */
    public String getBasket(String basketId) {
        // Define the two functions to be switched.
        // The first function in the list is considered the old/stable version.
        // The second function is the new version.
        List<Function<String, String>> functions = List.of(
            this::getBasketFromLegacy-Service,
            this::getBasketFromNewService
        );

        // Execute the switch using the configuration defined in your YAML files.
        return embeddedDeploy
            .setExecutableService("basket", "get-basket") // Corresponds to domain and service key in YAML
            .execute(StrategyType.CANARY, functions, basketId);
    }
}
```

## Configuration Reference

The behavior of the switch is controlled by the `deployment` spec in your YAML files.

| Property           | Description                                                                                             | Example        |
| ------------------ | ------------------------------------------------------------------------------------------------------- | -------------- |
| `strategy`         | The deployment strategy to use. Can be `canary`, `blueGreen`, or `shadow`.                              | `canary`       |
| `enabled`          | A boolean flag to enable or disable the strategy for this service. If `false`, the first function runs. | `true`         |
| `canaryPercentage` | **(Canary)** Defines the traffic split between the old and new functions (old/new). Must sum to 100.    | `30/70`        |
| `algorithm`        | **(Canary)** The algorithm for distributing traffic. Can be `sequence` or `random`. Defaults to `sequence`. | `random`       |
| `weight`           | **(BlueGreen)** Defines which function to run (old/new). `1/0` runs the old, `0/1` runs the new.         | `0/1`          |
| `ttl`              | **(BlueGreen)** Time-to-live in seconds. After the TTL expires, the traffic switches to the other version. `0` disables TTL. | `3600` |
| `shadowWeight`     | **(Shadow)** Defines the rate of shadowing. A value of `5` means 1 in every 5 calls will be shadowed.   | `5`            |

## Supported Strategies

### Canary

Distributes traffic between two versions of a function based on a percentage. This is useful for rolling out new features to a small subset of users before a full release.

### Blue-Green

Switches all traffic from one version to another. It can be configured with a Time-To-Live (TTL) to automatically switch traffic after a certain period.

### Shadow

Sends traffic to the primary (old) function and asynchronously copies it to the secondary (new) function. The result of the primary function is returned to the client, while the result of the shadow function can be used for testing and comparison. This is useful for testing new features with production traffic without affecting users.
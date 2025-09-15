package com.microswitch.application.executor;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.value.StrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Executor that delegates to registered DeploymentStrategy implementations.
 *
 * <p>Follows SOLID principles:
 * - Single Responsibility: Only orchestrates strategy lookup and execution.
 * - Open/Closed: New strategies can be registered without modifying this class.
 * - Liskov Substitution: Uses the DeploymentStrategy abstraction.
 * - Interface Segregation: Depends only on DeploymentStrategy.
 * - Dependency Inversion: Strategies are provided via constructor-time hook.
 */
public class DeploymentStrategyExecutor {

    private static final Logger log = LoggerFactory.getLogger(DeploymentStrategyExecutor.class);
    private static final String STABLE = "stable";
    private static final String EXPERIMENTAL = "experimental";

    private final Map<StrategyType, DeploymentStrategy> strategies = new EnumMap<>(StrategyType.class);
    private final DeploymentMetrics deploymentMetrics; // may be null if no MeterRegistry
    private final InitializerConfiguration properties;

    /**
     * Construct executor and initialize strategies via template hook.
     */
    public DeploymentStrategyExecutor(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.deploymentMetrics = deploymentMetrics; // may be null
        initializeStrategies(this.properties, deploymentMetrics); // deploymentMetrics can be null if MeterRegistry is not available
    }

    /**
     * Hook for registering strategies. Subclasses should call {@link #addStrategy(StrategyType, DeploymentStrategy)}
     * to register their implementations.
     */
    protected void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        // No-op by default. Subclasses can register concrete strategies here.
    }

    /**
     * Register a strategy implementation for a type.
     */
    protected void addStrategy(StrategyType type, DeploymentStrategy strategy) {
        strategies.put(Objects.requireNonNull(type, "type must not be null"),
                Objects.requireNonNull(strategy, "strategy must not be null"));
    }

    public <R> R executeCanary(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        return getRequiredStrategy(StrategyType.CANARY)
                .execute(
                        wrap(primary, serviceKey, STABLE, StrategyType.CANARY.getValue()),
                        wrap(secondary, serviceKey, EXPERIMENTAL, StrategyType.CANARY.getValue()),
                        serviceKey);
    }

    public <R> R executeShadow(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        return getRequiredStrategy(StrategyType.SHADOW)
                .execute(
                        wrap(primary, serviceKey, STABLE, StrategyType.SHADOW.getValue()),
                        wrap(secondary, serviceKey, EXPERIMENTAL, StrategyType.SHADOW.getValue()),
                        serviceKey);
    }

    public <R> R executeBlueGreen(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        return getRequiredStrategy(StrategyType.BLUE_GREEN)
                .execute(
                        wrap(primary, serviceKey, STABLE, StrategyType.BLUE_GREEN.getValue()),
                        wrap(secondary, serviceKey, EXPERIMENTAL, StrategyType.BLUE_GREEN.getValue()),
                        serviceKey);
    }

    /**
     * Executes deployment strategy based on the activeStrategy configuration for the given service.
     * 
     * <p>This method follows SOLID principles:
     * - Single Responsibility: Delegates strategy execution to appropriate handlers
     * - Open/Closed: New strategies can be added by extending StrategyType enum
     * - Dependency Inversion: Depends on StrategyType abstraction, not concrete strings
     *
     * @param primary the stable/primary function supplier
     * @param secondary the experimental/secondary function supplier
     * @param serviceKey the unique identifier for service configuration
     * @return the result from the selected strategy execution
     * @throws IllegalArgumentException if serviceKey is null, empty, or activeStrategy is not configured
     * @throws IllegalStateException if activeStrategy value is invalid or strategy is not registered
     */
    public <R> R executeByActiveStrategy(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        validateServiceKey(serviceKey);
        
        StrategyType strategyType = resolveActiveStrategy(serviceKey);
        
        return executeStrategyByType(strategyType, primary, secondary, serviceKey);
    }

    /**
     * Validates the service key parameter.
     * 
     * @param serviceKey the service key to validate
     * @throws IllegalArgumentException if serviceKey is null or empty
     */
    private void validateServiceKey(String serviceKey) {
        if (serviceKey == null || serviceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Service key must not be null or empty");
        }
    }
    
    /**
     * Resolves the active strategy for the given service key.
     * 
     * @param serviceKey the service key to look up
     * @return the resolved StrategyType
     * @throws IllegalArgumentException if activeStrategy is not configured or invalid
     */
    private StrategyType resolveActiveStrategy(String serviceKey) {
        String activeStrategyValue = getActiveStrategyValue(serviceKey);
        
        if (activeStrategyValue == null || activeStrategyValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Active strategy not configured for service: " + serviceKey);
        }
        
        try {
            return StrategyType.fromValue(activeStrategyValue.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid active strategy '" + activeStrategyValue + "' for service '" + serviceKey + 
                "'. Valid values are: " + getValidStrategyValues(), e);
        }
    }
    
    /**
     * Executes the strategy based on the resolved StrategyType using Java 21 enhanced switch expressions.
     * 
     * <p>This method demonstrates Java 21 best practices:
     * - Enhanced switch expressions with yield statements
     * - Exhaustive pattern matching
     * - Clear separation of concerns
     * - Proper logging for operational visibility
     * 
     * @param strategyType the strategy type to execute
     * @param primary the stable/primary function supplier
     * @param secondary the experimental/secondary function supplier
     * @param serviceKey the unique identifier for service configuration
     * @return the result from the selected strategy execution
     */
    private <R> R executeStrategyByType(StrategyType strategyType, Supplier<R> primary, 
                                       Supplier<R> secondary, String serviceKey) {
        log.debug("Executing {} strategy for service: {}", strategyType.getValue(), serviceKey);
        
        return switch (strategyType) {
            case CANARY -> {
                log.trace("Delegating to canary strategy execution for service: {}", serviceKey);
                yield executeCanary(primary, secondary, serviceKey);
            }
            case SHADOW -> {
                log.trace("Delegating to shadow strategy execution for service: {}", serviceKey);
                yield executeShadow(primary, secondary, serviceKey);
            }
            case BLUE_GREEN -> {
                log.trace("Delegating to blue-green strategy execution for service: {}", serviceKey);
                yield executeBlueGreen(primary, secondary, serviceKey);
            }
        };
    }
    
    /**
     * Retrieves the active strategy configuration value for the given service key.
     *
     * @param serviceKey the service key to look up
     * @return the active strategy name, or null if not configured
     */
    private String getActiveStrategyValue(String serviceKey) {
        if (properties == null || properties.getServices() == null) {
            return null;
        }
        
        InitializerConfiguration.DeployableServices serviceConfig = properties.getServices().get(serviceKey);
        if (serviceConfig == null) {
            return null;
        }
        
        return serviceConfig.getActiveStrategy();
    }
    
    /**
     * Returns a comma-separated string of valid strategy values for error messages.
     * 
     * @return valid strategy values as a string
     */
    private String getValidStrategyValues() {
        return java.util.Arrays.stream(StrategyType.values())
            .map(StrategyType::getValue)
            .collect(java.util.stream.Collectors.joining(", "));
    }

    private DeploymentStrategy getRequiredStrategy(StrategyType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new IllegalStateException("No strategy registered for type: " + type));
    }

    /**
     * Decorate a supplier to record success/error metrics with tags when invoked.
     * If deploymentMetrics is null (no MeterRegistry), returns the original supplier.
     */
    private <R> Supplier<R> wrap(Supplier<R> original, String serviceKey, String version, String strategy) {
        if (deploymentMetrics == null || original == null) {
            return original;
        }
        return () -> {
            try {
                R result = original.get();
                deploymentMetrics.recordSuccess(serviceKey, version, strategy);
                return result;
            } catch (RuntimeException e) {
                deploymentMetrics.recordError(serviceKey, version, strategy);
                throw e;
            }
        };
    }
}

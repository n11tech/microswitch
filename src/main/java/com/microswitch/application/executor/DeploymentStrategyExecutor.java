package com.microswitch.application.executor;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.value.StrategyType;

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

    private final Map<StrategyType, DeploymentStrategy> strategies = new EnumMap<>(StrategyType.class);
    private final DeploymentMetrics deploymentMetrics; // may be null if no MeterRegistry

    /**
     * Construct executor and initialize strategies via template hook.
     */
    public DeploymentStrategyExecutor(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        this.deploymentMetrics = deploymentMetrics; // may be null
        initializeStrategies(Objects.requireNonNull(properties, "properties must not be null"),
                deploymentMetrics); // deploymentMetrics can be null if MeterRegistry is not available
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
                        wrap(primary, serviceKey, "stable", "canary"),
                        wrap(secondary, serviceKey, "experimental", "canary"),
                        serviceKey);
    }

    public <R> R executeShadow(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        return getRequiredStrategy(StrategyType.SHADOW)
                .execute(
                        wrap(primary, serviceKey, "stable", "shadow"),
                        wrap(secondary, serviceKey, "experimental", "shadow"),
                        serviceKey);
    }

    public <R> R executeBlueGreen(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        return getRequiredStrategy(StrategyType.BLUE_GREEN)
                .execute(
                        wrap(primary, serviceKey, "stable", "blue_green"),
                        wrap(secondary, serviceKey, "experimental", "blue_green"),
                        serviceKey);
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

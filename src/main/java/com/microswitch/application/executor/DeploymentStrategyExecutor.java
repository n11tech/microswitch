package com.microswitch.application.executor;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.strategy.StrategyType;

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

    /**
     * Construct executor and initialize strategies via template hook.
     */
    public DeploymentStrategyExecutor(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        initializeStrategies(Objects.requireNonNull(properties, "properties must not be null"),
                Objects.requireNonNull(deploymentMetrics, "deploymentMetrics must not be null"));
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
                .execute(primary, secondary, serviceKey);
    }

    public <R> R executeShadow(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        return getRequiredStrategy(StrategyType.SHADOW)
                .execute(primary, secondary, serviceKey);
    }

    public <R> R executeBlueGreen(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        return getRequiredStrategy(StrategyType.BLUE_GREEN)
                .execute(primary, secondary, serviceKey);
    }

    private DeploymentStrategy getRequiredStrategy(StrategyType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new IllegalStateException("No strategy registered for type: " + type));
    }
}

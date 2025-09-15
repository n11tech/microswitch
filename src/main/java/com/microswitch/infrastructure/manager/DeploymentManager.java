package com.microswitch.infrastructure.manager;

import java.util.function.Supplier;

/**
 * Central facade for managing different deployment strategies.
 *
 * <p>This class provides a simplified interface for executing deployment strategies
 * such as Canary, Shadow, and Blue/Green deployments. It delegates the actual
 * strategy execution to an internal executor without exposing implementation details.
 *
 * <p>The deployment strategy is determined by the configuration (activeStrategy)
 * for each service, allowing for configuration-driven deployment management.
 *
 * <p>All methods use lazy evaluation through {@link Supplier} parameters,
 * ensuring that only the selected deployment path is executed.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private DeploymentManager deploymentManager;
 *
 * public String processPayment(PaymentRequest request) {
 *     return deploymentManager.execute(
 *         () -> legacyPaymentService.process(request),
 *         () -> newPaymentService.process(request),
 *         "payment-service"
 *     );
 * }
 * }</pre>
 *
 * @author N11 Development Team
 * @since 1.0
 */
public final class DeploymentManager {
    
    private final Object strategyExecutor;
    
    /**
     * Private constructor to prevent direct instantiation.
     * Use the static factory method instead.
     */
    private DeploymentManager(Object strategyExecutor) {
        if (strategyExecutor == null) {
            throw new NullPointerException("Strategy executor must not be null");
        }
        if (!strategyExecutor.getClass().getName().contains("DeploymentStrategyExecutor")) {
            throw new IllegalArgumentException("Invalid strategy executor type");
        }
        this.strategyExecutor = strategyExecutor;
    }
    
    /**
     * Factory method for internal use by Spring auto-configuration.
     * This method is intentionally not public to prevent external usage.
     * The parameter is typed as Object to hide internal implementation types.
     * 
     * @param strategyExecutor the internal strategy executor
     * @return a new DeploymentManager instance
     */
    public static DeploymentManager createWithExecutor(Object strategyExecutor) {
        return new DeploymentManager(strategyExecutor);
    }

    
    /**
     * Executes the configured deployment strategy for the specified service.
     *
     * <p>The deployment strategy (canary, shadow, or blueGreen) is determined
     * by the 'activeStrategy' configuration for the given serviceKey.
     *
     * @param <R>          the return type of both suppliers
     * @param stable       the stable/primary function supplier
     * @param experimental the experimental/secondary function supplier
     * @param serviceKey   the unique identifier for service configuration
     * @return the result from the selected supplier execution based on active strategy
     * @throws IllegalArgumentException if serviceKey is null or empty, or if activeStrategy is not configured
     */
    public <R> R execute(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return invokeStrategy("executeByActiveStrategy", stable, experimental, serviceKey);
    }

    /**
     * Executes a canary deployment strategy.
     *
     * <p>Canary deployment gradually shifts traffic from the stable version
     * to the experimental version based on configured percentages.
     *
     * @param <R>          the return type of both suppliers
     * @param stable       the stable/primary function supplier
     * @param experimental the experimental/secondary function supplier
     * @param serviceKey   the unique identifier for service configuration
     * @return the result from the selected supplier execution
     * @throws IllegalArgumentException if serviceKey is null or empty
     * @deprecated Use {@link #execute(Supplier, Supplier, String)} with activeStrategy configuration instead
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public <R> R canary(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return invokeStrategy("executeCanary", stable, experimental, serviceKey);
    }

    
    /**
     * Executes a shadow deployment strategy.
     *
     * <p>Shadow deployment runs both versions in parallel but always returns
     * the result from the stable version. The experimental version runs for
     * comparison and testing purposes.
     *
     * @param <R>          the return type of both suppliers
     * @param stable       the stable/primary function supplier
     * @param experimental the experimental/shadow function supplier
     * @param serviceKey   the unique identifier for service configuration
     * @return the result from the stable supplier execution
     * @throws IllegalArgumentException if serviceKey is null or empty
     * @deprecated Use {@link #execute(Supplier, Supplier, String)} with activeStrategy configuration instead
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public <R> R shadow(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return invokeStrategy("executeShadow", stable, experimental, serviceKey);
    }

    
    /**
     * Executes a blue/green deployment strategy.
     *
     * <p>Blue/Green deployment switches between two complete environments.
     * Switching is binary via weight (1/0 or 0/1) and/or time-based using TTL.
     *
     * @param <R>          the return type of both suppliers
     * @param stable       the blue/stable environment function supplier
     * @param experimental the green/new environment function supplier
     * @param serviceKey   the unique identifier for service configuration
     * @return the result from the selected supplier execution
     * @throws IllegalArgumentException if serviceKey is null or empty
     * @deprecated Use {@link #execute(Supplier, Supplier, String)} with activeStrategy configuration instead
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public <R> R blueGreen(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return invokeStrategy("executeBlueGreen", stable, experimental, serviceKey);
    }
    
    /**
     * Internal helper method to invoke strategy methods via reflection.
     * This allows us to work with the internal executor without exposing its type.
     */
    @SuppressWarnings("unchecked")
    private <R> R invokeStrategy(String methodName, Supplier<R> stable, 
                                  Supplier<R> experimental, String serviceKey) {
        try {
            var method = strategyExecutor.getClass().getMethod(methodName, 
                Supplier.class, Supplier.class, String.class);
            return (R) method.invoke(strategyExecutor, stable, experimental, serviceKey);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke strategy method: " + methodName, e);
        }
    }
}

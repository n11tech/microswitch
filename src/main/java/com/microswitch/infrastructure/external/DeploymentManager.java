package com.microswitch.infrastructure.external;

import com.microswitch.application.executor.DeploymentStrategyExecutor;

import java.util.function.Supplier;

/**
 * Central facade for managing different deployment strategies.
 * 
 * <p>This class provides a simplified interface for executing deployment strategies
 * such as Canary, Shadow, and Blue/Green deployments. It delegates the actual
 * strategy execution to the configured {@link DeploymentStrategyExecutor}.
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
 *     return deploymentManager.canary(
 *         () -> legacyPaymentService.process(request),
 *         () -> newPaymentService.process(request),
 *         "payment-service"
 *     );
 * }
 * }</pre>
 * 
 * @since 1.0
 * @author N11 Development Team
 */
public class DeploymentManager {
    
    private final DeploymentStrategyExecutor strategyExecutor;
    
    public DeploymentManager(DeploymentStrategyExecutor strategyExecutor) {
        this.strategyExecutor = strategyExecutor;
    }
    
    /**
     * Executes a canary deployment strategy.
     * 
     * <p>Canary deployment gradually shifts traffic from the stable version
     * to the experimental version based on configured percentages.
     * 
     * @param <R> the return type of both suppliers
     * @param stable the stable/primary function supplier
     * @param experimental the experimental/secondary function supplier  
     * @param serviceKey the unique identifier for service configuration
     * @return the result from the selected supplier execution
     * @throws IllegalArgumentException if serviceKey is null or empty
     */
    public <R> R canary(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return strategyExecutor.executeCanary(stable, experimental, serviceKey);
    }
    
    /**
     * Executes a shadow deployment strategy.
     * 
     * <p>Shadow deployment runs both versions in parallel but always returns
     * the result from the stable version. The experimental version runs for
     * comparison and testing purposes.
     * 
     * @param <R> the return type of both suppliers
     * @param stable the stable/primary function supplier
     * @param experimental the experimental/shadow function supplier
     * @param serviceKey the unique identifier for service configuration
     * @return the result from the stable supplier execution
     * @throws IllegalArgumentException if serviceKey is null or empty
     */
    public <R> R shadow(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return strategyExecutor.executeShadow(stable, experimental, serviceKey);
    }
    
    /**
     * Executes a blue/green deployment strategy.
     * 
     * <p>Blue/Green deployment switches between two complete environments.
     * It can use percentage-based routing or TTL-based complete switching.
     * 
     * @param <R> the return type of both suppliers
     * @param stable the blue/stable environment function supplier
     * @param experimental the green/new environment function supplier
     * @param serviceKey the unique identifier for service configuration
     * @return the result from the selected supplier execution
     * @throws IllegalArgumentException if serviceKey is null or empty
     */
    public <R> R blueGreen(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return strategyExecutor.executeBlueGreen(stable, experimental, serviceKey);
    }
}


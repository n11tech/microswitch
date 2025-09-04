package com.microswitch.domain;

import com.microswitch.domain.strategy.DeploymentStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Central facade for managing different deployment strategies.
 * 
 * <p>This class provides a simplified interface for executing deployment strategies
 * such as Canary, Shadow, and Blue/Green deployments. It delegates the actual
 * strategy execution to the configured {@link DeploymentStrategyFactory}.
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
@Component
public class DeploymentManager {
    
    private final DeploymentStrategyFactory strategyFactory;
    
    @Autowired
    public DeploymentManager(DeploymentStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
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
        return strategyFactory.executeCanary(stable, experimental, serviceKey);
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
        return strategyFactory.executeShadow(stable, experimental, serviceKey);
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
        return strategyFactory.executeBlueGreen(stable, experimental, serviceKey);
    }
}


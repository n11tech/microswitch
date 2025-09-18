package com.microswitch.domain.strategy;

import java.util.function.Supplier;

/**
 * Strategy interface for different deployment patterns.
 * 
 * <p>Implementations provide specific deployment strategies such as:
 * <ul>
 *   <li>Canary deployment - gradual traffic shifting</li>
 *   <li>Shadow deployment - parallel execution with comparison</li>
 *   <li>Blue/Green deployment - complete environment switching</li>
 * </ul>
 * 
 * @since 1.0
 */
public interface DeploymentStrategy {
    
    /**
     * Executes the deployment strategy with two function suppliers.
     * 
     * @param <R> the return type of both functions
     * @param primary the primary/stable function supplier
     * @param secondary the secondary/experimental function supplier
     * @param serviceKey the unique identifier for the service configuration
     * @return the result from the selected function execution
     * @throws IllegalArgumentException if serviceKey is null or empty
     */
    <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey);
    
    /**
     * Returns the name of this deployment strategy.
     * 
     * @return strategy name in lowercase
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName().toLowerCase();
    }
}

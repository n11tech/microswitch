package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class DeploymentStrategyFactory {
    
    private final Map<StrategyType, DeploymentStrategy> strategies;
    
    protected DeploymentStrategyFactory(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        this.strategies = new HashMap<>();
        initializeStrategies(properties, deploymentMetrics);
    }

    public <R> R executeCanary(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        DeploymentStrategy strategy = strategies.get(StrategyType.CANARY);
        return strategy.execute(primary, secondary, serviceKey);
    }

    public <R> R executeShadow(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        DeploymentStrategy strategy = strategies.get(StrategyType.SHADOW);
        return strategy.execute(primary, secondary, serviceKey);
    }

    public <R> R executeBlueGreen(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        DeploymentStrategy strategy = strategies.get(StrategyType.BLUE_GREEN);
        return strategy.execute(primary, secondary, serviceKey);
    }

    protected abstract void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics);

    protected void addStrategy(StrategyType strategyType, DeploymentStrategy strategy) {
        strategies.put(strategyType, strategy);
    }
}

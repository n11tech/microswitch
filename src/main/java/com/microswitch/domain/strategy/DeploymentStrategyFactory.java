package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class DeploymentStrategyFactory {
    
    private final Map<StrategyType, IDeploymentStrategy> strategies;
    
    protected DeploymentStrategyFactory(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        this.strategies = new HashMap<>();
        initializeStrategies(properties, deploymentMetrics);
    }

    public <R> R executeCanary(Supplier<R> func1, Supplier<R> func2, String serviceKey) {
        IDeploymentStrategy strategy = strategies.get(StrategyType.CANARY);
        return strategy.execute(func1, func2, serviceKey);
    }

    public <R> R executeShadow(Supplier<R> func1, Supplier<R> func2, String serviceKey) {
        IDeploymentStrategy strategy = strategies.get(StrategyType.SHADOW);
        return strategy.execute(func1, func2, serviceKey);
    }

    public <R> R executeBlueGreen(Supplier<R> func1, Supplier<R> func2, String serviceKey) {
        IDeploymentStrategy strategy = strategies.get(StrategyType.BLUE_GREEN);
        return strategy.execute(func1, func2, serviceKey);
    }

    protected abstract void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics);

    protected void addStrategy(StrategyType strategyType, IDeploymentStrategy strategy) {
        strategies.put(strategyType, strategy);
    }
}

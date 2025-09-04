package com.microswitch.application.executor;

import com.microswitch.domain.strategy.StrategyType;

import java.util.function.Supplier;

public class DeploymentStrategyExecutor {

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
}

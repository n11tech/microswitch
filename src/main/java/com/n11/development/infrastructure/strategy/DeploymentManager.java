package com.n11.development.infrastructure.strategy;

import com.n11.development.infrastructure.IDeploymentManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
class DeploymentManager implements IDeploymentManager {
    private final Map<String, IDeploymentStrategy> strategies;

    public DeploymentManager(Map<String, IDeploymentStrategy> strategies) {
        this.strategies = strategies;
    }

    IDeploymentStrategy currentStrategy;

    @Override
    public void setStrategy(String strategyType) {
        this.currentStrategy = strategies.get(strategyType);
        if (this.currentStrategy == null) {
            throw new IllegalArgumentException("No deployment strategy found for type: " + strategyType);
        }
    }

    @Override
    public <T, R> R executeDeploy(List<Function<T, R>> funcs,
                                  String domain,
                                  String serviceKey,
                                  T input) {
        if (currentStrategy == null) {
            throw new IllegalStateException("Deployment strategy not set.");
        }
        if (funcs == null || funcs.size() != 2) {
            throw new IllegalArgumentException("The funcs list must contain exactly two functions.");
        }
        return currentStrategy.execute(funcs.get(0), funcs.get(1), domain, serviceKey, input);
    }
}

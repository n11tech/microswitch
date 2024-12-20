package com.n11.architecture.tools.microswitch.infrastructure;

import com.n11.architecture.tools.microswitch.infrastructure.strategy.StrategyType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class EmbeddedDeploy implements IEmbeddedDeploy {
    private final IDeploymentManager deploymentManager;

    private String domain;
    private String serviceKey;

    public EmbeddedDeploy(IDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    @Override
    public EmbeddedDeploy setExecutableService(String domain, String serviceKey) {
        this.domain = domain;
        this.serviceKey = serviceKey;
        return this;
    }

    @Override
    public <T, R> R execute(StrategyType strategyType, List<Function<T, R>> funcList, T input) {
        deploymentManager.setStrategy(strategyType.getValue());

        return deploymentManager.executeDeploy(funcList, domain, serviceKey, input);
    }
}

package com.n11.development.infrastructure.strategy;

import com.n11.development.application.config.EmbDeployer;
import com.n11.development.application.config.IEmbDeployerLoader;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public abstract class DeployTemplate {
    private final IEmbDeployerLoader _embDeployerLoader;

    protected DeployTemplate(IEmbDeployerLoader embDeployerLoader) {
        this._embDeployerLoader = embDeployerLoader;
    }

    protected EmbDeployer.ServiceConfig getEmbeddedService(String domain,
                                                           String serviceKey,
                                                           StrategyType strategyType) {
        var services = _embDeployerLoader.getConfiguration(domain);
        var service = services.get(serviceKey);
        if (Objects.isNull(service) || !deployScopeIsValid(strategyType, service.getSpec().getDeployment().getStrategy()))
            return new EmbDeployer.ServiceConfig();
        return service;
    }

    private boolean deployScopeIsValid(StrategyType strategyType, String serviceStrategy) {
        return Objects.equals(strategyType.getValue(), serviceStrategy);
    }

    protected boolean deployScopeIsEnabled(EmbDeployer.ServiceConfig service) {
        return Objects.nonNull(service.getMetadata()) && service.getSpec().getDeployment().isEnabled();
    }
}

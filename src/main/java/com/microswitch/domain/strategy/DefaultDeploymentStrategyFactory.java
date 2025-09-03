package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import org.springframework.stereotype.Component;

@Component
public class DefaultDeploymentStrategyFactory extends DeploymentStrategyFactory {
    
    public DefaultDeploymentStrategyFactory(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        super(properties, deploymentMetrics);
    }
    
    @Override
    protected void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {

        addStrategy(StrategyType.CANARY, new Canary(properties, deploymentMetrics));

        addStrategy(StrategyType.SHADOW, new Shadow(properties, deploymentMetrics));

        addStrategy(StrategyType.BLUE_GREEN, new BlueGreen(properties, deploymentMetrics));
    }
}

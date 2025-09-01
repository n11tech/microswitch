package com.n11.development.core.strategy;

import com.n11.development.infrastructure.metrics.DeploymentMetrics;
import com.n11.development.properties.MicroswitchProperties;
import org.springframework.stereotype.Component;

@Component
public class DefaultDeploymentStrategyFactory extends DeploymentStrategyFactory {
    
    public DefaultDeploymentStrategyFactory(MicroswitchProperties properties, DeploymentMetrics deploymentMetrics) {
        super(properties, deploymentMetrics);
    }
    
    @Override
    protected void initializeStrategies(MicroswitchProperties properties, DeploymentMetrics deploymentMetrics) {

        addStrategy(StrategyType.CANARY, new Canary(properties, deploymentMetrics));

        addStrategy(StrategyType.SHADOW, new Shadow(properties, deploymentMetrics));

        addStrategy(StrategyType.BLUE_GREEN, new BlueGreen(properties, deploymentMetrics));
    }
}

package com.microswitch.application.executor;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.strategy.BlueGreen;
import com.microswitch.domain.strategy.Canary;
import com.microswitch.domain.strategy.Shadow;
import com.microswitch.domain.value.StrategyType;

/**
 * Concrete implementation of DeploymentStrategyExecutor that registers all available strategies.
 * 
 * <p>This implementation automatically registers:
 * <ul>
 *   <li>Canary deployment strategy</li>
 *   <li>Shadow deployment strategy</li>
 *   <li>Blue/Green deployment strategy</li>
 * </ul>
 * 
 * @author N11 Development Team
 * @since 1.0
 */
public class MicroswitchDeploymentStrategyExecutor extends DeploymentStrategyExecutor {

    public MicroswitchDeploymentStrategyExecutor(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        super(properties, deploymentMetrics);
    }

    @Override
    protected void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        // Register all available deployment strategies
        addStrategy(StrategyType.CANARY, new Canary(properties));
        addStrategy(StrategyType.SHADOW, new Shadow(properties));
        addStrategy(StrategyType.BLUE_GREEN, new BlueGreen(properties));
    }
}

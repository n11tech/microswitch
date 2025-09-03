package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;

public abstract class DeployTemplate {
    protected final InitializerConfiguration properties;
    protected final DeploymentMetrics deploymentMetrics;

    protected DeployTemplate(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        this.properties = properties;
        this.deploymentMetrics = deploymentMetrics;
    }
}

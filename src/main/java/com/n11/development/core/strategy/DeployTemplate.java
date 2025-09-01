package com.n11.development.core.strategy;

import com.n11.development.infrastructure.metrics.DeploymentMetrics;
import com.n11.development.properties.MicroswitchProperties;

import java.util.function.Supplier;

public abstract class DeployTemplate {
    protected final MicroswitchProperties properties;
    protected final DeploymentMetrics deploymentMetrics;

    protected DeployTemplate(MicroswitchProperties properties, DeploymentMetrics deploymentMetrics) {
        this.properties = properties;
        this.deploymentMetrics = deploymentMetrics;
    }


}

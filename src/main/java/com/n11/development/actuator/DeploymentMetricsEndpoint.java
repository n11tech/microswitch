package com.n11.development.actuator;

import com.n11.development.core.service.DeploymentMetricsService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "deployment-metrics")
public class DeploymentMetricsEndpoint {
    
    private final DeploymentMetricsService deploymentMetricsService;
    
    public DeploymentMetricsEndpoint(DeploymentMetricsService deploymentMetricsService) {
        this.deploymentMetricsService = deploymentMetricsService;
    }
    
    @ReadOperation
    public Map<String, Object> getMetrics() {
        return deploymentMetricsService.getAllDeploymentMetrics();
    }
}

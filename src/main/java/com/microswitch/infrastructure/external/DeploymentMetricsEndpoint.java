package com.microswitch.infrastructure.external;

import com.microswitch.application.metric.DeploymentMetricsService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Map;
import java.util.HashMap;

@Endpoint(id = "microswitch-metrics")
public class DeploymentMetricsEndpoint {
    
    private final DeploymentMetricsService deploymentMetricsService;
    
    public DeploymentMetricsEndpoint(DeploymentMetricsService deploymentMetricsService) {
        this.deploymentMetricsService = deploymentMetricsService;
    }
    
    @ReadOperation
    public Map<String, Object> getMetrics() {
        if (deploymentMetricsService == null) {
            Map<String, Object> info = new HashMap<>();
            info.put("status", "disabled");
            info.put("reason", "Micrometer MeterRegistry not found. Add micrometer-registry-prometheus in the consuming app to enable metrics.");
            return info;
        }
        return deploymentMetricsService.getAllDeploymentMetrics();
    }
}

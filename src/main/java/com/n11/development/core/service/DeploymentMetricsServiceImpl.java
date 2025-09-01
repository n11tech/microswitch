package com.n11.development.core.service;

import com.n11.development.infrastructure.metrics.DeploymentMetrics;
import com.n11.development.properties.MicroswitchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DeploymentMetricsServiceImpl implements DeploymentMetricsService {
    
    private final DeploymentMetrics deploymentMetrics;
    private final MicroswitchProperties properties;
    
    public DeploymentMetricsServiceImpl(DeploymentMetrics deploymentMetrics, MicroswitchProperties properties) {
        this.deploymentMetrics = deploymentMetrics;
        this.properties = properties;
    }
    
    @Override
    public Map<String, Object> getAllDeploymentMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            var services = properties.getServices();
            
            for (var entry : services.entrySet()) {
                String serviceKey = entry.getKey();
                var serviceConfig = entry.getValue();
                
                if (serviceConfig.isEnabled()) {
                    if (serviceConfig.getCanary() != null) {
                        double successRate = getCanarySuccessRate(serviceKey);
                        metrics.put(serviceKey + "_canary_success_rate", successRate);
                        log.debug("Canary success rate for {}: {}%", serviceKey, successRate);
                    }

                    if (serviceConfig.getShadow() != null) {
                        double accuracyRate = getShadowAccuracyRate(serviceKey, "shadow");
                        metrics.put(serviceKey + "_shadow_accuracy_rate", accuracyRate);
                        log.debug("Shadow accuracy rate for {}: {}%", serviceKey, accuracyRate);
                    }

                    if (serviceConfig.getBlueGreen() != null) {
                        String status = getBlueGreenStatus(serviceKey);
                        metrics.put(serviceKey + "_bluegreen_status", status);
                        log.debug("Blue/Green status for {}: {}", serviceKey, status);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error collecting deployment metrics", e);
            metrics.put("error", "Failed to collect metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    @Override
    public double getCanarySuccessRate(String serviceKey) {
        try {
            return deploymentMetrics.calculateCanarySuccessRate(serviceKey);
        } catch (Exception e) {
            log.warn("Failed to calculate canary success rate for service: {}", serviceKey, e);
            return 0.0;
        }
    }
    
    @Override
    public double getShadowAccuracyRate(String serviceKey, String strategy) {
        try {
            return deploymentMetrics.calculateAccuracyRate(serviceKey, strategy);
        } catch (Exception e) {
            log.warn("Failed to calculate shadow accuracy rate for service: {}", serviceKey, e);
            return 0.0;
        }
    }
    
    @Override
    public String getBlueGreenStatus(String serviceKey) {
        try {
            var serviceConfig = properties.getServices().get(serviceKey);
            if (serviceConfig != null && serviceConfig.getBlueGreen() != null) {
                return "active";
            }
            return "inactive";
        } catch (Exception e) {
            log.warn("Failed to get blue/green status for service: {}", serviceKey, e);
            return "error";
        }
    }
}

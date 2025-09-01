package com.n11.development.infrastructure.deployment;

import com.n11.development.core.strategy.DeploymentManager;
import com.n11.development.properties.MicroswitchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class DeploymentExecutor {

    private final DeploymentManager deploymentManager;

    @Autowired
    public DeploymentExecutor(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public <R> DeploymentResult<R> execute(DeploymentContext<R> context) {
        Instant startTime = Instant.now();
        
        try {
            log.debug("Starting deployment for service: {}, strategy: {}", 
                     context.getServiceKey(), context.getStrategyName());

            validateDeploymentParameters(context);

            R result = executeStrategy(context);

            String executedMethod = determineExecutedMethod(context, result);
            
            Instant endTime = Instant.now();
            
            log.debug("Deployment completed successfully for service: {}, method: {}, duration: {}ms", 
                     context.getServiceKey(), executedMethod, 
                     endTime.toEpochMilli() - startTime.toEpochMilli());
            
            return DeploymentResult.success(result, executedMethod, startTime, endTime);
            
        } catch (Exception e) {
            Instant endTime = Instant.now();
            
            log.error("Deployment failed for service: {}, strategy: {}", 
                     context.getServiceKey(), context.getStrategyName(), e);
            
            return DeploymentResult.failure(e.getMessage(), e, startTime, endTime);
        }
    }

    private <R> void validateDeploymentParameters(DeploymentContext<R> context) {
        if (context.getServiceKey() == null || context.getServiceKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Service key cannot be null or empty");
        }
        
        if (context.getStableMethod() == null) {
            throw new IllegalArgumentException("Stable method cannot be null");
        }
        
        if (context.getExperimentalMethod() == null) {
            throw new IllegalArgumentException("Experimental method cannot be null");
        }
    }

    private <R> R executeStrategy(DeploymentContext<R> context) {
        String strategyName = context.getStrategyName();
        String serviceKey = context.getServiceKey();
        
        log.debug("Executing strategy: {} for service: {}", strategyName, serviceKey);
        
        try {
            switch (strategyName.toLowerCase()) {
                case "canary":
                    return deploymentManager.canary(context.getStableMethod(), context.getExperimentalMethod(), serviceKey);
                case "shadow":
                    return deploymentManager.shadow(context.getStableMethod(), context.getExperimentalMethod(), serviceKey);
                case "blue-green":
                case "bluegreen":
                    return deploymentManager.blueGreen(context.getStableMethod(), context.getExperimentalMethod(), serviceKey);
                default:
                    log.warn("Unknown strategy: {}, falling back to stable method", strategyName);
                    return context.getStableMethod().get();
            }
        } catch (Exception e) {
            log.error("Strategy execution failed for service: {}, strategy: {}", serviceKey, strategyName, e);
            log.warn("Falling back to stable method");
            return context.getStableMethod().get();
        }
    }

    private <R> String determineExecutedMethod(DeploymentContext<R> context, R result) {
        try {
            R stableResult = context.getStableMethod().get();
            if (result.equals(stableResult)) {
                return "stable";
            } else {
                return "experimental";
            }
        } catch (Exception e) {
            log.warn("Could not determine executed method, defaulting to stable");
            return "stable";
        }
    }
}

package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.domain.InitializerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class Shadow extends DeployTemplate implements DeploymentStrategy {
    private static final Logger log = LoggerFactory.getLogger(Shadow.class);
    private static final byte DEFAULT_SHADOW_WEIGH = 1;
    private static Short weightCounter = (int) DEFAULT_SHADOW_WEIGH;

    protected Shadow(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        super(properties, deploymentMetrics);
    }

    @Override
    public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        if (serviceKey == null || serviceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Service key cannot be null or empty");
        }
        var serviceConfig = properties.getServices().get(serviceKey);
        if (serviceConfig == null || !serviceConfig.isEnabled()) {
            return primary.get();
        }
        
        var shadowConfig = serviceConfig.getShadow();
        if (shadowConfig == null || shadowConfig.getWeight() == null || shadowConfig.getWeight() <= 0) {
            return primary.get();
        }

        var weight = shadowConfig.getWeight();

        R result;
        boolean executedShadow = false;

        if (weight >= DEFAULT_SHADOW_WEIGH && weight.equals(weightCounter)) {
            weightCounter = (int) DEFAULT_SHADOW_WEIGH;
            result = executeAsyncSimultaneously(primary, secondary, serviceKey);
            executedShadow = true;
        } else {
            weightCounter++;
            result = executeJustFunc1(primary);
            executedShadow = false;
        }

        // Record metrics with proper error handling
        try {
            deploymentMetrics.recordSuccess(serviceKey, "stable", "shadow");
            if (executedShadow) {
                deploymentMetrics.recordSuccess(serviceKey, "shadow_execution", "shadow");
            }
        } catch (Exception e) {
            log.warn("Failed to record metrics for service: {} with strategy: shadow", serviceKey, e);
        }

        return result;
    }

    private static <R> R executeJustFunc1(Supplier<R> primary) {
        return primary.get();
    }

    private <R> R executeAsyncSimultaneously(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        var futureFunc1 = CompletableFuture.supplyAsync(primary);
        var futureFunc2 = CompletableFuture.supplyAsync(secondary);
        CompletableFuture.allOf(futureFunc1, futureFunc2).join();

        R result1 = futureFunc1.join();
        R result2 = null;
        try {
            result2 = futureFunc2.join();
        } catch (Exception e) {
            log.error("Shadow execution failed with an exception", e);
        }

        if (Objects.isNull(result2))
            log.warn("Shadow result is null. The shadow function may have thrown an exception or returned null.");
        else if (!result1.equals(result2))
            log.warn("Shadow result does not match original result.");

        recordShadowAccuracy(serviceKey, result1, result2);

        return result1;
    }

    private <R> void recordShadowAccuracy(String serviceKey, R result1, R result2) {
        try {
            if (result2 != null) {
                if (Objects.equals(result1, result2)) {
                    deploymentMetrics.recordSuccess(serviceKey, "shadow_accuracy", "shadow");
                } else {
                    deploymentMetrics.recordError(serviceKey, "shadow_mismatch", "shadow");
                }
            }
        } catch (Exception e) {
            log.debug("Failed to record shadow accuracy metrics", e);
        }
    }
}

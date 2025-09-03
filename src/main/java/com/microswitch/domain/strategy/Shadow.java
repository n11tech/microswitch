package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
public class Shadow extends DeployTemplate implements IDeploymentStrategy {
    private static final byte DEFAULT_SHADOW_WEIGH = 1;
    private static Short weightCounter = (int) DEFAULT_SHADOW_WEIGH;

    protected Shadow(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        super(properties, deploymentMetrics);
    }

    @Override
    public <R> R execute(Supplier<R> func1, Supplier<R> func2, String serviceKey) {
        var serviceConfig = properties.getServices().get(serviceKey);
        if (serviceConfig == null || !serviceConfig.isEnabled()) {
            return func1.get();
        }
        
        var shadowConfig = serviceConfig.getShadow();
        if (shadowConfig == null || shadowConfig.getWeight() == null || shadowConfig.getWeight() <= 0) {
            return func1.get();
        }

        var weight = shadowConfig.getWeight();

        R result;
        boolean executedShadow = false;

        if (weight >= DEFAULT_SHADOW_WEIGH && weight.equals(weightCounter)) {
            weightCounter = (int) DEFAULT_SHADOW_WEIGH;
            result = executeAsyncSimultaneously(func1, func2, serviceKey);
            executedShadow = true;
        } else {
            weightCounter++;
            result = executeJustFunc1(func1);
            executedShadow = false;
        }

        // Metrics kaydet - Shadow her zaman stable döner ama shadow execution yapar
        try {
            deploymentMetrics.recordSuccess(serviceKey, "stable", "shadow");
            if (executedShadow) {
                // Shadow execution yapıldığını kaydet
                deploymentMetrics.recordSuccess(serviceKey, "shadow_execution", "shadow");
            }
        } catch (Exception e) {
            // intentionally no-op
        }

        return result;
    }

    private static <R> R executeJustFunc1(Supplier<R> func1) {
        return func1.get();
    }

    private <R> R executeAsyncSimultaneously(Supplier<R> func1, Supplier<R> func2, String serviceKey) {
        var futureFunc1 = CompletableFuture.supplyAsync(func1);
        var futureFunc2 = CompletableFuture.supplyAsync(func2);
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

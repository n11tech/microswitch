package com.microswitch.domain.strategy;

import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.domain.InitializerConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
public class Shadow extends DeployTemplate implements DeploymentStrategy {
    private static final byte DEFAULT_SHADOW_WEIGH = 1;
    private Short weightCounter = (short) DEFAULT_SHADOW_WEIGH;

    public Shadow(InitializerConfiguration properties) {
        super(properties);
    }

    @Override
    public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        var serviceConfig = validateServiceAndGetConfig(serviceKey, primary);
        var primaryResult = executeIfServiceInvalid(serviceConfig, primary);
        if (primaryResult != null) {
            return primaryResult;
        }

        var shadowConfig = serviceConfig.getShadow();
        if (shadowConfig == null || shadowConfig.getWeight() == null || shadowConfig.getWeight() <= 0) {
            return primary.get();
        }

        var weight = shadowConfig.getWeight();

        if (weight.equals(weightCounter)) {
            weightCounter = (short) DEFAULT_SHADOW_WEIGH;
            return executeAsyncSimultaneously(primary, secondary, serviceKey);
        } else {
            weightCounter++;
            return executeJustPrimary(primary);
        }
    }

    private static <R> R executeJustPrimary(Supplier<R> primary) {
        return primary.get();
    }

    private <R> R executeAsyncSimultaneously(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        var futurePrimary = CompletableFuture.supplyAsync(primary);
        var futureSecondary = CompletableFuture.supplyAsync(secondary);
        CompletableFuture.allOf(futurePrimary, futureSecondary).join();

        R result1 = futurePrimary.join();
        R result2 = null;
        try {
            result2 = futureSecondary.join();
        } catch (Exception e) {
            log.error("Shadow execution failed with an exception", e);
        }

        if (Objects.isNull(result2))
            log.warn("Shadow result is null. The shadow function may have thrown an exception or returned null.");
        else if (!result1.equals(result2))
            log.warn("Shadow result does not match original result.");

        return result1;
    }
}

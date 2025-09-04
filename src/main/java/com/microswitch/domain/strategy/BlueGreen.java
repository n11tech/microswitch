package com.microswitch.domain.strategy;

import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.domain.InitializerConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class BlueGreen extends DeployTemplate implements DeploymentStrategy {
    private final Instant startTime = Instant.now();

    public BlueGreen(InitializerConfiguration properties) {
        super(properties);
    }

    @Override
    public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        var serviceConfig = validateServiceAndGetConfig(serviceKey, primary);
        var primaryResult = executeIfServiceInvalid(serviceConfig, primary);
        if (primaryResult != null) {
            return primaryResult;
        }

        var blueGreenConfig = serviceConfig.getBlueGreen();
        if (blueGreenConfig == null) {
            return primary.get();
        }

        var primaryPercentage = blueGreenConfig.getPrimaryPercentage();
        if (primaryPercentage == null) {
            return primary.get();
        }

        var weights = service.getSpec().getDeployment().getWeight().split("/");
        var func1Weight = Integer.parseInt(weights[0]);
        var func2Weight = Integer.parseInt(weights[1]);

        if (func1Weight + func2Weight != 1)
            throw new IllegalArgumentException("Weights must sum to 1");

        var ttl = service.getSpec().getDeployment().getTtl();

        if (ttl > 0) {
            if (Duration.between(startTime, Instant.now()).toSeconds() < ttl)
                return func1Weight == 1 ? func1.apply(input) : func2.apply(input);
            return func1Weight == 1 ? func2.apply(input) : func1.apply(input);
        } else {
            return func1Weight == 1 ? func1.apply(input) : func2.apply(input);
        }
    }
}


@Slf4j
public class BlueGreen extends DeployTemplate implements DeploymentStrategy {
    private final Instant startTime = Instant.now();

    private record CalculationResult<R>(R result, boolean usedExperimental) {
    }

    public BlueGreen(InitializerConfiguration properties) {
        super(properties);
    }

    @Override
    public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        var serviceConfig = validateServiceAndGetConfig(serviceKey, primary);
        var primaryResult = executeIfServiceInvalid(serviceConfig, primary);
        if (primaryResult != null) {
            return primaryResult;
        }

        var blueGreenConfig = serviceConfig.getBlueGreen();
        if (blueGreenConfig == null) {
            return primary.get();
        }

        var primaryPercentage = blueGreenConfig.getPrimaryPercentage();
        if (primaryPercentage == null) {
            return primary.get();
        }

        if (primaryPercentage < 0 || primaryPercentage > 100)
            throw new IllegalArgumentException("Stable percentage must be between 0 and 100.");

        var ttl = blueGreenConfig.getTtl();
        if (ttl != null && ttl > 0) {
            if (Duration.between(startTime, Instant.now()).toSeconds() < ttl) {
                return calculateResult(primary, secondary, primaryPercentage).result;
            } else {
                return secondary.get();
            }
        } else {
            return calculateResult(primary, secondary, primaryPercentage).result;
        }
    }

    private <R> CalculationResult<R> calculateResult(Supplier<R> primary, Supplier<R> secondary, int primaryPercentage) {
        if (primaryPercentage != 100) {
            if (primaryPercentage == 0) {
                return new CalculationResult<>(secondary.get(), true);
            } else {
                double random = Math.random() * 100;
                if (random < primaryPercentage) {
                    return new CalculationResult<>(primary.get(), false);
                } else {
                    return new CalculationResult<>(secondary.get(), true);
                }
            }
        } else {
            return new CalculationResult<>(primary.get(), false);
        }
    }
}


package com.n11.development.core.strategy;

import com.n11.development.infrastructure.metrics.DeploymentMetrics;
import com.n11.development.properties.MicroswitchProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class BlueGreen extends DeployTemplate implements IDeploymentStrategy {
    private final Instant startTime = Instant.now();

    protected BlueGreen(MicroswitchProperties properties, DeploymentMetrics deploymentMetrics) {
        super(properties, deploymentMetrics);
    }

    @Override
    public <R> R execute(Supplier<R> func1, Supplier<R> func2, String serviceKey) {
        var serviceConfig = properties.getServices().get(serviceKey);
        if (serviceConfig == null || !serviceConfig.isEnabled()) {
            return func1.get();
        }

        var blueGreenConfig = serviceConfig.getBlueGreen();
        if (blueGreenConfig == null) {
            return func1.get();
        }

        var primaryPercentage = blueGreenConfig.getPrimaryPercentage();
        if (primaryPercentage == null) {
            return func1.get();
        }

        if (primaryPercentage < 0 || primaryPercentage > 100)
            throw new IllegalArgumentException("Stable percentage must be between 0 and 100.");

        var ttl = blueGreenConfig.getTtl();

        R result;
        boolean usedExperimental = false;
        
        if (ttl != null && ttl > 0) {
            if (Duration.between(startTime, Instant.now()).toSeconds() < ttl) {
                var calculationResult = calculateResultWithMetrics(func1, func2, primaryPercentage);
                result = calculationResult.result;
                usedExperimental = calculationResult.usedExperimental;
            } else {
                result = func2.get();
                usedExperimental = true;
            }
        } else {
            var calculationResult = calculateResultWithMetrics(func1, func2, primaryPercentage);
            result = calculationResult.result;
            usedExperimental = calculationResult.usedExperimental;
        }

        // Metrics kaydet
        try {
            if (usedExperimental) {
                deploymentMetrics.recordSuccess(serviceKey, "experimental", "bluegreen");
            } else {
                deploymentMetrics.recordSuccess(serviceKey, "stable", "bluegreen");
            }
        } catch (Exception e) {
            // intentionally no-op
        }

        return result;
    }
    
    private <R> CalculationResult<R> calculateResultWithMetrics(Supplier<R> func1, Supplier<R> func2, int primaryPercentage) {
        if (primaryPercentage == 100) {
            return new CalculationResult<>(func1.get(), false);
        } else if (primaryPercentage == 0) {
            return new CalculationResult<>(func2.get(), true);
        } else {
            double random = Math.random() * 100;
            if (random < primaryPercentage) {
                return new CalculationResult<>(func1.get(), false);
            } else {
                return new CalculationResult<>(func2.get(), true);
            }
        }
    }
    
    private static class CalculationResult<R> {
        final R result;
        final boolean usedExperimental;
        
        CalculationResult(R result, boolean usedExperimental) {
            this.result = result;
            this.usedExperimental = usedExperimental;
        }
    }
}


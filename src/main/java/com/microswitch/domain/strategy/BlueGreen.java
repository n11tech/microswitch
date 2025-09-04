package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class BlueGreen extends DeployTemplate implements DeploymentStrategy {
    private static final Logger log = LoggerFactory.getLogger(BlueGreen.class);
    private final Instant startTime = Instant.now();

    protected BlueGreen(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
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

        R result;
        boolean usedExperimental = false;
        
        if (ttl != null && ttl > 0) {
            if (Duration.between(startTime, Instant.now()).toSeconds() < ttl) {
                var calculationResult = calculateResultWithMetrics(primary, secondary, primaryPercentage);
                result = calculationResult.result;
                usedExperimental = calculationResult.usedExperimental;
            } else {
                result = secondary.get();
                usedExperimental = true;
            }
        } else {
            var calculationResult = calculateResultWithMetrics(primary, secondary, primaryPercentage);
            result = calculationResult.result;
            usedExperimental = calculationResult.usedExperimental;
        }

        // Record metrics with proper error handling
        try {
            if (usedExperimental) {
                deploymentMetrics.recordSuccess(serviceKey, "experimental", "bluegreen");
            } else {
                deploymentMetrics.recordSuccess(serviceKey, "stable", "bluegreen");
            }
        } catch (Exception e) {
            log.warn("Failed to record metrics for service: {} with strategy: bluegreen", serviceKey, e);
        }

        return result;
    }
    
    private <R> CalculationResult<R> calculateResultWithMetrics(Supplier<R> primary, Supplier<R> secondary, int primaryPercentage) {
        if (primaryPercentage == 100) {
            return new CalculationResult<>(primary.get(), false);
        } else if (primaryPercentage == 0) {
            return new CalculationResult<>(secondary.get(), true);
        } else {
            double random = Math.random() * 100;
            if (random < primaryPercentage) {
                return new CalculationResult<>(primary.get(), false);
            } else {
                return new CalculationResult<>(secondary.get(), true);
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


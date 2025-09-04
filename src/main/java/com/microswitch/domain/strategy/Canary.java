package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class Canary extends DeployTemplate implements DeploymentStrategy {
    private static final Logger log = LoggerFactory.getLogger(Canary.class);
    
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile int totalCalls = 10;
    private volatile UniqueRandomGenerator uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);

    protected Canary(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
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
        
        var canaryConfig = serviceConfig.getCanary();
        if (canaryConfig == null) {
            return primary.get();
        }

        int callsForFunc1Method = getCallsForFunc1Method(canaryConfig);
        var algorithm = canaryConfig.getAlgorithm();

        boolean usedExperimental;
        R result;
        if (Objects.equals(algorithm, AlgorithmType.RANDOM.getValue())) {
            var executionResult = executeRandomWithMetrics(primary, secondary, callsForFunc1Method);
            result = executionResult.result;
            usedExperimental = executionResult.usedExperimental;
        } else {
            var executionResult = executeSequenceWithMetrics(primary, secondary, callsForFunc1Method);
            result = executionResult.result;
            usedExperimental = executionResult.usedExperimental;
        }

        // Record metrics with proper error handling
        try {
            if (usedExperimental) {
                deploymentMetrics.recordSuccess(serviceKey, "experimental", "canary");
            } else {
                deploymentMetrics.recordSuccess(serviceKey, "stable", "canary");
            }
        } catch (Exception e) {
            log.warn("Failed to record metrics for service: {} with strategy: canary", serviceKey, e);
        }

        return result;
    }

    private int getCallsForFunc1Method(InitializerConfiguration.Canary canaryConfig) {
        var primaryPercentage = canaryConfig.getPrimaryPercentage() == null ? 100 : canaryConfig.getPrimaryPercentage();
        
        if (primaryPercentage < 0 || primaryPercentage > 100) {
            throw new IllegalArgumentException("Primary percentage must be between 0 and 100");
        }

        var func1Percentage = primaryPercentage;
        var func2Percentage = 100 - primaryPercentage;

        totalCalls = 100 / gcd(func1Percentage, func2Percentage);
        return (totalCalls * func1Percentage) / 100;
    }
    
    private <R> ExecutionResult<R> executeSequenceWithMetrics(Supplier<R> primary, Supplier<R> secondary, int callsForFunc1Method) {
        int currentCount = counter.getAndUpdate(c -> (c + 1) % totalCalls);
        if (currentCount < callsForFunc1Method) {
            return new ExecutionResult<>(primary.get(), false);
        } else {
            return new ExecutionResult<>(secondary.get(), true);
        }
    }

    private <R> ExecutionResult<R> executeRandomWithMetrics(Supplier<R> primary, Supplier<R> secondary, int callsForFunc1Method) {
        synchronized (this) {
            if (uniqueRandomGenerator.getUniqueValues().size() != totalCalls) {
                uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);
            }
        }
        
        if (uniqueRandomGenerator.getNextUniqueRandomValue() < callsForFunc1Method) {
            return new ExecutionResult<>(primary.get(), false);
        } else {
            return new ExecutionResult<>(secondary.get(), true);
        }
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static class UniqueRandomGenerator {
        private final List<Integer> uniqueValues;
        private int index = 0;

        public UniqueRandomGenerator(int range) {
            uniqueValues = new ArrayList<>(range);
            for (int i = 0; i < range; i++) {
                uniqueValues.add(i);
            }
            shuffleValues();
        }

        public List<Integer> getUniqueValues() {
            return uniqueValues;
        }

        public int getNextUniqueRandomValue() {
            if (index >= uniqueValues.size()) {
                shuffleValues();
                index = 0;
            }
            return uniqueValues.get(index++);
        }

        private void shuffleValues() {
            Collections.shuffle(uniqueValues, new Random());
        }
    }
    
    private static class ExecutionResult<R> {
        final R result;
        final boolean usedExperimental;
        
        ExecutionResult(R result, boolean usedExperimental) {
            this.result = result;
            this.usedExperimental = usedExperimental;
        }
    }
}

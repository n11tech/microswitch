package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

public class Canary extends DeployTemplate implements IDeploymentStrategy {
    private int counter = 0;
    private int totalCalls = 10;
    private UniqueRandomGenerator uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);

    protected Canary(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        super(properties, deploymentMetrics);
    }

    @Override
    public <R> R execute(Supplier<R> func1, Supplier<R> func2, String serviceKey) {
        var serviceConfig = properties.getServices().get(serviceKey);
        if (serviceConfig == null || !serviceConfig.isEnabled()) {
            return func1.get();
        }
        
        var canaryConfig = serviceConfig.getCanary();
        if (canaryConfig == null) {
            return func1.get();
        }

        int callsForFunc1Method = getCallsForFunc1Method(canaryConfig);
        var algorithm = canaryConfig.getAlgorithm();

        boolean usedExperimental;
        R result;
        if (Objects.equals(algorithm, AlgorithmType.RANDOM.getValue())) {
            var executionResult = executeRandomWithMetrics(func1, func2, callsForFunc1Method);
            result = executionResult.result;
            usedExperimental = executionResult.usedExperimental;
        } else {
            var executionResult = executeSequenceWithMetrics(func1, func2, callsForFunc1Method);
            result = executionResult.result;
            usedExperimental = executionResult.usedExperimental;
        }

        // Metrics kaydet
        try {
            if (usedExperimental) {
                deploymentMetrics.recordSuccess(serviceKey, "experimental", "canary");
            } else {
                deploymentMetrics.recordSuccess(serviceKey, "stable", "canary");
            }
        } catch (Exception e) {
            // intentionally no-op
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
    
    private <R> ExecutionResult<R> executeSequenceWithMetrics(Supplier<R> func1, Supplier<R> func2, int callsForFunc1Method) {
        counter = (counter + 1) % totalCalls;
        if (counter < callsForFunc1Method) {
            return new ExecutionResult<>(func1.get(), false);
        } else {
            return new ExecutionResult<>(func2.get(), true);
        }
    }

    private <R> ExecutionResult<R> executeRandomWithMetrics(Supplier<R> func1, Supplier<R> func2, int callsForFunc1Method) {
        if (uniqueRandomGenerator.getUniqueValues().size() != totalCalls)
            uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);
        
        if (uniqueRandomGenerator.getNextUniqueRandomValue() < callsForFunc1Method) {
            return new ExecutionResult<>(func1.get(), false);
        } else {
            return new ExecutionResult<>(func2.get(), true);
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

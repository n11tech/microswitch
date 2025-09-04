package com.microswitch.domain.strategy;

import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.application.random.UniqueRandomGenerator;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.value.AlgorithmType;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
public class Canary extends DeployTemplate implements DeploymentStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile int totalCalls = 10;
    private UniqueRandomGenerator uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);

    private record ExecutionResult<R>(R result, boolean usedExperimental) {
    }

    protected Canary(InitializerConfiguration properties) {
        super(properties);
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

        if (Objects.equals(algorithm, AlgorithmType.RANDOM.getValue())) {
            return executeRandom(primary, secondary, callsForFunc1Method).result;
        } else {
            return executeSequence(primary, secondary, callsForFunc1Method).result;
        }
    }

    private int getCallsForFunc1Method(InitializerConfiguration.Canary canaryConfig) {
        var primaryPercentage = canaryConfig.getPrimaryPercentage() == null ? 100 : canaryConfig.getPrimaryPercentage();

        if (primaryPercentage < 0 || primaryPercentage > 100) {
            throw new IllegalArgumentException("Primary percentage must be between 0 and 100");
        }

        var secondaryPercentage = 100 - primaryPercentage;

        totalCalls = 100 / gcd(primaryPercentage, secondaryPercentage);
        return (totalCalls * primaryPercentage) / 100;
    }

    private <R> ExecutionResult<R> executeSequence(Supplier<R> primary, Supplier<R> secondary, int callsForFunc1Method) {
        int currentCount = counter.getAndUpdate(c -> (c + 1) % totalCalls);
        if (currentCount < callsForFunc1Method) {
            return new ExecutionResult<>(primary.get(), false);
        } else {
            return new ExecutionResult<>(secondary.get(), true);
        }
    }

    private <R> ExecutionResult<R> executeRandom(Supplier<R> primary, Supplier<R> secondary, int callsForFunc1Method) {
        int randomValue;
        synchronized (this) {
            if (uniqueRandomGenerator.getUniqueValues().size() != totalCalls) {
                uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);
            }
            randomValue = uniqueRandomGenerator.getNextUniqueRandomValue();
        }

        if (randomValue < callsForFunc1Method) {
            return new ExecutionResult<>(primary.get(), false);
        } else {
            return new ExecutionResult<>(secondary.get(), true);
        }
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}

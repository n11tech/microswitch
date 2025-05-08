package com.n11.development.infrastructure.strategy;

import com.n11.development.application.config.EmbDeployer;
import com.n11.development.application.config.IEmbDeployerLoader;
import com.n11.development.application.random.UniqueRandomGenerator;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Function;

@Component
public class Canary extends DeployTemplate implements IDeploymentStrategy {
    private int counter = 0;
    private int totalCalls = 10;
    private UniqueRandomGenerator uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);

    protected Canary(IEmbDeployerLoader embDeployerLoader) {
        super(embDeployerLoader);
    }

    @Override
    public <T, R> R execute(Function<T, R> func1, Function<T, R> func2, String domain, String serviceKey, T input) {
        var service = getEmbeddedService(domain, serviceKey, StrategyType.CANARY);
        if (!deployScopeIsEnabled(service)) return func1.apply(input);
        int callsForFunc1Method = getCallsForFunc1Method(service);
        var algorithm = service.getSpec().getDeployment().getAlgorithm() == null ? AlgorithmType.SEQUENCE.getValue() : service.getSpec().getDeployment().getAlgorithm();
        if (Objects.equals(algorithm, AlgorithmType.SEQUENCE.getValue()))
            return executeSequence(func1, func2, input, callsForFunc1Method);
        if (Objects.equals(algorithm, AlgorithmType.RANDOM.getValue()))
            return executeRandom(func1, func2, input, callsForFunc1Method);
        return func1.apply(input);
    }

    private int getCallsForFunc1Method(EmbDeployer.ServiceConfig service) {
        var canaryPercentage = service.getSpec().getDeployment().getCanaryPercentage() == null ? "100/0" : service.getSpec().getDeployment().getCanaryPercentage();
        var percentages = canaryPercentage.split("/");
        if (percentages.length < 2)
            throw new IllegalArgumentException("Invalid Canary Percentage");
        var func1Percentage = Integer.parseInt(percentages[0]);
        var func2Percentage = Integer.parseInt(percentages[1]);

        if (func1Percentage + func2Percentage != 100)
            throw new IllegalArgumentException("Percentages must sum to 100");

        totalCalls = 100 / gcd(func1Percentage, func2Percentage);
        return (totalCalls * func1Percentage) / 100;
    }

    private <T, R> R executeSequence(Function<T, R> func1, Function<T, R> func2, T input, int callsForFunc1Method) {
        counter = (counter + 1) % totalCalls;
        return counter < callsForFunc1Method ? func1.apply(input) : func2.apply(input);
    }

    private <T, R> R executeRandom(Function<T, R> func1, Function<T, R> func2, T input, int callsForFunc1Method) {
        if (uniqueRandomGenerator.getUniqueValues().size() != totalCalls)
            uniqueRandomGenerator = new UniqueRandomGenerator(totalCalls);
        return uniqueRandomGenerator.getNextUniqueRandomValue() < callsForFunc1Method ? func1.apply(input) : func2.apply(input);
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}

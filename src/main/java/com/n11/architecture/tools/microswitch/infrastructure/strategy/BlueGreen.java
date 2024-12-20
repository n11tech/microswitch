package com.n11.architecture.tools.microswitch.infrastructure.strategy;

import com.n11.architecture.tools.microswitch.application.config.IEmbDeployerLoader;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

@Component
public class BlueGreen extends DeployTemplate implements IDeploymentStrategy {
    private final Instant startTime = Instant.now();

    protected BlueGreen(IEmbDeployerLoader embDeployerLoader) {
        super(embDeployerLoader);
    }

    @Override
    public <T, R> R execute(Function<T, R> func1,
                            Function<T, R> func2,
                            String domain,
                            String serviceKey,
                            T input) {
        var service = getEmbeddedService(domain, serviceKey, StrategyType.BLUE_GREEN);
        if (!deployScopeIsEnabled(service)) return func1.apply(input);
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

package com.n11.architecture.tools.microswitch.infrastructure.strategy;

import com.n11.architecture.tools.microswitch.application.config.EmbDeployer;
import com.n11.architecture.tools.microswitch.application.config.IEmbDeployerLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
@Component
public class Shadow extends DeployTemplate implements IDeploymentStrategy {
    private static final byte DEFAULT_SHADOW_WEIGH = 1;
    private static Short _weightCounter = (int) DEFAULT_SHADOW_WEIGH;

    protected Shadow(IEmbDeployerLoader embDeployerLoader) {
        super(embDeployerLoader);
    }

    @Override
    public <T, R> R execute(Function<T, R> func1,
                            Function<T, R> func2,
                            String domain,
                            String serviceKey,
                            T input) {
        var service = getEmbeddedService(domain, serviceKey, StrategyType.SHADOW);
        if (!deployScopeIsEnabled(service)) return func1.apply(input);
        var weight = getShadowWeight(service);
        if (weight >= DEFAULT_SHADOW_WEIGH && weight.equals(_weightCounter)) {
            _weightCounter = (int) DEFAULT_SHADOW_WEIGH;
            return executeAsyncSimultaneously(func1, func2, input);
        }
        _weightCounter++;
        return executeJustFunc1(func1, input);
    }

    private static <T, R> R executeJustFunc1(Function<T, R> func1, T input) {
        return func1.apply(input);
    }

    private static <T, R> R executeAsyncSimultaneously(Function<T, R> func1, Function<T, R> func2, T input) {
        var futureFunc1 = CompletableFuture.supplyAsync(() -> func1.apply(input));
        var futureFunc2 = CompletableFuture.supplyAsync(() -> func2.apply(input));
        CompletableFuture.allOf(futureFunc1, futureFunc2).join();

        R result1 = futureFunc1.join();
        R result2 = futureFunc2.join();

        if (Objects.isNull(result2))
            log.info("shadow result is null");
        if (!result1.equals(result2))
            log.warn("shadow result not equals origin");

        return result1;
    }

    private Short getShadowWeight(EmbDeployer.ServiceConfig service) {
        return Objects.nonNull(service.getMetadata()) && service.getSpec().getDeployment().getShadowWeight() != null ? service.getSpec().getDeployment().getShadowWeight() : 0;
    }
}

package com.n11.development.infrastructure;


import com.n11.development.infrastructure.strategy.StrategyType;

import java.util.List;
import java.util.function.Function;

public interface IEmbeddedDeploy {
    /**
     * Set the domain for the deployment.
     *
     * @param domain The domain name
     * @return The current instance of EmbeddedDeploy for chaining
     */
    EmbeddedDeploy setExecutableService(String domain, String serviceKey);

    <T, R> R execute(StrategyType strategyType, List<Function<T, R>> funcs, T input);
}

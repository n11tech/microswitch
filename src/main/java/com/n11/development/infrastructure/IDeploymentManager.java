package com.n11.development.infrastructure;

import java.util.List;
import java.util.function.Function;

public interface IDeploymentManager {
    void setStrategy(String strategyType);

    <T, R> R executeDeploy(List<Function<T, R>> funcs, String domain, String serviceKey, T input);
}

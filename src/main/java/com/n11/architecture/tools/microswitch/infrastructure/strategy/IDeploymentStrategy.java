package com.n11.architecture.tools.microswitch.infrastructure.strategy;

import java.util.function.Function;

interface IDeploymentStrategy {
    <T, R> R execute(Function<T, R> func1, Function<T, R> func2, String domain, String serviceKey, T input);
}

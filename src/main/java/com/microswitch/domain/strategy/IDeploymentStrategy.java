package com.microswitch.domain.strategy;

import java.util.function.Supplier;

interface IDeploymentStrategy {
    <R> R execute(Supplier<R> func1, Supplier<R> func2, String serviceKey);
}

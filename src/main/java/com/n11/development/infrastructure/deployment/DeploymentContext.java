package com.n11.development.infrastructure.deployment;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.function.Supplier;

@Data
@Builder
public class DeploymentContext<R> {

    private String serviceKey;

    private String strategyName;

    private Supplier<R> stableMethod;

    private Supplier<R> experimentalMethod;

    private Instant startTime;


}

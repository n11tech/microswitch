package com.microswitch.application.metric;

import lombok.extern.slf4j.Slf4j;

/**
 * No-operation implementation of DeploymentMetrics.
 * Used when no MeterRegistry is available to prevent null pointer exceptions
 * while maintaining the same API contract.
 */

@Slf4j
public class NoOpDeploymentMetrics extends DeploymentMetrics {

    public NoOpDeploymentMetrics() {
        super(null);
        log.debug("NoOpDeploymentMetrics initialized - metrics recording disabled");
    }

    @Override
    public void recordSuccess(String serviceKey, String version, String strategy) {
        // No-op: metrics recording disabled
        log.trace("NoOp recordSuccess called for service: {}, version: {}, strategy: {}",
                serviceKey, version, strategy);
    }

    @Override
    public void recordError(String serviceKey, String version, String strategy) {
        // No-op: metrics recording disabled
        log.trace("NoOp recordError called for service: {}, version: {}, strategy: {}",
                serviceKey, version, strategy);
    }

    @Override
    public double calculateAccuracyRate(String serviceKey, String strategy) {
        // No-op: return neutral value
        return 0.0;
    }

    @Override
    public double calculateCanarySuccessRate(String serviceKey) {
        // No-op: return neutral value
        return 0.0;
    }
}

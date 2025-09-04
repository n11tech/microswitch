package com.microswitch.application.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DeploymentMetrics {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    public DeploymentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccess(String serviceKey, String version, String strategy) {
        String key = createMetricKey(serviceKey, version, strategy);
        Counter counter = successCounters.computeIfAbsent(key, k -> 
            Counter.builder("deployment.success")
                .tag("service", serviceKey)
                .tag("version", version)
                .tag("strategy", strategy)
                .register(meterRegistry)
        );
        counter.increment();
        log.debug("Success recorded for service: {}, version: {}, strategy: {}", serviceKey, version, strategy);
    }

    public void recordError(String serviceKey, String version, String strategy) {
        String key = createMetricKey(serviceKey, version, strategy);
        Counter counter = errorCounters.computeIfAbsent(key, k -> 
            Counter.builder("deployment.error")
                .tag("service", serviceKey)
                .tag("version", version)
                .tag("strategy", strategy)
                .register(meterRegistry)
        );
        counter.increment();
        log.debug("Error recorded for service: {}, version: {}, strategy: {}", serviceKey, version, strategy);
    }

    public double calculateAccuracyRate(String serviceKey, String strategy) {
        String stableKey = createMetricKey(serviceKey, "stable", strategy);
        String experimentalKey = createMetricKey(serviceKey, "experimental", strategy);
        
        long stableSuccess = getCounterValue(successCounters.get(stableKey));
        long stableErrors = getCounterValue(errorCounters.get(stableKey));
        long experimentalSuccess = getCounterValue(successCounters.get(experimentalKey));
        long experimentalErrors = getCounterValue(errorCounters.get(experimentalKey));
        
        long totalStable = stableSuccess + stableErrors;
        long totalExperimental = experimentalSuccess + experimentalErrors;
        
        if (totalStable == 0 && totalExperimental == 0) {
            return 0.0;
        }
        
        double stableAccuracy = totalStable > 0 ? (double) stableSuccess / totalStable * 100 : 0.0;
        double experimentalAccuracy = totalExperimental > 0 ? (double) experimentalSuccess / totalExperimental * 100 : 0.0;
        
        if (totalStable == 0) return experimentalAccuracy;
        if (totalExperimental == 0) return stableAccuracy;
        
        return (stableAccuracy + experimentalAccuracy) / 2.0;
    }

    public double calculateCanarySuccessRate(String serviceKey) {
        return calculateAccuracyRate(serviceKey, "canary");
    }
    
    private String createMetricKey(String serviceKey, String version, String strategy) {
        return serviceKey + ":" + version + ":" + strategy;
    }
    
    private long getCounterValue(Counter counter) {
        return counter != null ? (long) counter.count() : 0L;
    }
}

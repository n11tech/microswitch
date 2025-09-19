package com.microswitch.domain.strategy;

import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.util.DeepObjectComparator;
import com.microswitch.domain.value.MethodType;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
public class Shadow extends DeployTemplate implements DeploymentStrategy {
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final ExecutorService shadowExecutor;
    private volatile boolean isShutdown = false;
    private final DeepObjectComparator comparator;
    private final boolean deepComparisonEnabled;

    public Shadow(InitializerConfiguration properties) {
        super(properties);
        this.shadowExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("shadow-virtual-", 0).factory()
        );
        
        this.deepComparisonEnabled = checkIfDeepComparisonEnabled(properties);
        
        if (this.deepComparisonEnabled) {
            var cmpCfg = resolveComparatorConfig(properties);
            DeepObjectComparator.Builder builder = DeepObjectComparator.builder()
                    .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                    .withMaxDepth(10)
                    .compareNullsAsEqual(false)
                    .ignoreFields("timestamp", "requestId", "traceId");

            if (cmpCfg != null) {
                builder = builder
                        .withMaxCollectionElements(cmpCfg.getMaxCollectionElements())
                        .withMaxCompareTimeMillis(cmpCfg.getMaxCompareTimeMillis())
                        .enableSamplingOnHuge(cmpCfg.isEnableSamplingOnHuge())
                        .withStride(cmpCfg.getStride())
                        .withMaxFieldsPerClass(cmpCfg.getMaxFieldsPerClass());
            }

            this.comparator = builder.build();
        } else {
            this.comparator = null; 
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    private boolean checkIfDeepComparisonEnabled(InitializerConfiguration properties) {
        if (properties.getServices() != null) {
            for (var service : properties.getServices().values()) {
                if (service.getShadow() != null && 
                    "enable".equalsIgnoreCase(service.getShadow().getComparatorMode())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolve comparator configuration from the first service that has deep comparison enabled.
     * If multiple services enable it, the first encountered configuration will be used as global defaults
     * for the comparator instance. This keeps initialization simple while allowing tuning via config.
     */
    private InitializerConfiguration.Shadow.Comparator resolveComparatorConfig(InitializerConfiguration properties) {
        if (properties.getServices() == null) {
            return null;
        }
        for (var entry : properties.getServices().entrySet()) {
            var service = entry.getValue();
            if (service == null || service.getShadow() == null) continue;
            var shadow = service.getShadow();
            if ("enable".equalsIgnoreCase(shadow.getComparatorMode())) {
                return shadow.getComparator();
            }
        }
        return null;
    }
    
    private boolean isDeepComparisonEnabledForService(String serviceKey) {
        if (configuration.getServices() != null) {
            var service = configuration.getServices().get(serviceKey);
            if (service != null && service.getShadow() != null) {
                return "enable".equalsIgnoreCase(service.getShadow().getComparatorMode());
            }
        }
        return false;
    }

    @Override
    public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        var serviceConfig = validateServiceAndGetConfig(serviceKey, primary);
        var primaryResult = executeIfServiceInvalid(serviceConfig, primary);
        if (primaryResult != null) {
            return primaryResult;
        }

        var shadowConfig = serviceConfig.getShadow();
        
        if (shadowConfig == null || shadowConfig.getMirrorPercentage() == null || shadowConfig.getMirrorPercentage() <= 0) {
            if (shadowConfig == null) {
                return primary.get();
            }
            return executeStableMethod(primary, secondary, shadowConfig);
        }

        var mirrorPercentage = shadowConfig.getMirrorPercentage();

        int currentRequest = requestCounter.incrementAndGet();
        int interval = 100 / mirrorPercentage;

        if (currentRequest % interval == 0) {
            return executeAsyncSimultaneously(primary, secondary, shadowConfig, serviceKey);
        } else {
            return executeStableMethod(primary, secondary, shadowConfig);
        }
    }

    private <R> R executeStableMethod(Supplier<R> primary, Supplier<R> secondary, InitializerConfiguration.Shadow shadowConfig) {
        var stableMethod = shadowConfig.getStable();
        if (stableMethod == MethodType.PRIMARY) {
            return primary.get();
        } else {
            return secondary.get();
        }
    }

    private <R> R executeAsyncSimultaneously(Supplier<R> primary, Supplier<R> secondary, InitializerConfiguration.Shadow shadowConfig, String serviceKey) {
        if (isShutdown) {
            log.warn("Shadow executor is shutdown, falling back to stable method");
            return executeStableMethod(primary, secondary, shadowConfig);
        }

        Supplier<R> stableSupplier = (shadowConfig.getStable() == MethodType.PRIMARY) ? primary : secondary;
        Supplier<R> mirrorSupplier = (shadowConfig.getMirror() == MethodType.PRIMARY) ? primary : secondary;

        CompletableFuture<R> futureStable = CompletableFuture.supplyAsync(stableSupplier, shadowExecutor);
        CompletableFuture<R> futureMirror = CompletableFuture.supplyAsync(mirrorSupplier, shadowExecutor);

        try {
            CompletableFuture.allOf(futureStable, futureMirror)
                    .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();

            R stableResult = futureStable.join();

            R mirrorResult = futureMirror.handle((result, throwable) -> {
                if (throwable != null) {
                    log.warn("Mirror execution failed: {}", throwable.getMessage());
                    return null;
                }
                return result;
            }).join();

            if (Objects.isNull(mirrorResult)) {
                log.warn("Shadow result is null. The shadow function may have thrown an exception or returned null.");
            } else {
                boolean useDeepComparison = isDeepComparisonEnabledForService(serviceKey);
                
                boolean resultsMatch;
                if (useDeepComparison && comparator != null) {
                    resultsMatch = comparator.areEqual(stableResult, mirrorResult);
                    if (!resultsMatch) {
                        log.warn("Shadow result does not match stable result for service: {}. " +
                                "Deep comparison detected differences in object fields.", serviceKey);
                    } else {
                        log.info("Shadow execution successful - results match for service: {} " +
                                "(deep comparison validated)", serviceKey);
                    }
                } else {
                    // Use standard equals() method
                    resultsMatch = stableResult.equals(mirrorResult);
                    if (!resultsMatch) {
                        log.warn("Shadow result does not match stable result for service: {} " +
                                "(using standard equals)", serviceKey);
                    } else {
                        log.info("Shadow execution successful - results match for service: {} " +
                                "(standard comparison)", serviceKey);
                    }
                }
                
                if (!resultsMatch && log.isDebugEnabled()) {
                    log.debug("Stable result: {}, Mirror result: {}", stableResult, mirrorResult);
                }
            }

            return stableResult;

        } catch (CompletionException e) {
            log.error("Shadow execution timeout or failure for service {}: {}", serviceKey, e.getMessage());
            return executeStableMethod(primary, secondary, shadowConfig);
        }
    }

    /**
     * Gracefully shutdown the shadow executor
     */
    private void shutdown() {
        if (!isShutdown) {
            isShutdown = true;
            shadowExecutor.shutdown();
            try {
                if (!shadowExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    shadowExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                shadowExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("Shadow executor shutdown completed");
        }
    }
}

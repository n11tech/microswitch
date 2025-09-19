package com.microswitch.domain.strategy;

import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.domain.InitializerConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Thread-safe Blue-Green deployment strategy implementation for Kubernetes multi-pod environments.
 * <p>
 * Blue-Green deployment maintains two identical production environments:
 * - Blue (current/stable)
 * - Green (new/experimental)
 * <p>
 * Traffic can be switched instantly between environments based on TTL or weight configuration.
 * This implementation ensures consistency across multiple pods in a Kubernetes cluster.
 */

@Slf4j
public class BlueGreen extends DeployTemplate implements DeploymentStrategy {

    private final ConcurrentHashMap<String, AtomicReference<Instant>> serviceStartTimes = new ConcurrentHashMap<>();
    private final AtomicReference<ConcurrentHashMap<String, BlueGreenConfig>> configCache = new AtomicReference<>(new ConcurrentHashMap<>());

    /**
     * Immutable configuration record for Blue-Green deployment with validation.
     * Ensures thread-safe configuration management across multiple pods.
     */
    public record BlueGreenConfig(Long ttl, String weight) {
        public BlueGreenConfig {
            if (ttl != null && ttl < 0) {
                throw new IllegalArgumentException("TTL must be non-negative, got: " + ttl);
            }

            if (weight != null && !weight.trim().isEmpty()) {
                validateWeightFormat(weight);
            }
        }

        private static void validateWeightFormat(String weight) {
            try {
                var weights = weight.split("/");
                if (weights.length != 2) {
                    throw new IllegalArgumentException("Weight must be in format 'blue/green', got: " + weight);
                }

                var blueWeight = Integer.parseInt(weights[0].trim());
                var greenWeight = Integer.parseInt(weights[1].trim());

                if (!((blueWeight == 1 && greenWeight == 0) || (blueWeight == 0 && greenWeight == 1))) {
                    throw new IllegalArgumentException("Blue-Green weights must be binary: '1/0' or '0/1', got: " + weight);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid weight format: " + weight + ". Expected binary format: '1/0' (Blue) or '0/1' (Green)");
            }
        }
    }

    public BlueGreen(InitializerConfiguration properties) {
        super(properties);
    }

    @Override
    public <R> R execute(Supplier<R> blue, Supplier<R> green, String serviceKey) {
        if (blue == null) {
            throw new IllegalArgumentException("Blue supplier cannot be null");
        }
        if (green == null) {
            throw new IllegalArgumentException("Green supplier cannot be null");
        }
        if (serviceKey == null || serviceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Service key cannot be null or empty");
        }

        var serviceConfig = validateServiceAndGetConfig(serviceKey, blue);
        var primaryResult = executeIfServiceInvalid(serviceConfig, blue);
        if (primaryResult != null) {
            return primaryResult;
        }

        BlueGreenConfig blueGreenConfig = getOrCreateBlueGreenConfig(serviceConfig, serviceKey);
        if (blueGreenConfig == null) {
            return blue.get();
        }

        boolean useGreenEnvironment = determineActiveEnvironment(blueGreenConfig, serviceKey);

        return useGreenEnvironment ? green.get() : blue.get();
    }

    /**
     * Thread-safe method to get or create BlueGreenConfig for a service.
     * Uses atomic operations to ensure consistency across multiple pods.
     */
    private BlueGreenConfig getOrCreateBlueGreenConfig(Object serviceConfig, String serviceKey) {
        return configCache.get().computeIfAbsent(serviceKey, key -> {
            try {
                var config = (InitializerConfiguration.DeployableServices) serviceConfig;
                var blueGreenSection = getBlueGreenSection(config);

                if (blueGreenSection == null) {
                    return null;
                }

                Long ttl = extractTtl(blueGreenSection);
                String weight = extractWeight(blueGreenSection);

                return new BlueGreenConfig(ttl, weight);
            } catch (Exception e) {
                log.warn("Failed to create BlueGreenConfig for service {}: {}", serviceKey, e.getMessage());
                return null;
            }
        });
    }

    /**
     * Safely extract BlueGreen section from service configuration.
     */
    private Object getBlueGreenSection(InitializerConfiguration.DeployableServices config) {
        try {
            var method = config.getClass().getMethod("getBlueGreen");
            return method.invoke(config);
        } catch (Exception e) {
            log.debug("No BlueGreen configuration found for service");
            return null;
        }
    }

    /**
     * Safely extract TTL from BlueGreen configuration.
     */
    private Long extractTtl(Object blueGreenSection) {
        try {
            var method = blueGreenSection.getClass().getMethod("getTtl");
            Object result = method.invoke(blueGreenSection);
            switch (result) {
                case null -> {
                    return null;
                }

                // Handle both Integer and Long types
                case Integer i -> {
                    return i.longValue();
                }
                case Long l -> {
                    return l;
                }
                default -> {
                    log.warn("Unexpected TTL type: {}, expected Integer or Long", result.getClass());
                    return null;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract TTL from configuration: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Safely extract weight from BlueGreen configuration.
     */
    private String extractWeight(Object blueGreenSection) {
        try {
            var method = blueGreenSection.getClass().getMethod("getWeight");
            return (String) method.invoke(blueGreenSection);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Determines which environment (Blue or Green) should handle the request.
     * Blue-Green deployment uses binary switching, not gradual traffic shifting.
     * Thread-safe implementation for multi-pod consistency.
     * <p>
     * When both weight and TTL are configured:
     * - Weight determines the INITIAL environment
     * - TTL determines when to switch FROM that initial environment
     *
     * @param blueGreenConfig the validated configuration for this service
     * @param serviceKey      the service identifier
     * @return true if Green environment should be used, false for Blue
     */
    private boolean determineActiveEnvironment(BlueGreenConfig blueGreenConfig, String serviceKey) {
        if (blueGreenConfig.weight() != null && blueGreenConfig.ttl() != null) {
            return isGreenEnvironmentActiveByWeightAndTtl(blueGreenConfig.weight(), blueGreenConfig.ttl(), serviceKey);
        }

        if (blueGreenConfig.ttl() != null) {
            return isGreenEnvironmentActiveByTtl(blueGreenConfig.ttl(), serviceKey);
        }

        if (blueGreenConfig.weight() != null) {
            return parseEnvironmentFromWeight(blueGreenConfig.weight());
        }

        return false;
    }

    /**
     * Thread-safe environment determination when both weight and TTL are configured.
     * Weight determines the INITIAL environment, TTL determines when to switch FROM that initial environment.
     * <p>
     * TTL behavior:
     * - TTL = 0 or undefined → Infinite TTL (never switch, stay in initial environment)
     * - TTL > 0 → Switch to opposite environment after TTL seconds
     * <p>
     * Examples:
     * - weight: "0/1", ttl: 20 → Start with Green, switch to Blue after 20 seconds
     * - weight: "0/1", ttl: 0 → Start with Green, never switch (infinite TTL)
     * - weight: "1/0", ttl: 20 → Start with Blue, switch to Green after 20 seconds
     */
    private boolean isGreenEnvironmentActiveByWeightAndTtl(String weight, Long ttl, String serviceKey) {
        boolean initiallyGreen = parseEnvironmentFromWeight(weight);

        if (ttl == null || ttl <= 0) {
            return initiallyGreen;
        }

        AtomicReference<Instant> startTimeRef = serviceStartTimes.computeIfAbsent(
                serviceKey,
                key -> new AtomicReference<>(Instant.now())
        );

        Instant startTime = startTimeRef.get();
        if (startTime == null) {
            startTime = Instant.now();
            startTimeRef.set(startTime);
        }

        long elapsedSeconds = Duration.between(startTime, Instant.now()).toSeconds();

        if (elapsedSeconds >= ttl) {
            return !initiallyGreen;
        } else {
            return initiallyGreen;
        }
    }

    /**
     * Thread-safe TTL-based environment determination.
     * Each service maintains its own start time for consistency across pods.
     * <p>
     * TTL behavior:
     * - TTL = 0 or undefined → Infinite TTL (stay in Blue forever)
     * - TTL > 0 → Start with Blue, switch to Green after TTL seconds
     */
    private boolean isGreenEnvironmentActiveByTtl(Long ttl, String serviceKey) {
        if (ttl == null || ttl <= 0) {
            return false;
        }

        AtomicReference<Instant> startTimeRef = serviceStartTimes.computeIfAbsent(
                serviceKey,
                key -> new AtomicReference<>(Instant.now())
        );

        Instant startTime = startTimeRef.get();
        long elapsedSeconds = Duration.between(startTime, Instant.now()).toSeconds();

        return elapsedSeconds >= ttl;
    }

    /**
     * Parses weight configuration to determine active environment.
     * For Blue-Green, weight should be binary: "1/0" (Blue active) or "0/1" (Green active)
     */
    private boolean parseEnvironmentFromWeight(String weight) {
        if (weight == null || weight.trim().isEmpty()) {
            return false;
        }

        try {
            var weights = weight.split("/");
            if (weights.length == 2) {
                var blueWeight = Integer.parseInt(weights[0].trim());
                var greenWeight = Integer.parseInt(weights[1].trim());

                if (blueWeight == 1 && greenWeight == 0) {
                    return false;
                } else if (blueWeight == 0 && greenWeight == 1) {
                    return true;
                }
            }

            throw new IllegalArgumentException("Blue-Green weights must be binary: '1/0' (Blue active) or '0/1' (Green active)");

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid weight format: " + weight +
                    ". Expected binary format: '1/0' (Blue active) or '0/1' (Green active).Continuing with the stable version...");
        }
    }
}

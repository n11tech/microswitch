package com.microswitch.domain.strategy;

import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.application.random.UniqueRandomGenerator;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.value.AlgorithmType;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Canary extends DeployTemplate implements DeploymentStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(Canary.class);

    private final ConcurrentHashMap<String, AtomicInteger> serviceCounters = new ConcurrentHashMap<>();
    private final AtomicReference<ConcurrentHashMap<String, CanaryConfig>> serviceConfigs = new AtomicReference<>(new ConcurrentHashMap<>());
    private final ConcurrentHashMap<String, UniqueRandomGenerator> randomGenerators = new ConcurrentHashMap<>();

    private record CanaryConfig(int primaryPercentage, int secondaryPercentage, int totalCalls, AlgorithmType algorithm) {
        public CanaryConfig {
            validatePercentages(primaryPercentage, secondaryPercentage);
            if (totalCalls <= 0) {
                throw new IllegalArgumentException("Total calls must be positive, got: " + totalCalls);
            }
            if (algorithm == null) {
                throw new IllegalArgumentException("Algorithm cannot be null");
            }
        }
        
        private static void validatePercentages(int primary, int secondary) {
            validatePercentage(primary, secondary);
        }

        private static void validatePercentage(int primary, int secondary) {
            if (primary < 0 || primary > 100) {
                throw new IllegalArgumentException("Primary percentage must be between 0 and 100, got: " + primary);
            }
            if (secondary < 0 || secondary > 100) {
                throw new IllegalArgumentException("Secondary percentage must be between 0 and 100, got: " + secondary);
            }
            if (primary + secondary != 100) {
                throw new IllegalArgumentException("Primary and secondary percentages must sum to 100, got: " + (primary + secondary));
            }
        }
    }

    private record ExecutionResult<R>(R result, boolean usedExperimental) {
    }

    public Canary(InitializerConfiguration properties) {
        super(properties);
    }

    @Override
    public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
        validateInputs(primary, secondary, serviceKey);
        
        var serviceConfig = validateServiceAndGetConfig(serviceKey, primary);
        var primaryResult = executeIfServiceInvalid(serviceConfig, primary);
        if (primaryResult != null) {
            return primaryResult;
        }

        var canaryConfigData = serviceConfig.getCanary();
        if (canaryConfigData == null) {
            return primary.get();
        }

        CanaryConfig config = createCanaryConfig(canaryConfigData, serviceKey);
        
        if (config.algorithm() == AlgorithmType.RANDOM) {
            return executeRandom(primary, secondary, config, serviceKey).result;
        } else {
            return executeSequence(primary, secondary, config, serviceKey).result;
        }
    }
    
    private void validateInputs(Supplier<?> primary, Supplier<?> secondary, String serviceKey) {
        if (primary == null) {
            throw new IllegalArgumentException("Primary supplier cannot be null");
        }
        if (secondary == null) {
            throw new IllegalArgumentException("Secondary supplier cannot be null");
        }
        if (serviceKey == null || serviceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Service key cannot be null or empty");
        }
    }

    private CanaryConfig createCanaryConfig(InitializerConfiguration.Canary canaryConfig, String serviceKey) {
        var percentages = parsePercentageString(canaryConfig.getPercentage());
        int primaryPercentage = percentages[0];
        int secondaryPercentage = percentages[1];
        
        int totalCalls = calculateTotalCalls(primaryPercentage, secondaryPercentage);
        
        AlgorithmType algorithm = parseAlgorithm(canaryConfig.getAlgorithm());
        
        CanaryConfig config = new CanaryConfig(primaryPercentage, secondaryPercentage, totalCalls, algorithm);
        
        serviceConfigs.get().put(serviceKey, config);
        
        return config;
    }
    
    private int calculateTotalCalls(int primaryPercentage, int secondaryPercentage) {
        try {
            int gcdValue = gcd(primaryPercentage, secondaryPercentage);
            if (gcdValue == 0) {
                throw new ArithmeticException("GCD calculation resulted in zero");
            }
            return 100 / gcdValue;
        } catch (ArithmeticException e) {
            logger.warn("GCD calculation failed, using default total calls of 100: {}", e.getMessage());
            return 100;
        }
    }
    
    private int[] parsePercentageString(String percentageString) {
        if (percentageString == null || percentageString.trim().isEmpty()) {
            return new int[]{100, 0};
        }
        
        String trimmed = percentageString.trim();
        
        if (trimmed.contains("/")) {
            return parseSlashFormat(trimmed, percentageString);
        } else {
            return parseSingleNumber(trimmed, percentageString);
        }
    }
    
    private int[] parseSlashFormat(String trimmed, String originalString) {
        String[] parts = trimmed.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid percentage format. Expected 'primary/secondary' (e.g., '10/90'), got: " + originalString);
        }
        
        try {
            int primary = Integer.parseInt(parts[0].trim());
            int secondary = Integer.parseInt(parts[1].trim());
            return processRawPercentages(primary, secondary);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in percentage string: " + originalString, e);
        }
    }
    
    private int[] processRawPercentages(int primary, int secondary) {
        validateNonNegative(primary, secondary);
        
        int sum = primary + secondary;
        if (sum == 0) {
            throw new IllegalArgumentException("Primary and secondary values cannot both be zero");
        }
        
        if (sum != 100) {
            return normalizeToPercentages(primary, secondary, sum);
        }
        
        CanaryConfig.validatePercentages(primary, secondary);
        return new int[]{primary, secondary};
    }
    
    private void validateNonNegative(int primary, int secondary) {
        if (primary < 0 || secondary < 0) {
            throw new IllegalArgumentException("Percentages cannot be negative. Got primary=" + primary + ", secondary=" + secondary);
        }
    }
    
    private int[] normalizeToPercentages(int primary, int secondary, int sum) {
        int normalizedPrimary = (int) Math.round((primary * 100.0) / sum);
        int normalizedSecondary = 100 - normalizedPrimary;
        CanaryConfig.validatePercentages(normalizedPrimary, normalizedSecondary);
        return new int[]{normalizedPrimary, normalizedSecondary};
    }
    
    private int[] parseSingleNumber(String trimmed, String originalString) {
        try {
            int primary = Integer.parseInt(trimmed);
            if (primary < 0 || primary > 100) {
                throw new IllegalArgumentException("Primary percentage must be between 0 and 100, got: " + primary);
            }
            int secondary = 100 - primary;
            return new int[]{primary, secondary};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid percentage format. Expected number or 'primary/secondary' format, got: " + originalString, e);
        }
    }
    
    private AlgorithmType parseAlgorithm(String algorithmString) {
        if (algorithmString == null || algorithmString.trim().isEmpty()) {
            return AlgorithmType.SEQUENTIAL;
        }
        
        try {
            return AlgorithmType.valueOf(algorithmString.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (AlgorithmType type : AlgorithmType.values()) {
                if (Objects.equals(algorithmString.toLowerCase(), type.name().toLowerCase())) {
                    return type;
                }
            }
            logger.warn("Unknown algorithm type: {}, defaulting to sequential", algorithmString);
            return AlgorithmType.SEQUENTIAL;
        }
    }

    private <R> ExecutionResult<R> executeSequence(Supplier<R> primary, Supplier<R> secondary, CanaryConfig config, String serviceKey) {
        AtomicInteger counter = serviceCounters.computeIfAbsent(serviceKey, k -> new AtomicInteger(0));
        
        int callsForPrimaryMethod = (config.totalCalls() * config.primaryPercentage()) / 100;
        int currentCount = counter.getAndUpdate(c -> (c + 1) % config.totalCalls());
        
        if (currentCount < callsForPrimaryMethod) {
            return new ExecutionResult<>(primary.get(), false);
        } else {
            return new ExecutionResult<>(secondary.get(), true);
        }
    }

    private <R> ExecutionResult<R> executeRandom(Supplier<R> primary, Supplier<R> secondary, CanaryConfig config, String serviceKey) {
        UniqueRandomGenerator generator = randomGenerators.computeIfAbsent(serviceKey, k -> {
            try {
                return new UniqueRandomGenerator(config.totalCalls());
            } catch (Exception e) {
                logger.warn("Failed to create UniqueRandomGenerator for service {}: {}", serviceKey, e.getMessage());
                return new UniqueRandomGenerator(100);
            }
        });
        
        int randomValue;
        synchronized (generator) {
            if (generator.getUniqueValues().size() >= config.totalCalls()) {
                try {
                    generator = new UniqueRandomGenerator(config.totalCalls());
                    randomGenerators.put(serviceKey, generator);
                } catch (Exception e) {
                    logger.warn("Failed to reset UniqueRandomGenerator for service {}: {}", serviceKey, e.getMessage());
                }
            }
            randomValue = generator.getNextUniqueRandomValue();
        }
        
        int callsForPrimaryMethod = (config.totalCalls() * config.primaryPercentage()) / 100;
        
        if (randomValue < callsForPrimaryMethod) {
            return new ExecutionResult<>(primary.get(), false);
        } else {
            return new ExecutionResult<>(secondary.get(), true);
        }
    }

    private int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        
        return a == 0 ? 1 : a;
    }
}

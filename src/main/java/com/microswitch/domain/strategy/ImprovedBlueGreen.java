package com.microswitch.domain.strategy;

import com.microswitch.application.executor.DeploymentStrategy;
import com.microswitch.domain.InitializerConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Improved Blue-Green deployment strategy implementation.
 * 
 * Blue-Green deployment maintains two identical production environments:
 * - Blue (current/stable)
 * - Green (new/experimental)
 * 
 * Traffic can be switched gradually or instantly between environments.
 */
public class ImprovedBlueGreen extends DeployTemplate implements DeploymentStrategy {
    private final Instant startTime = Instant.now();
    
    public ImprovedBlueGreen(InitializerConfiguration properties) {
        super(properties);
    }

    @Override
    public <R> R execute(Supplier<R> blue, Supplier<R> green, String serviceKey) {
        var serviceConfig = validateServiceAndGetConfig(serviceKey, blue);
        var primaryResult = executeIfServiceInvalid(serviceConfig, blue);
        if (primaryResult != null) {
            return primaryResult;
        }

        var blueGreenConfig = serviceConfig.getBlueGreen();
        if (blueGreenConfig == null) {
            return blue.get(); // Default to blue (stable)
        }

        // Blue-Green is a BINARY switch - either Blue (false) or Green (true)
        boolean useGreenEnvironment = determineActiveEnvironment(blueGreenConfig, serviceKey);
        
        return useGreenEnvironment ? green.get() : blue.get();
    }
    
    /**
     * Determines which environment (Blue or Green) should handle the request.
     * Blue-Green deployment uses binary switching, not gradual traffic shifting.
     * 
     * @param blueGreenConfig the configuration for this service
     * @param serviceKey the service identifier
     * @return true if Green environment should be used, false for Blue
     */
    private boolean determineActiveEnvironment(Object blueGreenConfig, String serviceKey) {
        // Cast to actual config type to access TTL
        var config = (InitializerConfiguration.BlueGreen) blueGreenConfig;
        
        // Access TTL field (Lombok should generate getTtl() method)
        Long ttl = config.getTtl();
        
        if (ttl != null && ttl > 0) {
            // TTL-based binary switch: Blue before TTL expires, Green after
            long elapsedSeconds = Duration.between(startTime, Instant.now()).toSeconds();
            return elapsedSeconds >= ttl; // Switch to Green after TTL expires
        }
        
        // No TTL configured, check weight for manual switch
        String weight = config.getWeight();
        if (weight != null) {
            return parseEnvironmentFromWeight(weight);
        }
        
        return false; // Default to Blue (stable) environment
    }
    
    /**
     * Parses weight configuration to determine active environment.
     * For Blue-Green, weight should be binary: "1/0" (Blue) or "0/1" (Green)
     */
    private boolean parseEnvironmentFromWeight(String weight) {
        if (weight == null || weight.trim().isEmpty()) {
            return false; // Default to Blue
        }
        
        try {
            var weights = weight.split("/");
            if (weights.length == 2) {
                var blueWeight = Integer.parseInt(weights[0].trim());
                var greenWeight = Integer.parseInt(weights[1].trim());
                
                // Validate binary weights
                if ((blueWeight == 1 && greenWeight == 0) || (blueWeight == 0 && greenWeight == 1)) {
                    return greenWeight == 1; // Return true if Green is active
                }
            }
            
            throw new IllegalArgumentException("Blue-Green weights must be binary: '1/0' or '0/1'");
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid weight format: " + weight + 
                ". Expected binary format: '1/0' (Blue) or '0/1' (Green)");
        }
    }
}

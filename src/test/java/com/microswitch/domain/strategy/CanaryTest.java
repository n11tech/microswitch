package com.microswitch.domain.strategy;

import com.microswitch.domain.InitializerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CanaryTest {

    private InitializerConfiguration properties;
    private Canary canaryStrategy;

    @BeforeEach
    void setUp() {
        properties = new InitializerConfiguration();
        Map<String, InitializerConfiguration.DeployableServices> services = new HashMap<>();
        properties.setServices(services);
        canaryStrategy = new Canary(properties);
    }

    @Test
    void testExecuteWithDisabledService() {
        // Given
        String serviceKey = "disabled-service";
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When
        String result = canaryStrategy.execute(primary, secondary, serviceKey);

        // Then
        assertEquals("primary", result);
    }

    @Test
    void testExecuteWithNullServiceKey() {
        // Given
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> canaryStrategy.execute(primary, secondary, null));
    }

    @Test
    void testExecuteWithEmptyServiceKey() {
        // Given
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> canaryStrategy.execute(primary, secondary, ""));
    }

    @Test
    void testNullPrimarySupplierThrows() {
        // Given
        String serviceKey = "svc";
        Supplier<String> secondary = () -> "secondary";

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> canaryStrategy.execute(null, secondary, serviceKey));
    }

    @Test
    void testNullSecondarySupplierThrows() {
        // Given
        String serviceKey = "svc";
        Supplier<String> primary = () -> "primary";

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> canaryStrategy.execute(primary, null, serviceKey));
    }

    @Test
    void testExecuteWithEnabledServiceButNoCanaryConfig() {
        // Given
        String serviceKey = "test-service";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When
        String result = canaryStrategy.execute(primary, secondary, serviceKey);

        // Then
        assertEquals("primary", result);
    }

    @Test
    void testExecuteWithCanaryConfigurationSequentialDeterministicFirstCall() {
        // Given
        String serviceKey = "test-service";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("80/20");
        canaryConfig.setAlgorithm("sequence");
        deployableServices.setCanary(canaryConfig);
        
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When
        String result = canaryStrategy.execute(primary, secondary, serviceKey);

        // Then: SEQUENTIAL with 80/20 -> first call should be primary
        assertEquals("primary", result);
    }

    @Test
    void testSequentialWindowDistribution_80_20_over5Calls() {
        // Given
        String serviceKey = "seq-window";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("80/20"); 
        canaryConfig.setAlgorithm("sequential");
        deployableServices.setCanary(canaryConfig);
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        int primaryCount = 0;
        int secondaryCount = 0;
        for (int i = 0; i < 5; i++) {
            String res = canaryStrategy.execute(primary, secondary, serviceKey);
            if ("primary".equals(res)) primaryCount++; else secondaryCount++;
        }

        assertEquals(4, primaryCount);
        assertEquals(1, secondaryCount);
    }

    @Test
    void testSingleNumberPercentage_70_means_70_30() {
        // Given
        String serviceKey = "single-number";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("70");
        canaryConfig.setAlgorithm("sequential");
        deployableServices.setCanary(canaryConfig);
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When & Then: first call in SEQUENTIAL should be primary
        assertEquals("primary", canaryStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testNormalization_1_over_9_becomes_10_90_with_window10() {
        // Given
        String serviceKey = "normalize-1-9";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("1/9"); 
        canaryConfig.setAlgorithm("sequential");
        deployableServices.setCanary(canaryConfig);
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        int primaryCount = 0;
        int secondaryCount = 0;
        for (int i = 0; i < 10; i++) {
            String res = canaryStrategy.execute(primary, secondary, serviceKey);
            if ("primary".equals(res)) primaryCount++; else secondaryCount++;
        }

        assertEquals(1, primaryCount);
        assertEquals(9, secondaryCount);
    }

    @Test
    void testRandomAlgorithm_with_0_100_always_secondary() {
        // Given
        String serviceKey = "random-0-100";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("0/100");
        canaryConfig.setAlgorithm("random");
        deployableServices.setCanary(canaryConfig);
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        for (int i = 0; i < 20; i++) {
            assertEquals("secondary", canaryStrategy.execute(primary, secondary, serviceKey));
        }
    }

    @Test
    void testUnknownAlgorithmDefaultsToSequential() {
        // Given
        String serviceKey = "unknown-algo";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("80/20");
        canaryConfig.setAlgorithm("totally-unknown");
        deployableServices.setCanary(canaryConfig);
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When & Then: should default to SEQUENTIAL, first call primary
        assertEquals("primary", canaryStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testInvalidPrimaryPercentage() {
        // Given
        String serviceKey = "test-service";
        InitializerConfiguration.DeployableServices deployableServices = new InitializerConfiguration.DeployableServices();
        deployableServices.setEnabled(true);
        
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("150/-50"); 
        deployableServices.setCanary(canaryConfig);
        
        properties.getServices().put(serviceKey, deployableServices);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> canaryStrategy.execute(primary, secondary, serviceKey));
    }
}

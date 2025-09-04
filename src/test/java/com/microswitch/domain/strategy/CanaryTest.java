package com.microswitch.domain.strategy;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanaryTest {

    @Mock
    private DeploymentMetrics deploymentMetrics;

    private InitializerConfiguration properties;
    private Canary canaryStrategy;

    @BeforeEach
    void setUp() {
        properties = new InitializerConfiguration();
        Map<String, InitializerConfiguration.ServiceConfig> services = new HashMap<>();
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
        verifyNoInteractions(deploymentMetrics);
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
    void testExecuteWithEnabledServiceButNoCanaryConfig() {
        // Given
        String serviceKey = "test-service";
        InitializerConfiguration.ServiceConfig serviceConfig = new InitializerConfiguration.ServiceConfig();
        serviceConfig.setEnabled(true);
        properties.getServices().put(serviceKey, serviceConfig);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When
        String result = canaryStrategy.execute(primary, secondary, serviceKey);

        // Then
        assertEquals("primary", result);
    }

    @Test
    void testExecuteWithCanaryConfiguration() {
        // Given
        String serviceKey = "test-service";
        InitializerConfiguration.ServiceConfig serviceConfig = new InitializerConfiguration.ServiceConfig();
        serviceConfig.setEnabled(true);
        
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPrimaryPercentage(80);
        canaryConfig.setAlgorithm("sequence");
        serviceConfig.setCanary(canaryConfig);
        
        properties.getServices().put(serviceKey, serviceConfig);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When
        String result = canaryStrategy.execute(primary, secondary, serviceKey);

        // Then
        assertNotNull(result);
        assertTrue(result.equals("primary") || result.equals("secondary"));
        verify(deploymentMetrics, atLeastOnce()).recordSuccess(eq(serviceKey), anyString(), eq("canary"));
    }

    @Test
    void testInvalidPrimaryPercentage() {
        // Given
        String serviceKey = "test-service";
        InitializerConfiguration.ServiceConfig serviceConfig = new InitializerConfiguration.ServiceConfig();
        serviceConfig.setEnabled(true);
        
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPrimaryPercentage(150); // Invalid percentage
        serviceConfig.setCanary(canaryConfig);
        
        properties.getServices().put(serviceKey, serviceConfig);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> canaryStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testMetricsRecordingFailure() {
        // Given
        String serviceKey = "test-service";
        InitializerConfiguration.ServiceConfig serviceConfig = new InitializerConfiguration.ServiceConfig();
        serviceConfig.setEnabled(true);
        
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPrimaryPercentage(100); // Always primary
        serviceConfig.setCanary(canaryConfig);
        
        properties.getServices().put(serviceKey, serviceConfig);

        doThrow(new RuntimeException("Metrics failure"))
            .when(deploymentMetrics).recordSuccess(anyString(), anyString(), anyString());

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        // When
        String result = canaryStrategy.execute(primary, secondary, serviceKey);

        // Then
        assertEquals("primary", result); // Should still return result despite metrics failure
    }
}

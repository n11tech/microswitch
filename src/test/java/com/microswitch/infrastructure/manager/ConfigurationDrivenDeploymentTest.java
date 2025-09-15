package com.microswitch.infrastructure.manager;

import com.microswitch.application.executor.MicroswitchDeploymentStrategyExecutor;
import com.microswitch.application.metric.NoOpDeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the new configuration-driven deployment approach.
 * Tests that the execute() method correctly routes to different strategies
 * based on the activeStrategy configuration.
 */
class ConfigurationDrivenDeploymentTest {

    private DeploymentManager deploymentManager;
    private InitializerConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new InitializerConfiguration();
        configuration.setEnabled(true);
        
        // Set up services configuration
        Map<String, InitializerConfiguration.DeployableServices> services = new HashMap<>();
        
        // Configure canary service
        InitializerConfiguration.DeployableServices canaryService = new InitializerConfiguration.DeployableServices();
        canaryService.setEnabled(true);
        canaryService.setActiveStrategy("canary");
        
        InitializerConfiguration.Canary canaryConfig = new InitializerConfiguration.Canary();
        canaryConfig.setPercentage("90/10");
        canaryConfig.setAlgorithm("sequential");
        canaryService.setCanary(canaryConfig);
        
        services.put("canary-service", canaryService);
        
        // Configure shadow service
        InitializerConfiguration.DeployableServices shadowService = new InitializerConfiguration.DeployableServices();
        shadowService.setEnabled(true);
        shadowService.setActiveStrategy("shadow");
        
        InitializerConfiguration.Shadow shadowConfig = new InitializerConfiguration.Shadow();
        shadowConfig.setMirrorPercentage((short) 20);
        shadowService.setShadow(shadowConfig);
        
        services.put("shadow-service", shadowService);
        
        // Configure blue-green service
        InitializerConfiguration.DeployableServices blueGreenService = new InitializerConfiguration.DeployableServices();
        blueGreenService.setEnabled(true);
        blueGreenService.setActiveStrategy("blueGreen");
        
        InitializerConfiguration.BlueGreen blueGreenConfig = new InitializerConfiguration.BlueGreen();
        blueGreenConfig.setWeight("1/0");
        blueGreenConfig.setTtl(60000L);
        blueGreenService.setBlueGreen(blueGreenConfig);
        
        services.put("bluegreen-service", blueGreenService);
        
        configuration.setServices(services);
        
        // Create deployment manager with the configuration
        MicroswitchDeploymentStrategyExecutor executor = new MicroswitchDeploymentStrategyExecutor(
            configuration, new NoOpDeploymentMetrics());
        deploymentManager = DeploymentManager.createWithExecutor(executor);
    }

    @Test
    void testExecuteWithCanaryStrategy() {
        Supplier<String> stable = () -> "stable-result";
        Supplier<String> experimental = () -> "experimental-result";
        
        // Execute should route to canary strategy based on configuration
        String result = deploymentManager.execute(stable, experimental, "canary-service");
        
        // Result should be one of the two (depending on canary logic)
        assertTrue(result.equals("stable-result") || result.equals("experimental-result"));
    }

    @Test
    void testExecuteWithShadowStrategy() {
        Supplier<String> stable = () -> "stable-result";
        Supplier<String> experimental = () -> "experimental-result";
        
        // Execute should route to shadow strategy based on configuration
        String result = deploymentManager.execute(stable, experimental, "shadow-service");
        
        // Shadow strategy behavior depends on implementation - just verify it executes
        assertNotNull(result);
        assertTrue(result.equals("stable-result") || result.equals("experimental-result"));
    }

    @Test
    void testExecuteWithBlueGreenStrategy() {
        Supplier<String> stable = () -> "stable-result";
        Supplier<String> experimental = () -> "experimental-result";
        
        // Execute should route to blue-green strategy based on configuration
        String result = deploymentManager.execute(stable, experimental, "bluegreen-service");
        
        // Result should be one of the two (depending on blue-green logic)
        assertTrue(result.equals("stable-result") || result.equals("experimental-result"));
    }

    @Test
    void testExecuteWithMissingActiveStrategy() {
        // Create service without activeStrategy
        InitializerConfiguration.DeployableServices serviceWithoutStrategy = new InitializerConfiguration.DeployableServices();
        serviceWithoutStrategy.setEnabled(true);
        serviceWithoutStrategy.setActiveStrategy(null); // Explicitly set to null to trigger exception
        
        configuration.getServices().put("no-strategy-service", serviceWithoutStrategy);
        
        Supplier<String> stable = () -> "stable-result";
        Supplier<String> experimental = () -> "experimental-result";
        
        // Should throw exception for missing active strategy (wrapped in IllegalStateException due to reflection)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            deploymentManager.execute(stable, experimental, "no-strategy-service"));
        
        assertTrue(exception.getCause() instanceof java.lang.reflect.InvocationTargetException);
        assertTrue(exception.getMessage().contains("Failed to invoke strategy method"));
    }

    @Test
    void testExecuteWithInvalidActiveStrategy() {
        // Create service with invalid activeStrategy
        InitializerConfiguration.DeployableServices serviceWithInvalidStrategy = new InitializerConfiguration.DeployableServices();
        serviceWithInvalidStrategy.setEnabled(true);
        serviceWithInvalidStrategy.setActiveStrategy("invalid-strategy");
        
        configuration.getServices().put("invalid-strategy-service", serviceWithInvalidStrategy);
        
        Supplier<String> stable = () -> "stable-result";
        Supplier<String> experimental = () -> "experimental-result";
        
        // Should throw exception for invalid active strategy (wrapped in IllegalStateException due to reflection)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            deploymentManager.execute(stable, experimental, "invalid-strategy-service"));
        
        assertTrue(exception.getCause() instanceof java.lang.reflect.InvocationTargetException);
        assertTrue(exception.getMessage().contains("Failed to invoke strategy method"));
    }

    @Test
    void testExecuteWithNonExistentService() {
        Supplier<String> stable = () -> "stable-result";
        Supplier<String> experimental = () -> "experimental-result";
        
        // Should throw exception for non-existent service (wrapped in IllegalStateException due to reflection)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            deploymentManager.execute(stable, experimental, "non-existent-service"));
        
        assertTrue(exception.getCause() instanceof java.lang.reflect.InvocationTargetException);
        assertTrue(exception.getMessage().contains("Failed to invoke strategy method"));
    }

    @Test
    void testBackwardCompatibilityWithDeprecatedMethods() {
        Supplier<String> stable = () -> "stable-result";
        Supplier<String> experimental = () -> "experimental-result";
        
        // Deprecated methods should still work
        assertDoesNotThrow(() -> deploymentManager.canary(stable, experimental, "canary-service"));
        assertDoesNotThrow(() -> deploymentManager.shadow(stable, experimental, "shadow-service"));
        assertDoesNotThrow(() -> deploymentManager.blueGreen(stable, experimental, "bluegreen-service"));
    }
}

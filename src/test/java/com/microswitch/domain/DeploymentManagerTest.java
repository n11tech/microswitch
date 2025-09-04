package com.microswitch.domain;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.strategy.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeploymentManagerTest {

    private DeploymentManager deploymentManager;

    @BeforeEach
    void setUp() {
        // Create simple test factory with test strategies
        TestDeploymentStrategyFactory factory = new TestDeploymentStrategyFactory();
        deploymentManager = new DeploymentManager(factory);
    }

    @Test
    void testCanaryDeployment() {
        // Given
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";
        String serviceKey = "test-service";

        // When
        String result = deploymentManager.canary(primary, secondary, serviceKey);

        // Then
        assertEquals("primary", result);
    }

    @Test
    void testShadowDeployment() {
        // Given
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";
        String serviceKey = "test-service";

        // When
        String result = deploymentManager.shadow(primary, secondary, serviceKey);

        // Then
        assertEquals("primary", result);
    }

    @Test
    void testBlueGreenDeployment() {
        // Given
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";
        String serviceKey = "test-service";

        // When
        String result = deploymentManager.blueGreen(primary, secondary, serviceKey);

        // Then
        assertEquals("primary", result);
    }

    @Test
    void testGenericTypeHandling() {
        // Given
        Supplier<Integer> primary = () -> 42;
        Supplier<Integer> secondary = () -> 24;
        String serviceKey = "test-service";

        // When
        Integer result = deploymentManager.canary(primary, secondary, serviceKey);

        // Then
        assertEquals(Integer.valueOf(42), result);
    }

    // Test strategy implementations that always return primary
    static class TestCanaryStrategy implements DeploymentStrategy {
        @Override
        public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
            return primary.get();
        }
    }

    static class TestShadowStrategy implements DeploymentStrategy {
        @Override
        public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
            secondary.get(); // Execute secondary but ignore result
            return primary.get();
        }
    }

    static class TestBlueGreenStrategy implements DeploymentStrategy {
        @Override
        public <R> R execute(Supplier<R> primary, Supplier<R> secondary, String serviceKey) {
            return primary.get();
        }
    }

    static class TestDeploymentStrategyFactory extends DeploymentStrategyFactory {
        public TestDeploymentStrategyFactory() {
            super(new InitializerConfiguration(), 
                  new DeploymentMetrics(Mockito.mock(MeterRegistry.class)));
        }

        @Override
        protected void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
            addStrategy(StrategyType.CANARY, new TestCanaryStrategy());
            addStrategy(StrategyType.SHADOW, new TestShadowStrategy());
            addStrategy(StrategyType.BLUE_GREEN, new TestBlueGreenStrategy());
        }
    }
}

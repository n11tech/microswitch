package com.n11.development.infrastructure.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeploymentManagerTest {
    @Mock
    private IDeploymentStrategy canaryStrategy;
    @Mock
    private IDeploymentStrategy blueGreenStrategy;
    @Mock
    private IDeploymentStrategy shadowStrategy;

    private DeploymentManager deploymentManager;

    private static final String domain = "basket";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up strategies map
        Map<String, IDeploymentStrategy> strategies = new HashMap<>();
        strategies.put(StrategyType.CANARY.getValue(), canaryStrategy);
        strategies.put("blueGreen", blueGreenStrategy);
        strategies.put("shadow", shadowStrategy);

        deploymentManager = new DeploymentManager(strategies);
    }

    @Test
    void testSetStrategyWithValidStrategy() {
        // Act
        deploymentManager.setStrategy(StrategyType.CANARY.getValue());

        // Verify that the canary strategy was set
        assertNotNull(deploymentManager.currentStrategy);
    }

    @Test
    void testSetStrategyWithInvalidStrategy() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            deploymentManager.setStrategy("INVALID_STRATEGY");
        });

        assertEquals("No deployment strategy found for type: INVALID_STRATEGY", exception.getMessage());
    }

    @Test
    void testExecuteDeployWithSetStrategyAndValidFuncs() {
        // Arrange
        deploymentManager.setStrategy(StrategyType.CANARY.getValue());
        Function<String, Integer> func1 = String::length;
        Function<String, Integer> func2 = String::hashCode;
        List<Function<String, Integer>> funcs = List.of(func1, func2);
        String serviceKey = "service1";
        String input = "test";
        Integer expectedResult = 123;

        // Stub the current strategy's execute method
        when(canaryStrategy.execute(func1, func2, domain, serviceKey, input)).thenReturn(expectedResult);

        // Act
        Integer result = deploymentManager.executeDeploy(funcs, domain, serviceKey, input);

        // Assert
        assertEquals(expectedResult, result);
        verify(canaryStrategy).execute(func1, func2, domain, serviceKey, input);
    }

    @Test
    void testExecuteDeployWithoutSettingStrategy() {
        // Arrange
        Function<String, Integer> func1 = String::length;
        Function<String, Integer> func2 = String::hashCode;
        List<Function<String, Integer>> funcs = List.of(func1, func2);
        String serviceKey = "service1";
        String input = "test";

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            deploymentManager.executeDeploy(funcs, domain, serviceKey, input);
        });

        assertEquals("Deployment strategy not set.", exception.getMessage());
    }

    @Test
    void testExecuteDeployWithInvalidFuncsList() {
        // Arrange
        deploymentManager.setStrategy(StrategyType.BLUE_GREEN.getValue());
        List<Function<String, Integer>> invalidFuncs = List.of(String::length);
        String serviceKey = "service1";
        String input = "test";

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            deploymentManager.executeDeploy(invalidFuncs, domain, serviceKey, input);
        });

        assertEquals("The funcs list must contain exactly two functions.", exception.getMessage());
    }
}

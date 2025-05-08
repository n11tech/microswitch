package com.n11.development.infrastructure;

import com.n11.development.infrastructure.strategy.StrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmbeddedDeployTest {
    @Mock
    private IDeploymentManager deploymentManager;

    @InjectMocks
    private EmbeddedDeploy embeddedDeploy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExecuteWithValidStrategyAndFuncs() {
        // Arrange
        StrategyType strategyType = StrategyType.CANARY;
        String domain = "basket";
        String serviceKey = "get-basket";
        Function<String, Integer> func1 = String::length;
        Function<String, Integer> func2 = String::hashCode;
        List<Function<String, Integer>> funcs = List.of(func1, func2);
        String input = "test";
        Integer expectedResult = 4;

        // Stub deploymentManager behavior
        when(deploymentManager.executeDeploy(any(), any(), any(), any())).thenReturn(expectedResult);

        // Act
        Integer result = embeddedDeploy.setExecutableService(domain, serviceKey)
                .execute(strategyType, funcs, input);

        // Assert
        assertEquals(expectedResult, result);
        verify(deploymentManager).setStrategy(strategyType.getValue());
        verify(deploymentManager).executeDeploy(funcs, domain, serviceKey, input);
    }

    @Test
    void testExecuteWithEmptyFuncs() {
        // Arrange
        StrategyType strategyType = StrategyType.SHADOW;
        String domain = "basket";
        String serviceKey = "service2";
        List<Function<String, Integer>> funcs = List.of();
        String input = "test";

        // Stub deploymentManager behavior for empty list
        when(deploymentManager.executeDeploy(funcs, domain, serviceKey, input)).thenReturn(null);

        // Act
        Integer result = embeddedDeploy.setExecutableService(domain, serviceKey)
                .execute(strategyType, funcs, input);

        // Assert
        assertNull(result);
        verify(deploymentManager).setStrategy(strategyType.getValue());
        verify(deploymentManager).executeDeploy(funcs, domain, serviceKey, input);
    }

    @Test
    void testExecuteWithDifferentStrategy() {
        // Arrange
        StrategyType strategyType = StrategyType.BLUE_GREEN;
        String domain = "basket";
        String serviceKey = "service3";
        Function<String, Integer> func1 = String::length;
        List<Function<String, Integer>> funcs = List.of(func1);
        String input = "anotherTest";
        Integer expectedResult = 10;

        // Stub deploymentManager behavior
        when(deploymentManager.executeDeploy(funcs, domain, serviceKey, input)).thenReturn(expectedResult);

        // Act
        Integer result = embeddedDeploy.setExecutableService(domain, serviceKey)
                .execute(strategyType, funcs, input);

        // Assert
        assertEquals(expectedResult, result);
        verify(deploymentManager).setStrategy(strategyType.getValue());
        verify(deploymentManager).executeDeploy(funcs, domain, serviceKey, input);
    }
}

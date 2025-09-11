package com.microswitch.application.executor;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.value.StrategyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentStrategyExecutorTest {

    @Mock
    private DeploymentMetrics deploymentMetrics;

    static class EmptyExecutor extends DeploymentStrategyExecutor {
        EmptyExecutor(InitializerConfiguration properties, DeploymentMetrics metrics) {
            super(properties, metrics);
        }
        @Override
        protected void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
        }
    }

    static class TestExecutor extends DeploymentStrategyExecutor {
        private final DeploymentStrategy canaryStrategy;
        private final DeploymentStrategy shadowStrategy;
        private final DeploymentStrategy blueGreenStrategy;

        TestExecutor(InitializerConfiguration properties, DeploymentMetrics metrics,
                     DeploymentStrategy canary, DeploymentStrategy shadow, DeploymentStrategy blueGreen) {
            super(properties, metrics);
            this.canaryStrategy = canary;
            this.shadowStrategy = shadow;
            this.blueGreenStrategy = blueGreen;
            // Register strategies AFTER super() to avoid pre-construction nulls
            if (this.canaryStrategy != null) addStrategy(StrategyType.CANARY, this.canaryStrategy);
            if (this.shadowStrategy != null) addStrategy(StrategyType.SHADOW, this.shadowStrategy);
            if (this.blueGreenStrategy != null) addStrategy(StrategyType.BLUE_GREEN, this.blueGreenStrategy);
        }

        @Override
        protected void initializeStrategies(InitializerConfiguration properties, DeploymentMetrics deploymentMetrics) {
            // no-op; strategies are registered in the constructor after super()
        }
    }

    @Test
    void executeCanary_withoutRegisteredStrategy_throwsIllegalState() {
        DeploymentStrategyExecutor executor = new EmptyExecutor(new InitializerConfiguration(), null);
        assertThrows(IllegalStateException.class,
                () -> executor.executeCanary(() -> 1, () -> 2, "svc"));
    }

    @Test
    void executeCanary_primaryOnly_recordsStableSuccess() {
        // Strategy calls only primary supplier
        DeploymentStrategy canary = new DeploymentStrategy() {
            @Override
            public <R> R execute(Supplier<R> stable, Supplier<R> experimental, String key) {
                return stable.get();
            }
        };
        TestExecutor executor = new TestExecutor(new InitializerConfiguration(), deploymentMetrics,
                canary, null, null);

        Integer result = executor.executeCanary(() -> 10, () -> 20, "svc1");
        assertEquals(10, result);

        // Only stable recorded as success
        verify(deploymentMetrics, times(1)).recordSuccess("svc1", "stable", "canary");
        verify(deploymentMetrics, never()).recordSuccess("svc1", "experimental", "canary");
        verify(deploymentMetrics, never()).recordError(anyString(), anyString(), anyString());
    }

    @Test
    void executeCanary_bothCalled_recordsBothSuccesses_inOrder() {
        // Strategy calls both suppliers
        DeploymentStrategy canary = new DeploymentStrategy() {
            @Override
            public <R> R execute(Supplier<R> stable, Supplier<R> experimental, String key) {
                Integer a = (Integer) stable.get();
                Integer b = (Integer) experimental.get();
                @SuppressWarnings("unchecked")
                R sum = (R) Integer.valueOf(a + b);
                return sum;
            }
        };
        TestExecutor executor = new TestExecutor(new InitializerConfiguration(), deploymentMetrics,
                canary, null, null);

        Integer result = executor.executeCanary(() -> 1, () -> 2, "svc2");
        assertEquals(3, result);

        InOrder inOrder = inOrder(deploymentMetrics);
        inOrder.verify(deploymentMetrics).recordSuccess("svc2", "stable", "canary");
        inOrder.verify(deploymentMetrics).recordSuccess("svc2", "experimental", "canary");
        verify(deploymentMetrics, never()).recordError(anyString(), anyString(), anyString());
    }

    @Test
    void executeCanary_primaryThrows_recordsStableError_thenRethrows() {
        DeploymentStrategy canary = new DeploymentStrategy() {
            @Override
            public <R> R execute(Supplier<R> stable, Supplier<R> experimental, String key) {
                return stable.get();
            }
        };
        TestExecutor executor = new TestExecutor(new InitializerConfiguration(), deploymentMetrics,
                canary, null, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> executor.executeCanary(() -> { throw new RuntimeException("boom"); }, () -> 2, "svc3"));
        assertEquals("boom", ex.getMessage());

        verify(deploymentMetrics, times(1)).recordError("svc3", "stable", "canary");
        verify(deploymentMetrics, never()).recordSuccess(anyString(), anyString(), anyString());
    }

    @Test
    void executeShadow_bothCalled_recordsBothSuccesses() {
        DeploymentStrategy shadow = new DeploymentStrategy() {
            @Override
            public <R> R execute(Supplier<R> stable, Supplier<R> experimental, String key) {
                String a = (String) stable.get();
                String b = (String) experimental.get();
                @SuppressWarnings("unchecked")
                R res = (R) (a + "+" + b);
                return res;
            }
        };
        TestExecutor executor = new TestExecutor(new InitializerConfiguration(), deploymentMetrics,
                null, shadow, null);

        String result = executor.executeShadow(() -> "S", () -> "E", "svc4");
        assertEquals("S+E", result);
        verify(deploymentMetrics).recordSuccess("svc4", "stable", "shadow");
        verify(deploymentMetrics).recordSuccess("svc4", "experimental", "shadow");
    }

    @Test
    void executeBlueGreen_primaryOnly_recordsStableSuccess() {
        DeploymentStrategy blueGreen = new DeploymentStrategy() {
            @Override
            public <R> R execute(Supplier<R> stable, Supplier<R> experimental, String key) {
                return stable.get();
            }
        };
        TestExecutor executor = new TestExecutor(new InitializerConfiguration(), deploymentMetrics,
                null, null, blueGreen);

        String result = executor.executeBlueGreen(() -> "blue", () -> "green", "svc5");
        assertEquals("blue", result);
        verify(deploymentMetrics).recordSuccess("svc5", "stable", "blue_green");
        verify(deploymentMetrics, never()).recordSuccess("svc5", "experimental", "blue_green");
    }
}

package com.microswitch.infrastructure.manager;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentManagerTest {

    // Minimal fake executor whose class name contains "DeploymentStrategyExecutor"
    // and exposes the expected methods used by DeploymentManager via reflection.
    static class TestDeploymentStrategyExecutor {
        public <R> R executeCanary(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
            return stable.get();
        }
        public <R> R executeShadow(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
            experimental.get();
            return stable.get();
        }
        public <R> R executeBlueGreen(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
            return experimental.get();
        }
    }

    @Test
    void canary_delegatesAndReturnsStable() {
        DeploymentManager manager = DeploymentManager.createWithExecutor(new TestDeploymentStrategyExecutor());

        Supplier<Integer> stable = () -> 1;
        Supplier<Integer> experimental = () -> 2;
        Integer result = manager.canary(stable, experimental, "svc");

        assertEquals(1, result);
    }

    @Test
    void shadow_delegatesAndReturnsStable() {
        DeploymentManager manager = DeploymentManager.createWithExecutor(new TestDeploymentStrategyExecutor());

        Supplier<String> stable = () -> "stable";
        Supplier<String> experimental = () -> "shadow";
        String result = manager.shadow(stable, experimental, "svc");

        assertEquals("stable", result);
    }

    @Test
    void blueGreen_delegatesAndReturnsExperimental() {
        DeploymentManager manager = DeploymentManager.createWithExecutor(new TestDeploymentStrategyExecutor());

        Supplier<String> stable = () -> "blue";
        Supplier<String> experimental = () -> "green";
        String result = manager.blueGreen(stable, experimental, "svc");

        assertEquals("green", result);
    }

    @Test
    void createWithExecutor_null_throwsNpe() {
        assertThrows(NullPointerException.class, () -> DeploymentManager.createWithExecutor(null));
    }

    @Test
    void createWithExecutor_invalidTypeName_throwsIllegalArgument() {
        class NotExecutor {}
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DeploymentManager.createWithExecutor(new NotExecutor()));
        assertTrue(ex.getMessage().contains("Invalid strategy executor type"));
    }

    @Test
    void invokeStrategy_missingMethod_throwsIllegalState() {
        class BadDeploymentStrategyExecutor {
            <R> R wrongMethod(Supplier<R> s1, Supplier<R> s2, String key) { return s1.get(); }
        }
        DeploymentManager manager = DeploymentManager.createWithExecutor(new BadDeploymentStrategyExecutor());

        Supplier<String> stable = () -> "a";
        Supplier<String> experimental = () -> "b";

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> manager.canary(stable, experimental, "svc"));
        assertTrue(ex.getMessage().contains("Failed to invoke strategy method"));
    }
}

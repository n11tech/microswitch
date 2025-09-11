package com.microswitch.domain.strategy;

import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.value.MethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ShadowTest {

    private InitializerConfiguration properties;
    private Shadow shadowStrategy;

    @BeforeEach
    void setUp() {
        properties = new InitializerConfiguration();
        Map<String, InitializerConfiguration.DeployableServices> services = new HashMap<>();
        properties.setServices(services);
        shadowStrategy = new Shadow(properties);
    }

    @Test
    void testExecuteWithDisabledService_returnsPrimary() {
        String serviceKey = "disabled-service";
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        String result = shadowStrategy.execute(primary, secondary, serviceKey);
        assertEquals("primary", result);
    }

    @Test
    void testServiceKeyNull_throwsIllegalArgument() {
        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";
        assertThrows(IllegalArgumentException.class, () -> shadowStrategy.execute(primary, secondary, null));
        assertThrows(IllegalArgumentException.class, () -> shadowStrategy.execute(primary, secondary, " "));
    }

    @Test
    void testEnabledServiceButNoShadowConfig_returnsPrimary() {
        String serviceKey = "no-shadow";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        assertEquals("primary", shadowStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testMirrorPercentageNullOrZero_returnsPrimary() {
        String serviceKey = "zero-mirror";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var shadow = new InitializerConfiguration.Shadow();
        shadow.setStable(MethodType.PRIMARY);
        shadow.setMirror(MethodType.SECONDARY);
        shadow.setMirrorPercentage((short) 0);
        deployable.setShadow(shadow);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        assertEquals("primary", shadowStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testStableMethodPrimary_nonMirrorPath_returnsPrimary() {
        String serviceKey = "stable-primary-nonmirror";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var shadow = new InitializerConfiguration.Shadow();
        shadow.setStable(MethodType.PRIMARY);
        shadow.setMirror(MethodType.SECONDARY);
        shadow.setMirrorPercentage((short) 50); // interval = 2, first call won't mirror
        deployable.setShadow(shadow);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        assertEquals("primary", shadowStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testStableMethodSecondary_nonMirrorPath_returnsSecondary() {
        String serviceKey = "stable-secondary-nonmirror";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var shadow = new InitializerConfiguration.Shadow();
        shadow.setStable(MethodType.SECONDARY);
        shadow.setMirror(MethodType.PRIMARY);
        shadow.setMirrorPercentage((short) 50); // interval = 2, first call won't mirror
        deployable.setShadow(shadow);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> primary = () -> "primary";
        Supplier<String> secondary = () -> "secondary";

        assertEquals("secondary", shadowStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testMirrorTriggered_returnsStableEvenIfMirrorThrows() {
        String serviceKey = "mirror-triggered";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var shadow = new InitializerConfiguration.Shadow();
        shadow.setStable(MethodType.PRIMARY);
        shadow.setMirror(MethodType.SECONDARY);
        shadow.setMirrorPercentage((short) 100); // interval = 1, always mirror
        deployable.setShadow(shadow);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> primary = () -> "stable";
        Supplier<String> secondary = () -> { throw new RuntimeException("mirror failed"); };

        // Should not throw; mirror failure is handled, stable result returned
        assertEquals("stable", shadowStrategy.execute(primary, secondary, serviceKey));
    }

    @Test
    void testMirrorTriggered_returnsStableWhenResultsDiffer() {
        String serviceKey = "mirror-diff";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var shadow = new InitializerConfiguration.Shadow();
        shadow.setStable(MethodType.PRIMARY);
        shadow.setMirror(MethodType.SECONDARY);
        shadow.setMirrorPercentage((short) 100); // interval = 1, always mirror
        deployable.setShadow(shadow);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> primary = () -> "A";
        Supplier<String> secondary = () -> "B";

        assertEquals("A", shadowStrategy.execute(primary, secondary, serviceKey));
    }
}

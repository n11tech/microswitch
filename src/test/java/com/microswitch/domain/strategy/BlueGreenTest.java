package com.microswitch.domain.strategy;

import com.microswitch.domain.InitializerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class BlueGreenTest {

    private InitializerConfiguration properties;
    private BlueGreen blueGreenStrategy;

    @BeforeEach
    void setUp() {
        properties = new InitializerConfiguration();
        Map<String, InitializerConfiguration.DeployableServices> services = new HashMap<>();
        properties.setServices(services);
        blueGreenStrategy = new BlueGreen(properties);
    }

    @Test
    void testExecuteWithDisabledService() {
        // Given
        String serviceKey = "disabled-service";
        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        // When
        String result = blueGreenStrategy.execute(blue, green, serviceKey);

        // Then
        assertEquals("blue", result);
    }

    @Test
    void testNullServiceKeyThrows() {
        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";
        assertThrows(IllegalArgumentException.class, () -> blueGreenStrategy.execute(blue, green, null));
        assertThrows(IllegalArgumentException.class, () -> blueGreenStrategy.execute(blue, green, " "));
    }

    @Test
    void testNullBlueSupplierThrows() {
        String serviceKey = "svc";
        Supplier<String> green = () -> "green";
        assertThrows(IllegalArgumentException.class, () -> blueGreenStrategy.execute(null, green, serviceKey));
    }

    @Test
    void testNullGreenSupplierThrows() {
        String serviceKey = "svc";
        Supplier<String> blue = () -> "blue";
        assertThrows(IllegalArgumentException.class, () -> blueGreenStrategy.execute(blue, null, serviceKey));
    }

    @Test
    void testEnabledServiceButNoBlueGreenConfig_defaultsToBlue() {
        String serviceKey = "test-service";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        String result = blueGreenStrategy.execute(blue, green, serviceKey);
        assertEquals("blue", result);
    }

    @Test
    void testWeightOnly_blueActive_1_0() {
        String serviceKey = "weight-blue";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var blueGreen = new InitializerConfiguration.BlueGreen();
        blueGreen.setWeight("1/0");
        deployable.setBlueGreen(blueGreen);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        String result = blueGreenStrategy.execute(blue, green, serviceKey);
        assertEquals("blue", result);
    }

    @Test
    void testWeightOnly_greenActive_0_1() {
        String serviceKey = "weight-green";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var blueGreen = new InitializerConfiguration.BlueGreen();
        blueGreen.setWeight("0/1");
        deployable.setBlueGreen(blueGreen);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        String result = blueGreenStrategy.execute(blue, green, serviceKey);
        assertEquals("green", result);
    }

    @Test
    void testInvalidWeightFormat_defaultsToBlue() {
        String serviceKey = "invalid-weight";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var blueGreen = new InitializerConfiguration.BlueGreen();
        blueGreen.setWeight("2/3"); // not binary
        deployable.setBlueGreen(blueGreen);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        // Behavior: config creation logs and returns null; execute() defaults to blue
        assertEquals("blue", blueGreenStrategy.execute(blue, green, serviceKey));
    }

    @Test
    void testTtlOnly_zeroMeansAlwaysBlue() {
        String serviceKey = "ttl-zero";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var blueGreen = new InitializerConfiguration.BlueGreen();
        blueGreen.setTtl(0L); // infinite (never switch) -> blue
        deployable.setBlueGreen(blueGreen);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        String result = blueGreenStrategy.execute(blue, green, serviceKey);
        assertEquals("blue", result);
    }

    @Test
    void testTtlOnly_positiveBeforeExpiry_isBlue() {
        String serviceKey = "ttl-before-expiry";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var blueGreen = new InitializerConfiguration.BlueGreen();
        blueGreen.setTtl(2L); // switch to green after 2s
        deployable.setBlueGreen(blueGreen);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        String result = blueGreenStrategy.execute(blue, green, serviceKey);
        assertEquals("blue", result);
    }

    @Test
    void testWeightAndTtl_startGreen_switchToBlueAfterTtl() throws InterruptedException {
        String serviceKey = "wg-green-to-blue";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var blueGreen = new InitializerConfiguration.BlueGreen();
        blueGreen.setWeight("0/1"); // start Green
        blueGreen.setTtl(1L);       // after 1s switch to Blue
        deployable.setBlueGreen(blueGreen);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        // Initially Green
        assertEquals("green", blueGreenStrategy.execute(blue, green, serviceKey));
        // After TTL -> Blue
        Thread.sleep(1100);
        assertEquals("blue", blueGreenStrategy.execute(blue, green, serviceKey));
    }

    @Test
    void testWeightAndTtl_startBlue_switchToGreenAfterTtl() throws InterruptedException {
        String serviceKey = "wg-blue-to-green";
        var deployable = new InitializerConfiguration.DeployableServices();
        deployable.setEnabled(true);
        var blueGreen = new InitializerConfiguration.BlueGreen();
        blueGreen.setWeight("1/0"); // start Blue
        blueGreen.setTtl(1L);       // after 1s switch to Green
        deployable.setBlueGreen(blueGreen);
        properties.getServices().put(serviceKey, deployable);

        Supplier<String> blue = () -> "blue";
        Supplier<String> green = () -> "green";

        // Initially Blue
        assertEquals("blue", blueGreenStrategy.execute(blue, green, serviceKey));
        // After TTL -> Green
        Thread.sleep(1100);
        assertEquals("green", blueGreenStrategy.execute(blue, green, serviceKey));
    }
}

package com.n11.architecture.tools.microswitch.infrastructure.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrategyTypeTest {

    @Test
    void testFromValueWithValidValues() {
        // Verify each valid strategy type
        assertEquals(StrategyType.CANARY, StrategyType.fromValue("canary"));
        assertEquals(StrategyType.BLUE_GREEN, StrategyType.fromValue("blueGreen"));
        assertEquals(StrategyType.AB, StrategyType.fromValue("ab"));
        assertEquals(StrategyType.SHADOW, StrategyType.fromValue("shadow"));

        // Verify that the method is case-insensitive
        assertEquals(StrategyType.CANARY, StrategyType.fromValue("CANARY"));
        assertEquals(StrategyType.BLUE_GREEN, StrategyType.fromValue("BLUEGREEN"));
        assertEquals(StrategyType.AB, StrategyType.fromValue("AB"));
        assertEquals(StrategyType.SHADOW, StrategyType.fromValue("SHADOW"));
    }

    @Test
    void testFromValueWithInvalidValue() {
        // Verify that an exception is thrown for an unknown strategy
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StrategyType.fromValue("invalidStrategy");
        });

        assertEquals("Unknown strategy: invalidStrategy", exception.getMessage());
    }

    @Test
    void testGetValue() {
        // Verify that the value returned by getValue matches the expected string
        assertEquals("canary", StrategyType.CANARY.getValue());
        assertEquals("blueGreen", StrategyType.BLUE_GREEN.getValue());
        assertEquals("ab", StrategyType.AB.getValue());
        assertEquals("shadow", StrategyType.SHADOW.getValue());
    }
}

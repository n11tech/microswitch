package com.microswitch.domain.value;

import lombok.Getter;

/**
 * Enum representing different deployment strategy types.
 * 
 * <p>This enum follows best practices:
 * - Immutable with final fields
 * - Type-safe strategy identification
 * - Case-insensitive value matching
 * - Clear error messages for invalid values
 */
@Getter
public enum StrategyType {
    CANARY("canary"),
    BLUE_GREEN("blueGreen"),
    SHADOW("shadow");

    /**
     * -- GETTER --
     *  Gets the string value of this strategy type.
     *
     */
    private final String value;

    StrategyType(String value) {
        this.value = value;
    }

    /**
     * Converts a string value to the corresponding StrategyType.
     * 
     * @param value the string value to convert (case-insensitive)
     * @return the corresponding StrategyType
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static StrategyType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Strategy value cannot be null");
        }
        
        for (StrategyType strategyType : StrategyType.values()) {
            if (strategyType.value.equalsIgnoreCase(value.trim())) {
                return strategyType;
            }
        }
        throw new IllegalArgumentException("Unknown strategy: " + value);
    }
}

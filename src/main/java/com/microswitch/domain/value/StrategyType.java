package com.microswitch.domain.value;

import lombok.Getter;

@Getter
public enum StrategyType {
    CANARY("canary"),
    BLUE_GREEN("blueGreen"),
    SHADOW("shadow");

    private final String value;

    StrategyType(String value) {
        this.value = value;
    }

    public static StrategyType fromValue(String value) {
        for (StrategyType strategyType : StrategyType.values()) {
            if (strategyType.value.equalsIgnoreCase(value)) {
                return strategyType;
            }
        }
        throw new IllegalArgumentException("Unknown strategy: " + value);
    }
}

package com.n11.development.infrastructure.strategy;

import lombok.Getter;

@Getter
public enum StrategyType {
    CANARY("canary"),
    BLUE_GREEN("blueGreen"),
    AB("ab"),
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

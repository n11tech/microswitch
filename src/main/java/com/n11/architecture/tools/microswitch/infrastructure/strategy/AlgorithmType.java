package com.n11.architecture.tools.microswitch.infrastructure.strategy;

import lombok.Getter;

@Getter
public enum AlgorithmType {
    SEQUENCE("sequence"),
    RANDOM("random");

    private final String value;

    AlgorithmType(String value) {
        this.value = value;
    }

    public static AlgorithmType fromValue(String value) {
        for (AlgorithmType strategyType : AlgorithmType.values()) {
            if (strategyType.value.equalsIgnoreCase(value)) {
                return strategyType;
            }
        }
        throw new IllegalArgumentException("Unknown algorithm: " + value);
    }
}

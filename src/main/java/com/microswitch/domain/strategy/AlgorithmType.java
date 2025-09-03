package com.microswitch.domain.strategy;

import lombok.Getter;

@Getter
public enum AlgorithmType {
    SEQUENCE("sequence"),
    RANDOM("random");

    private final String value;

    AlgorithmType(String value) {
        this.value = value;
    }


}

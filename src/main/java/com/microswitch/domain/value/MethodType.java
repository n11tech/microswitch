package com.microswitch.domain.value;

import lombok.Getter;

@Getter
public enum MethodType {
    PRIMARY("primary"),
    SECONDARY("secondary");

    private final String value;

    MethodType(String value) {
        this.value = value;
    }
}

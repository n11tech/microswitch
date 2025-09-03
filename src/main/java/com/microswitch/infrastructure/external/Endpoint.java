package com.microswitch.infrastructure.external;

import com.microswitch.domain.InitializerConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

@org.springframework.boot.actuate.endpoint.annotation.Endpoint(id = "microswitch")
public class Endpoint {

    private final InitializerConfiguration properties;

    public Endpoint(InitializerConfiguration properties) {
        this.properties = properties;
    }

    @ReadOperation
    public InitializerConfiguration configurations() {
        return properties;
    }
}

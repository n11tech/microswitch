package com.n11.development.actuator;

import com.n11.development.properties.MicroswitchProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

@Endpoint(id = "microswitch")
public class MicroswitchEndpoint {

    private final MicroswitchProperties properties;

    public MicroswitchEndpoint(MicroswitchProperties properties) {
        this.properties = properties;
    }

    @ReadOperation
    public MicroswitchProperties configurations() {
        return properties;
    }
}

package com.microswitch.domain.strategy;

import com.microswitch.domain.InitializerConfiguration;

public abstract class DeployTemplate {
    protected final InitializerConfiguration properties;

    protected DeployTemplate(InitializerConfiguration properties) {
        this.properties = properties;
    }
}

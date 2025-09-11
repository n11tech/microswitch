package com.microswitch.domain.strategy;

import com.microswitch.domain.InitializerConfiguration;

import java.util.function.Supplier;

public abstract class DeployTemplate {
    protected final InitializerConfiguration configuration;

    protected DeployTemplate(InitializerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Validates the service key and retrieves the service configuration.
     * Returns the primary result if validation fails or service is disabled.
     *
     * @param serviceKey the service key to validate
     * @param primary    the primary supplier to execute if validation fails
     * @param <R>        the return type
     * @return the service configuration if valid, or null if primary should be executed
     */
    protected <R> InitializerConfiguration.DeployableServices validateServiceAndGetConfig(String serviceKey, Supplier<R> primary) {
        if (serviceKey == null || serviceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Service key cannot be null or empty");
        }

        var deployableService = configuration.getServices().get(serviceKey);
        if (deployableService == null || !deployableService.isEnabled()) {
            return null;
        }

        return deployableService;
    }

    /**
     * Executes primary supplier if the service configuration is null (validation failed).
     * This is a helper method to reduce boilerplate in strategy implementations.
     *
     * @param deployableServices the service configuration (can be null)
     * @param primary            the primary supplier
     * @param <R>                the return type
     * @return the result from primary supplier, or null if deployableServices is valid
     */
    protected <R> R executeIfServiceInvalid(InitializerConfiguration.DeployableServices deployableServices, Supplier<R> primary) {
        if (deployableServices == null) {
            return primary.get();
        }
        return null;
    }
}

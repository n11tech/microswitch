package com.n11.architecture.tools.microswitch.infrastructure.strategy;

import com.n11.architecture.tools.microswitch.application.config.EmbDeployer;
import com.n11.architecture.tools.microswitch.application.config.IEmbDeployerLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

class DeployTemplateTest {
    static class TestDeployTemplate extends DeployTemplate {
        protected TestDeployTemplate(Environment environment, IEmbDeployerLoader embDeployerLoader) {
            super(embDeployerLoader);
        }
    }

    @Mock
    private EmbDeployer embDeployer;

    @Mock
    private Environment environment;

    @Mock
    private IEmbDeployerLoader embDeployerLoader;

    private DeployTemplate deployTemplate;
    private EmbDeployer.ServiceConfig _service;
    private EmbDeployer.ServiceConfig.Spec _spec;
    private EmbDeployer.ServiceConfig.Spec.Deployment _deployment;

    private final static String domain = "basket";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deployTemplate = new TestDeployTemplate(environment, embDeployerLoader);
        _service = new EmbDeployer.ServiceConfig();
        _deployment = new EmbDeployer.ServiceConfig.Spec.Deployment();
        _spec = new EmbDeployer.ServiceConfig.Spec();
    }

    @Test
    void testGetEmbeddedService_WithValidStrategy() {
        // Arrange
        String serviceKey = "service1";
        StrategyType strategyType = StrategyType.CANARY;

        _deployment.setStrategy("canary");
        _spec.setDeployment(_deployment);
        _service.setSpec(_spec);

        Map<String, EmbDeployer.ServiceConfig> services = Map.of(serviceKey, _service);
        when(embDeployerLoader.getConfiguration(domain)).thenReturn(services);
        when(embDeployer.getEmbeddedServices()).thenReturn(services);

        // Act
        var result = deployTemplate.getEmbeddedService(domain, serviceKey, strategyType);

        // Assert
        assertEquals(_service, result);
    }

    @Test
    void testGetEmbeddedService_WithInvalidStrategy() {
        // Arrange
        String serviceKey = "service1";
        StrategyType strategyType = StrategyType.CANARY;

        _deployment.setStrategy("blueGreen");
        _spec.setDeployment(_deployment);
        _service.setSpec(_spec);

        Map<String, EmbDeployer.ServiceConfig> services = new HashMap<>();
        services.put(serviceKey, _service);

        when(embDeployer.getEmbeddedServices()).thenReturn(services);

        // Act
        var result = deployTemplate.getEmbeddedService(domain, serviceKey, strategyType);

        // Assert
        assertNotEquals(_service, result);
        assertNull(result.getMetadata());
    }

    @Test
    void testGetEmbeddedService_ServiceNotFound() {
        // Arrange
        String serviceKey = "nonExistentService";
        StrategyType strategyType = StrategyType.CANARY;

        when(embDeployer.getEmbeddedServices()).thenReturn(new HashMap<>());

        // Act
        var result = deployTemplate.getEmbeddedService(domain, serviceKey, strategyType);

        // Assert
        assertNull(result.getMetadata());
    }

    @Test
    void testDeployScopeIsEnabled_WithEnabledService() {
        // Arrange
        _deployment.setEnabled(true);
        _spec.setDeployment(_deployment);
        _service.setSpec(_spec);

        _service.setMetadata(new EmbDeployer.ServiceConfig.Metadata());

        // Act
        boolean result = deployTemplate.deployScopeIsEnabled(_service);

        // Assert
        assertTrue(result);
    }

    @Test
    void testDeployScopeIsEnabled_WithDisabledService() {
        // Arrange
        _deployment.setEnabled(false);
        _spec.setDeployment(_deployment);
        _service.setSpec(_spec);

        _service.setMetadata(new EmbDeployer.ServiceConfig.Metadata());

        // Act
        boolean result = deployTemplate.deployScopeIsEnabled(_service);

        // Assert
        assertFalse(result);
    }

    @Test
    void testDeployScopeIsEnabled_WithNullMetadata() {
        // Arrange
        _deployment.setEnabled(true);
        _spec.setDeployment(_deployment);
        _service.setSpec(_spec);

        // Act
        boolean result = deployTemplate.deployScopeIsEnabled(_service);

        // Assert
        assertFalse(result); // Should be false due to null metadata
    }
}

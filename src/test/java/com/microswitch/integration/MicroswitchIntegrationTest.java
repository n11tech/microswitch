package com.microswitch.integration;

import com.microswitch.infrastructure.external.DeploymentManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestConfiguration.class)
@TestPropertySource(properties = {
    "microswitch.enabled=true",
    "microswitch.services.test-service.enabled=true",
    "microswitch.services.test-service.canary.primary-percentage=50",
    "microswitch.services.test-service.canary.algorithm=random"
})
class MicroswitchIntegrationTest {

    @Autowired
    private DeploymentManager deploymentManager;

    @Test
    void testCanaryDeploymentIntegration() {
        // When
        String result = deploymentManager.canary(
            () -> "Stable Version",
            () -> "Experimental Version",
            "test-service"
        );

        // Then
        assertThat(result).isIn("Stable Version", "Experimental Version");
    }

    @Test
    void testShadowDeploymentIntegration() {
        // When
        String result = deploymentManager.shadow(
            () -> "Primary Result",
            () -> "Shadow Result",
            "test-service"
        );

        // Then
        assertThat(result).isEqualTo("Primary Result"); // Shadow always returns primary
    }

    @Test
    void testBlueGreenDeploymentIntegration() {
        // When
        String result = deploymentManager.blueGreen(
            () -> "Blue Version",
            () -> "Green Version",
            "test-service"
        );

        // Then
        assertThat(result).isIn("Blue Version", "Green Version");
    }
}

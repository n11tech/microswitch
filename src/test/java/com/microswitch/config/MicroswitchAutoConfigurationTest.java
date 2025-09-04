package com.microswitch.config;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.infrastructure.manager.DeploymentManager;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.application.executor.DeploymentStrategyExecutor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to verify that MicroswitchAutoConfiguration properly configures all beans
 * when the library is included in a Spring Boot application.
 */
class MicroswitchAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MicroswitchAutoConfiguration.class))
            .withUserConfiguration(TestMeterRegistryConfiguration.class);

    @Test
    void shouldAutoConfigureAllMicroswitchBeans() {
        contextRunner.run(context -> {
            // Verify that all expected beans are created
            assertThat(context).hasSingleBean(InitializerConfiguration.class);
            assertThat(context).hasSingleBean(DeploymentMetrics.class);
            assertThat(context).hasSingleBean(DeploymentStrategyExecutor.class);
            assertThat(context).hasSingleBean(DeploymentManager.class);
        });
    }

    @Test
    void shouldConfigureWithDefaultProperties() {
        contextRunner.run(context -> {
            InitializerConfiguration config = context.getBean(InitializerConfiguration.class);
            assertThat(config).isNotNull(); // Verify config bean exists
        });
    }

    @Configuration
    static class TestMeterRegistryConfiguration {
        @Bean
        public SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}

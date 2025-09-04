package com.microswitch.config;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.infrastructure.external.DeploymentManager;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.application.executor.DeploymentStrategyExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Microswitch library.
 *
 * <p>This configuration automatically sets up all necessary beans for the microswitch
 * deployment strategies when the library is included in a Spring Boot application.
 *
 * <p>The configuration is conditional and will only activate when the required classes
 * are present on the classpath. All beans can be overridden by providing custom
 * implementations in the consuming application.
 *
 * @author N11 Development Team
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnClass({DeploymentManager.class})
@EnableConfigurationProperties(InitializerConfiguration.class)
public class MicroswitchAutoConfiguration {

    /**
     * Creates the DeploymentManager bean if none exists.
     *
     * @param strategyFactory the deployment strategy factory
     * @return configured deployment manager
     */
    @Bean
    @ConditionalOnMissingBean(DeploymentManager.class)
    public DeploymentManager deploymentManager(DeploymentStrategyExecutor strategyFactory) {
        return new DeploymentManager(strategyFactory);
    }

    /**
     * Creates the DeploymentMetrics bean if none exists.
     *
     * @param meterRegistry the micrometer meter registry
     * @return configured deployment metrics service
     */
    @Bean
    @ConditionalOnMissingBean(DeploymentMetrics.class)
    public DeploymentMetrics deploymentMetrics(MeterRegistry meterRegistry) {
        return new DeploymentMetrics(meterRegistry);
    }
}

package com.microswitch.application.config;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.application.metric.NoOpDeploymentMetrics;
import com.microswitch.infrastructure.manager.DeploymentManager;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.application.executor.DeploymentStrategyExecutor;
import com.microswitch.application.executor.MicroswitchDeploymentStrategyExecutor;
import com.microswitch.infrastructure.external.Endpoint;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
@Slf4j
@AutoConfiguration
@ConditionalOnClass({DeploymentManager.class})
@EnableConfigurationProperties(InitializerConfiguration.class)
@ComponentScan(basePackages = {
        "com.microswitch.application.metric"
})
public class MicroswitchAutoConfiguration {

    private final InitializerConfiguration properties;

    public MicroswitchAutoConfiguration(InitializerConfiguration properties) {
        this.properties = properties;
    }

    @Bean
    public String logInitialization() {
        if (properties != null && properties.isEnabled()) {
            log.info("Microswitch library initialized and ENABLED - deployment strategies are now available");
            return "microswitch-enabled";
        } else {
            log.info("Microswitch library initialized but DISABLED - DeploymentManager bean is available but strategies are disabled");
            return "microswitch-disabled";
        }
    }

    /**
     * Creates the DeploymentStrategyExecutor bean if none exists.
     *
     * @param properties the microswitch configuration properties
     * @param deploymentMetrics the deployment metrics (optional)
     * @return configured deployment strategy executor
     */
    @Bean
    @ConditionalOnMissingBean(DeploymentStrategyExecutor.class)
    public DeploymentStrategyExecutor deploymentStrategyExecutor(InitializerConfiguration properties, 
                                                                @Autowired(required = false) DeploymentMetrics deploymentMetrics) {
        return new MicroswitchDeploymentStrategyExecutor(properties, deploymentMetrics);
    }

    /**
     * Creates the DeploymentManager bean if none exists.
     *
     * @param strategyFactory the deployment strategy executor
     * @return configured deployment manager
     */
    @Bean
    @ConditionalOnMissingBean(DeploymentManager.class)
    public DeploymentManager deploymentManager(DeploymentStrategyExecutor strategyFactory) {
        // Call the public factory method directly (no reflection)
        return DeploymentManager.createWithExecutor(strategyFactory);
    }

    /**
     * Creates the DeploymentMetrics bean if none exists.
     * If a MeterRegistry is available, creates a real DeploymentMetrics.
     * Otherwise, creates a NoOpDeploymentMetrics to prevent null injection.
     *
     * @param meterRegistry the micrometer meter registry (optional)
     * @return configured deployment metrics (never null)
     */
    @Bean
    @ConditionalOnMissingBean(DeploymentMetrics.class)
    public DeploymentMetrics deploymentMetrics(@Autowired(required = false) MeterRegistry meterRegistry) {
        if (meterRegistry != null) {
            return new DeploymentMetrics(meterRegistry);
        } else {
            return new NoOpDeploymentMetrics();
        }
    }


    /**
     * Configuration for Spring Boot Actuator integration.
     * 
     * <p>This nested configuration class provides actuator endpoint beans
     * when Spring Boot Actuator is present on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    protected static class MicroswitchActuatorConfiguration {

        /**
         * Creates the microswitch actuator endpoint bean.
         *
         * @param properties the microswitch configuration properties
         * @return configured microswitch endpoint
         */
        @Bean
        @ConditionalOnMissingBean(Endpoint.class)
        public Endpoint microswitchEndpoint(InitializerConfiguration properties) {
            return new Endpoint(properties);
        }
    }
}

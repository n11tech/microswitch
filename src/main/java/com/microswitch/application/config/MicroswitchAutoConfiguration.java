package com.microswitch.application.config;

import com.microswitch.application.metric.DeploymentMetrics;
import com.microswitch.infrastructure.manager.DeploymentManager;
import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.application.executor.DeploymentStrategyExecutor;
import com.microswitch.application.executor.MicroswitchDeploymentStrategyExecutor;
import com.microswitch.infrastructure.external.Endpoint;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@AutoConfiguration
@ConditionalOnProperty(prefix = "microswitch", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({DeploymentManager.class})
@EnableConfigurationProperties(InitializerConfiguration.class)
@ComponentScan(basePackages = {
        "com.microswitch.infrastructure.external",
        "com.microswitch.application.metric"
})
public class MicroswitchAutoConfiguration {

    /**
     * Creates the DeploymentStrategyExecutor bean if none exists.
     *
     * @param properties the microswitch configuration properties
     * @param deploymentMetrics the deployment metrics service (optional)
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
     * @param strategyFactory the deployment strategy factory
     * @return configured deployment manager
     */
    @Bean
    @ConditionalOnMissingBean(DeploymentManager.class)
    public DeploymentManager deploymentManager(DeploymentStrategyExecutor strategyFactory) {
        // Use reflection to call the package-private factory method
        try {
            var factoryMethod = DeploymentManager.class.getDeclaredMethod(
                "createWithExecutor", Object.class);
            factoryMethod.setAccessible(true);
            return (DeploymentManager) factoryMethod.invoke(null, strategyFactory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create DeploymentManager", e);
        }
    }

    /**
     * Creates the DeploymentMetrics bean if none exists.
     *
     * @param meterRegistry the micrometer meter registry
     * @return configured deployment metrics service
     */
    @Bean
    @ConditionalOnMissingBean(DeploymentMetrics.class)
    @ConditionalOnBean(MeterRegistry.class)
    public DeploymentMetrics deploymentMetrics(MeterRegistry meterRegistry) {
        return new DeploymentMetrics(meterRegistry);
    }

    /**
     * Configuration for Spring Boot Actuator integration.
     * 
     * <p>This nested configuration class provides actuator endpoint beans
     * when Spring Boot Actuator is present on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(org.springframework.boot.actuate.endpoint.annotation.Endpoint.class)
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

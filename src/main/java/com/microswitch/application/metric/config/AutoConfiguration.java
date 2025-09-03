package com.microswitch.application.metric.config;

import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.infrastructure.external.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@org.springframework.boot.autoconfigure.AutoConfiguration
@ConditionalOnProperty(prefix = "microswitch", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(InitializerConfiguration.class)
@ComponentScan(basePackages = {
        "com.microswitch.infrastructure",
        "com.microswitch.application.actuator",
        "com.microswitch.application.metric"
})
public class AutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(org.springframework.boot.actuate.endpoint.annotation.Endpoint.class)
    protected static class MicroswitchActuatorConfiguration {

        @Bean
        public Endpoint microswitchEndpoint(InitializerConfiguration properties) {
            return new Endpoint(properties);
        }
    }
}

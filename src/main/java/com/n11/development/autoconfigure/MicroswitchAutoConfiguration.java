package com.n11.development.autoconfigure;

import com.n11.development.actuator.MicroswitchEndpoint;
import com.n11.development.properties.MicroswitchProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnProperty(prefix = "microswitch", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MicroswitchProperties.class)
@ComponentScan(basePackages = {
        "com.n11.development.infrastructure",
        "com.n11.development.actuator",
        "com.n11.development.core"
})
public class MicroswitchAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Endpoint.class)
    protected static class MicroswitchActuatorConfiguration {

        @Bean
        public MicroswitchEndpoint microswitchEndpoint(MicroswitchProperties properties) {
            return new MicroswitchEndpoint(properties);
        }
    }
}

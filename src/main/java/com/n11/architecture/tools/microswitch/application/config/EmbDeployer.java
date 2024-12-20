package com.n11.architecture.tools.microswitch.application.config;

import com.n11.architecture.tools.microswitch.infrastructure.strategy.AlgorithmType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Getter
@Setter
public class EmbDeployer {
    private String version;
    private String kind;
    private String domain;
    private String platform;
    private Map<String, ServiceConfig> embeddedServices;

    @Getter
    @Setter
    public static class ServiceConfig {
        private Metadata metadata;
        private Spec spec;

        @Getter
        @Setter
        public static class Metadata {
            private String name;
        }

        @Getter
        @Setter
        public static class Spec {
            private Deployment deployment;

            @Getter
            @Setter
            public static class Deployment {
                private String strategy;
                private boolean enabled;
                private String canaryPercentage;
                private String algorithm;
                private String weight;
                private Long ttl;
                private Short shadowWeight;

                @PostConstruct
                public void init() {
                    if (this.algorithm == null) {
                        this.algorithm = AlgorithmType.SEQUENCE.getValue();
                    }
                }
            }
        }
    }
}

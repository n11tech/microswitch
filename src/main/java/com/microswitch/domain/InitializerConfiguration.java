package com.microswitch.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.microswitch.domain.value.AlgorithmType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "microswitch")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitializerConfiguration {

    private boolean enabled = true;
    private Map<String, DeployableServices> services;

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeployableServices {
        private boolean enabled = true;
        private Canary canary;
        private BlueGreen blueGreen;
        private Shadow shadow;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Canary {
        private Integer primaryPercentage;
        private String algorithm = AlgorithmType.SEQUENCE.getValue().toLowerCase();
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BlueGreen {
        private String weight;
        private Long ttl;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Shadow {
        private Short weight;
    }
}

package com.microswitch.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "microswitch")
@Getter
@Setter
public class InitializerConfiguration {

    private boolean enabled = true;
    private String logger = "disable"; // Default: disable execution logging
    private java.util.Map<String, DeployableServices> services = new java.util.HashMap<>();

    @Getter
    @Setter
    public static class DeployableServices {
        private boolean enabled = true;
        private String activeStrategy = "canary";
        private Canary canary = new Canary();
        private BlueGreen blueGreen = new BlueGreen();
        private Shadow shadow = new Shadow();
    }

    @Getter
    @Setter
    public static class Canary {
        private String percentage = "50/50";
        private String algorithm = "sequential";
    }

    @Getter
    @Setter
    public static class BlueGreen {
        private String weight = "1/0";
        private int ttl = 300;

        public void setTtl(long ttl) {
            this.ttl = (int) ttl;
        }
    }

    @Getter
    @Setter
    public static class Shadow {
        private int percentage = 20;
        private com.microswitch.domain.value.MethodType stable = com.microswitch.domain.value.MethodType.PRIMARY;
        private com.microswitch.domain.value.MethodType mirror = com.microswitch.domain.value.MethodType.SECONDARY;
        private String comparator = "disable"; // Default: disable deep comparison

        // Legacy methods for backward compatibility with Shadow strategy
        public Integer getMirrorPercentage() {
            return percentage;
        }

        public void setMirrorPercentage(Integer mirrorPercentage) {
            this.percentage = mirrorPercentage != null ? mirrorPercentage : 20;
        }

        public void setMirrorPercentage(short mirrorPercentage) {
            this.percentage = mirrorPercentage;
        }

        public void setMirrorPercentage(Short mirrorPercentage) {
            this.percentage = mirrorPercentage != null ? mirrorPercentage : 20;
        }

        public void setStable(com.microswitch.domain.value.MethodType stable) {
            this.stable = stable != null ? stable : com.microswitch.domain.value.MethodType.PRIMARY;
        }

        public void setMirror(com.microswitch.domain.value.MethodType mirror) {
            this.mirror = mirror != null ? mirror : com.microswitch.domain.value.MethodType.SECONDARY;
        }
    }
}

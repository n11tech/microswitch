package com.microswitch.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microswitch")
public class InitializerConfiguration {

    private boolean enabled = true;
    private java.util.Map<String, DeployableServices> services = new java.util.HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public java.util.Map<String, DeployableServices> getServices() {
        return services;
    }

    public void setServices(java.util.Map<String, DeployableServices> services) {
        this.services = services;
    }

    public static class DeployableServices {
        private boolean enabled = true;
        private String activeStrategy = "canary";
        private Canary canary = new Canary();
        private BlueGreen blueGreen = new BlueGreen();
        private Shadow shadow = new Shadow();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getActiveStrategy() {
            return activeStrategy;
        }

        public void setActiveStrategy(String activeStrategy) {
            this.activeStrategy = activeStrategy;
        }

        public Canary getCanary() {
            return canary;
        }

        public void setCanary(Canary canary) {
            this.canary = canary;
        }

        public BlueGreen getBlueGreen() {
            return blueGreen;
        }

        public void setBlueGreen(BlueGreen blueGreen) {
            this.blueGreen = blueGreen;
        }

        public Shadow getShadow() {
            return shadow;
        }

        public void setShadow(Shadow shadow) {
            this.shadow = shadow;
        }
    }

    public static class Canary {
        private String percentage = "50/50";
        private String algorithm = "sequential";

        public String getPercentage() {
            return percentage;
        }

        public void setPercentage(String percentage) {
            this.percentage = percentage;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
    }

    public static class BlueGreen {
        private String weight = "1/0";
        private int ttl = 300;

        public String getWeight() {
            return weight;
        }

        public void setWeight(String weight) {
            this.weight = weight;
        }

        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = (int) ttl;
        }
    }

    public static class Shadow {
        private int percentage = 20;
        private com.microswitch.domain.value.MethodType stable = com.microswitch.domain.value.MethodType.PRIMARY;
        private com.microswitch.domain.value.MethodType mirror = com.microswitch.domain.value.MethodType.SECONDARY;

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }

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

        public com.microswitch.domain.value.MethodType getStable() {
            return stable;
        }

        public void setStable(com.microswitch.domain.value.MethodType stable) {
            this.stable = stable != null ? stable : com.microswitch.domain.value.MethodType.PRIMARY;
        }

        public com.microswitch.domain.value.MethodType getMirror() {
            return mirror;
        }

        public void setMirror(com.microswitch.domain.value.MethodType mirror) {
            this.mirror = mirror != null ? mirror : com.microswitch.domain.value.MethodType.SECONDARY;
        }
    }
}

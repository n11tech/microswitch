package com.microswitch.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

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
        // Legacy simple toggle kept for backward compatibility. Prefer using nested 'comparator.mode'.
        @Deprecated
        private String comparatorMode = "disable"; // Default: disable deep comparison

        // New comparator configuration object under 'shadow.comparator.*'
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Comparator comparator = new Comparator();

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

        // --- Compatibility and new configuration accessors ---
        /**
         * Backward compatible getter for the comparator mode string.
         */
        public String getComparatorMode() {
            return (this.comparator != null ? this.comparator.getMode() : this.comparatorMode);
        }

        /**
         * Backward compatible setter. Maps legacy string to new comparator.mode as well.
         */
        public void setComparator(String mode) {
            this.comparatorMode = (mode != null ? mode : "disable");
            if (this.comparator == null) {
                this.comparator = new Comparator();
            }
            this.comparator.setMode(this.comparatorMode);
        }

        /**
         * Getter for the comparator configuration object (enables Spring binding at 'shadow.comparator.*').
         */
        public Comparator getComparator() {
            return this.comparator;
        }

        /**
         * New-style setter for the comparator configuration object (for Spring binding at 'shadow.comparator.*').
         * Also keeps legacy comparatorMode in sync for compatibility with older consumers.
         */
        public void setComparator(Comparator comparator) {
            this.comparator = (comparator != null ? comparator : new Comparator());
            this.comparatorMode = this.comparator.getMode();
        }

        @Getter
        @Setter
        public static class Comparator {
            /**
             * enable/disable deep comparison. Values: "enable" or "disable" (case-insensitive).
             */
            private String mode = "disable";
            /**
             * Upper threshold to switch from full element-wise comparison to sampling.
             */
            private int maxCollectionElements = 10_000;
            /**
             * Time budget in milliseconds for a comparison run. Exceeding it should short-circuit.
             */
            private long maxCompareTimeMillis = 200L;
            /**
             * When true and threshold exceeded, comparator switches to sampling instead of full scan.
             */
            private boolean enableSamplingOnHuge = true;
            /**
             * Sampling stride/step for large lists when sampling is enabled.
             */
            private int stride = 100;
            /**
             * Maximum number of reflected fields per class to consider during deep comparison.
             * Prevents excessive work on pathological or generated classes.
             */
            private int maxFieldsPerClass = 100; // Hard max enforced

            // Enforce absolute upper bound of 100 regardless of provided value
            public void setMaxFieldsPerClass(int maxFieldsPerClass) {
                int sanitized = Math.max(1, maxFieldsPerClass);
                this.maxFieldsPerClass = Math.min(100, sanitized);
            }
        }
    }
}

package com.microswitch.domain;

import com.microswitch.domain.value.MethodType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "microswitch")
@Getter
@Setter
public class InitializerConfiguration {

    private Boolean enabled = true;
    private String logger = "disable"; // Default: disable execution logging
    private Map<String, DeployableServices> services = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    @Getter
    @Setter
    public static class DeployableServices {
        private Boolean enabled = true;
        private String activeStrategy = "canary";
        private Canary canary = new Canary();
        private BlueGreen blueGreen = new BlueGreen();
        private Shadow shadow = new Shadow();

        public Boolean isEnabled() {
            return enabled;
        }
    }

    @Getter
    @Setter
    public static class Canary {
        private String percentage = "1/99";
        private String algorithm = "sequential";
    }

    @Getter
    @Setter
    public static class BlueGreen {
        private String weight = "1/0";
        // Use wrapper to allow null during binding when value is empty string
        private Integer ttl = 0;

        // Support numeric binding via wrapper to allow null (empty string maps to null safely)
        public void setTtl(Long ttl) {
            if (ttl == null) {
                return; // keep current/default value when source is empty string
            }
            this.ttl = ttl.intValue();
        }

        // Gracefully handle empty string values from configuration sources
        public void setTtl(String ttl) {
            if (ttl == null || ttl.isBlank()) {
                // keep current/default value
                return;
            }
            try {
                this.ttl = Integer.parseInt(ttl.trim());
            } catch (NumberFormatException ignored) {
                // leave as-is if unparsable; Spring will surface validation elsewhere if needed
            }
        }
    }

    @Getter
    @Setter
    public static class Shadow {
        private Integer percentage = 20;
        private MethodType stable = MethodType.PRIMARY;
        private MethodType mirror = MethodType.SECONDARY;
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
            this.percentage = (int) mirrorPercentage;
        }

        public void setMirrorPercentage(Short mirrorPercentage) {
            this.percentage = Integer.valueOf(mirrorPercentage != null ? mirrorPercentage : 20);
        }

        public void setStable(MethodType stable) {
            this.stable = stable != null ? stable : MethodType.PRIMARY;
        }

        public void setMirror(MethodType mirror) {
            this.mirror = mirror != null ? mirror : MethodType.SECONDARY;
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
            private Integer maxCollectionElements = 10_000;
            /**
             * Time budget in milliseconds for a comparison run. Exceeding it should short-circuit.
             */
            private Long maxCompareTimeMillis = 200L;
            /**
             * When true and threshold exceeded, comparator switches to sampling instead of full scan.
             */
            private Boolean enableSamplingOnHuge = true;
            /**
             * Sampling stride/step for large lists when sampling is enabled.
             */
            private Integer stride = 100;
            /**
             * Maximum number of reflected fields per class to consider during deep comparison.
             * Prevents excessive work on pathological or generated classes.
             */
            private Integer maxFieldsPerClass = 100; // Hard max enforced

            // Enforce absolute upper bound of 100 regardless of provided value
            public void setMaxFieldsPerClass(int maxFieldsPerClass) {
                var sanitized = Math.max(1, maxFieldsPerClass);
                this.maxFieldsPerClass = Math.min(100, sanitized);
            }

            public boolean isEnableSamplingOnHuge() {
                return enableSamplingOnHuge;
            }
        }
    }
}

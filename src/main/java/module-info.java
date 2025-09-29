module io.development.n11tech.microswitch {
    // Export ONLY the public API - DeploymentManager
    // This is the ONLY class that consuming applications can access
    exports com.microswitch.infrastructure.manager;

    // ALL OTHER PACKAGES ARE HIDDEN:
    // - com.microswitch.application.* (internal application logic)
    // - com.microswitch.domain.* (internal domain models and strategies)
    // - com.microswitch.infrastructure.external.* (internal actuator endpoints)

    // Open minimal internal packages for Spring/Actuator reflective access ONLY
    // These packages remain inaccessible to consuming applications
    opens com.microswitch.application.config to spring.beans, spring.context, spring.boot.autoconfigure;
    opens com.microswitch.application.metric to spring.beans, spring.context;
    opens com.microswitch.infrastructure.external to spring.beans, spring.context, spring.boot.actuator;
    opens com.microswitch.domain to spring.boot.autoconfigure;
    // Allow Spring Core test utilities (ReflectionUtils/ReflectionTestUtils) to access
    // private fields in strategy tests under this package
    opens com.microswitch.domain.strategy to spring.core;

    // Spring Boot and Spring Framework dependencies
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    // Spring Core for org.springframework.lang.* annotations (e.g., NonNullApi)
    requires spring.core;

    // Configuration processor for @ConfigurationProperties
    requires static spring.boot.configuration.processor;

    // Actuator annotations used by custom endpoint
    requires spring.boot.actuator;

    // Micrometer API (optional bean in auto-config)
    requires micrometer.core;

    // Logging API
    requires org.slf4j;

    // Jackson YAML (used in InitializerConfiguration)
    requires com.fasterxml.jackson.dataformat.yaml;
    // Jackson annotations used in InitializerConfiguration
    requires com.fasterxml.jackson.annotation;
    // Jackson databind (used in DeepObjectComparator)
    requires com.fasterxml.jackson.databind;

    // Lombok is used at compile-time only; mark as static
    requires static lombok;
    requires java.desktop;
}

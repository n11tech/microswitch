module com.n11.development.microswitch {
    // Export only the public API surface
    exports com.microswitch.infrastructure.manager;

    // Open minimal internal packages for Spring/Actuator reflective access
    opens com.microswitch.application.config to spring.core, spring.beans, spring.context, spring.boot.autoconfigure;
    opens com.microswitch.infrastructure.external to spring.core, spring.boot.actuator;

    // Spring Boot and Spring Framework
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;

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

    // Lombok is used at compile-time only; mark as static
    requires static lombok;
}

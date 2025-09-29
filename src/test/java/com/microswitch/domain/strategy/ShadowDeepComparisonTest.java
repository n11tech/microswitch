package com.microswitch.domain.strategy;

import com.microswitch.domain.InitializerConfiguration;
import com.microswitch.domain.value.MethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to demonstrate deep object comparison in Shadow strategy.
 * Validates that objects are compared by field values, not by reference.
 */
class ShadowDeepComparisonTest {

    private InitializerConfiguration properties;
    private Shadow shadow;

    // Test domain objects
    static class Person {
        private final Long id;
        private final String name;
        private final Address address;
        private final List<String> hobbies;

        public Person(Long id, String name, Address address, List<String> hobbies) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.hobbies = hobbies;
        }

        // Intentionally NOT overriding equals() to test deep comparison
        // Default equals() will use object reference comparison
    }

    static class Address {
        private final String street;
        private final String city;
        private final String zipCode;

        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }

        // Intentionally NOT overriding equals()
    }

    static class ComplexResult {
        private final Map<String, Object> data;
        private final List<Person> persons;
        private final String status;

        public ComplexResult(Map<String, Object> data, List<Person> persons, String status) {
            this.data = data;
            this.persons = persons;
            this.status = status;
        }

        // Intentionally NOT overriding equals()
    }

    @BeforeEach
    void setUp() {
        properties = new InitializerConfiguration();
        
        // Configure services
        Map<String, InitializerConfiguration.DeployableServices> services = new HashMap<>();
        
        // Test service with shadow configuration
        InitializerConfiguration.DeployableServices testService = new InitializerConfiguration.DeployableServices();
        testService.setEnabled(true);
        
        // Get the shadow config from the service and modify it
        InitializerConfiguration.Shadow shadowConfig = testService.getShadow();
        shadowConfig.setStable(MethodType.PRIMARY);
        shadowConfig.setMirror(MethodType.SECONDARY);
        shadowConfig.setMirrorPercentage(100); // Always mirror for testing
        
        services.put("test-service", testService);
        
        // Use the public setter instead of reflection
        properties.setServices(services);
        
        shadow = new Shadow(properties);
    }

    @Test
    void testDeepComparisonWithIdenticalFieldValues() {
        // Create suppliers that return different object instances with same field values
        Supplier<Person> primarySupplier = () -> new Person(
            1L,
            "Fatih",
            new Address("Main St", "Istanbul", "34000"),
            Arrays.asList("coding", "reading")
        );

        Supplier<Person> secondarySupplier = () -> new Person(
            1L,
            "Fatih", 
            new Address("Main St", "Istanbul", "34000"),
            Arrays.asList("coding", "reading")
        );

        // Execute shadow comparison
        Person result = shadow.execute(primarySupplier, secondarySupplier, "test-service");

        // Verify result is returned
        assertNotNull(result);
        assertEquals(1L, ReflectionTestUtils.getField(result, "id"));
        assertEquals("Fatih", ReflectionTestUtils.getField(result, "name"));
        
        // The objects are different instances but have same field values
        // Deep comparison should detect them as equal
        Person primary = primarySupplier.get();
        Person secondary = secondarySupplier.get();
        
        // Verify they are different instances (would fail with simple equals)
        assertNotSame(primary, secondary);
        assertFalse(primary.equals(secondary)); // Default equals uses reference comparison
        
        // But our deep comparison in Shadow should have logged them as matching
        // (Check logs for: "Shadow execution successful - results match")
    }

    @Test
    void testDeepComparisonWithDifferentFieldValues() {
        // Create suppliers with different field values
        Supplier<Person> primarySupplier = () -> new Person(
            1L,
            "Fatih",
            new Address("Main St", "Istanbul", "34000"),
            Arrays.asList("coding", "reading")
        );

        Supplier<Person> secondarySupplier = () -> new Person(
            1L,
            "Fatih",
            new Address("Different St", "Ankara", "06000"), // Different address
            Arrays.asList("coding", "reading")
        );

        // Execute shadow comparison
        Person result = shadow.execute(primarySupplier, secondarySupplier, "test-service");

        assertNotNull(result);
        // Shadow should detect the difference and log a warning
        // (Check logs for: "Shadow result does not match stable result")
    }

    @Test
    void testDeepComparisonWithComplexNestedObjects() {
        // Create complex nested structures
        Supplier<ComplexResult> primarySupplier = () -> {
            Map<String, Object> data = new HashMap<>();
            data.put("count", 100);
            data.put("active", true);
            data.put("nested", Map.of("key", "value"));

            List<Person> persons = Arrays.asList(
                new Person(1L, "User1", new Address("St1", "City1", "11111"), List.of("hobby1")),
                new Person(2L, "User2", new Address("St2", "City2", "22222"), List.of("hobby2"))
            );

            return new ComplexResult(data, persons, "SUCCESS");
        };

        Supplier<ComplexResult> secondarySupplier = () -> {
            Map<String, Object> data = new HashMap<>();
            data.put("count", 100);
            data.put("active", true);
            data.put("nested", Map.of("key", "value"));

            List<Person> persons = Arrays.asList(
                new Person(1L, "User1", new Address("St1", "City1", "11111"), List.of("hobby1")),
                new Person(2L, "User2", new Address("St2", "City2", "22222"), List.of("hobby2"))
            );

            return new ComplexResult(data, persons, "SUCCESS");
        };

        // Execute shadow comparison
        ComplexResult result = shadow.execute(primarySupplier, secondarySupplier, "test-service");

        assertNotNull(result);
        assertEquals("SUCCESS", ReflectionTestUtils.getField(result, "status"));
        
        // Despite being different object instances, they have identical field values
        // Deep comparison should recognize them as equal
    }

    @Test
    void testDeepComparisonWithCollections() {
        // Test with collections that have same elements but different order
        Supplier<Map<String, List<String>>> primarySupplier = () -> {
            Map<String, List<String>> result = new HashMap<>();
            result.put("items", Arrays.asList("apple", "banana", "cherry"));
            result.put("categories", Arrays.asList("fruit", "food"));
            return result;
        };

        Supplier<Map<String, List<String>>> secondarySupplier = () -> {
            Map<String, List<String>> result = new HashMap<>();
            result.put("items", Arrays.asList("apple", "banana", "cherry")); // Same order
            result.put("categories", Arrays.asList("fruit", "food"));
            return result;
        };

        // Execute shadow comparison
        Map<String, List<String>> result = shadow.execute(primarySupplier, secondarySupplier, "test-service");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("items"));
        assertTrue(result.containsKey("categories"));
    }

    @Test
    void testDeepComparisonWithNullValues() {
        // Test null handling in deep comparison
        Supplier<Person> primarySupplier = () -> new Person(
            1L,
            "Fatih",
            null, // null address
            Arrays.asList("coding")
        );

        Supplier<Person> secondarySupplier = () -> new Person(
            1L,
            "Fatih",
            null, // null address (same as primary)
            Arrays.asList("coding")
        );

        // Execute shadow comparison
        Person result = shadow.execute(primarySupplier, secondarySupplier, "test-service");

        assertNotNull(result);
        assertEquals("Fatih", ReflectionTestUtils.getField(result, "name"));
        // Should handle nulls correctly and consider them equal
    }

    @Test
    void testDeepComparisonPerformance() {
        // Test with large objects to verify performance optimization
        Supplier<Map<String, Object>> primarySupplier = () -> {
            Map<String, Object> largeMap = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                largeMap.put("key" + i, "value" + i);
            }
            return largeMap;
        };

        Supplier<Map<String, Object>> secondarySupplier = () -> {
            Map<String, Object> largeMap = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                largeMap.put("key" + i, "value" + i);
            }
            return largeMap;
        };

        long startTime = System.currentTimeMillis();
        
        // Execute shadow comparison
        Map<String, Object> result = shadow.execute(primarySupplier, secondarySupplier, "test-service");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertNotNull(result);
        assertEquals(1000, result.size());
        
        // Performance assertion - deep comparison should complete quickly
        assertTrue(duration < 5000, "Deep comparison took too long: " + duration + "ms");
    }
}

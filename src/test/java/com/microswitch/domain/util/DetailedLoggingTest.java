package com.microswitch.domain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Detailed Logging Test - Demonstrate Mismatch Logging")
class DetailedLoggingTest {

    @Test
    @DisplayName("Test detailed mismatch logging with different values")
    void testDetailedMismatchLogging() {
        // Create two similar but different responses to trigger detailed logging
        Map<String, Object> response1 = Map.of(
                "data", List.of(
                        Map.of("id", "item-001", "price", 200, "active", true),
                        Map.of("id", "item-002", "price", 150, "active", true)
                ),
                "success", true,
                "statusCode", 200
        );

        Map<String, Object> response2 = Map.of(
                "data", List.of(
                        Map.of("id", "item-001", "price", 201, "active", true), // Different price: 201 vs 200
                        Map.of("id", "item-002", "price", 150, "active", false) // Different active: false vs true
                ),
                "success", true,
                "statusCode", 200
        );

        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        System.out.println("=== DETAILED MISMATCH LOGGING TEST ===");
        System.out.println("This test demonstrates the enhanced logging that shows exactly which fields differ");
        System.out.println("Expected log output should show:");
        System.out.println("- Comparison mismatch at field 'map[data]->list[0]->map[price]': values (200, 201)");
        System.out.println("- Comparison mismatch at field 'map[data]->list[1]->map[active]': values (true, false)");
        System.out.println();

        boolean areEqual = comparator.areEqual(response1, response2);
        
        System.out.println("Comparison result: " + areEqual);
        System.out.println("✅ Check the logs above to see detailed field-level mismatch information!");

        assertFalse(areEqual, "These responses should be different due to price and active field differences");
    }

    @Test
    @DisplayName("Test map size mismatch logging")
    void testMapSizeMismatchLogging() {
        Map<String, Object> response1 = Map.of(
                "field1", "value1",
                "field2", "value2"
        );

        Map<String, Object> response2 = Map.of(
                "field1", "value1",
                "field2", "value2",
                "field3", "value3" // Extra field
        );

        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        System.out.println("=== MAP SIZE MISMATCH LOGGING TEST ===");
        System.out.println("Expected log: map size difference - map1.size()=2, map2.size()=3");

        boolean areEqual = comparator.areEqual(response1, response2);
        
        assertFalse(areEqual, "Maps with different sizes should not be equal");
    }

    @Test
    @DisplayName("Test missing key logging")
    void testMissingKeyLogging() {
        Map<String, Object> response1 = Map.of(
                "field1", "value1",
                "field2", "value2"
        );

        Map<String, Object> response2 = Map.of(
                "field1", "value1",
                "differentField", "value2" // Different key name
        );

        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        System.out.println("=== MISSING KEY LOGGING TEST ===");
        System.out.println("Expected log: missing key 'field2' in second map");

        boolean areEqual = comparator.areEqual(response1, response2);
        
        assertFalse(areEqual, "Maps with different keys should not be equal");
    }
}

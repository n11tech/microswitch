package com.microswitch.domain.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Real World Shadow Test - Simulate Actual Scenario")
class RealWorldShadowTest {

    @Test
    @DisplayName("Simulate actual Shadow execution with JSON responses")
    void simulateActualShadowExecution() throws Exception {
        // Given: Generic mock JSON response for testing
        String responseJson = """
            {
              "data": [
                {
                  "id": "item-001",
                  "categoryId": "cat-100",
                  "entityId": 1001,
                  "name": "Test Item Alpha",
                  "price": 29.99,
                  "active": true
                },
                {
                  "id": "item-002",
                  "categoryId": "cat-100", 
                  "entityId": 1002,
                  "name": "Test Item Beta",
                  "price": 49.50,
                  "active": true
                },
                {
                  "id": "item-003",
                  "categoryId": "cat-200",
                  "entityId": 1003,
                  "name": "Test Item Gamma",
                  "price": 15.75,
                  "active": false
                }
              ],
              "metadata": {
                "totalCount": 3,
                "page": 1,
                "pageSize": 10
              },
              "success": true,
              "message": "Operation completed successfully",
              "statusCode": 200,
              "timestamp": "2024-01-01T12:00:00Z"
            }""";

        ObjectMapper objectMapper = new ObjectMapper();
        
        // Simulate res1 supplier
        Supplier<Map<String, Object>> supplier1 = () -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(responseJson, Map.class);
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // Simulate res2 supplier
        Supplier<Map<String, Object>> supplier2 = () -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(responseJson, Map.class);
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // Create comparator with same config as Shadow
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .withMaxDepth(10)
                .compareNullsAsEqual(false)
                .ignoreFields("timestamp", "requestId", "traceId")
                .build();

        // Simulate async execution like in Shadow.executeAsyncSimultaneously
        CompletableFuture<Map<String, Object>> futureStable = CompletableFuture.supplyAsync(supplier1);
        CompletableFuture<Map<String, Object>> futureMirror = CompletableFuture.supplyAsync(supplier2);

        // Wait for both to complete
        CompletableFuture.allOf(futureStable, futureMirror).join();

        Map<String, Object> stableResult = futureStable.join();
        Map<String, Object> mirrorResult = futureMirror.join();

        System.out.println("=== SHADOW EXECUTION SIMULATION ===");
        System.out.println("Stable result: " + stableResult);
        System.out.println("Mirror result: " + mirrorResult);
        System.out.println("Same reference: " + (stableResult == mirrorResult));
        System.out.println("Objects.equals: " + java.util.Objects.equals(stableResult, mirrorResult));

        // This is the exact comparison from Shadow.java line 170
        boolean resultsMatch = comparator.areEqual(stableResult, mirrorResult);
        System.out.println("Deep comparator result: " + resultsMatch);

        if (!resultsMatch) {
            System.out.println("❌ ISSUE REPRODUCED: Deep comparator detected differences!");
            
            // Debug the differences
            System.out.println("\n=== DEBUGGING DIFFERENCES ===");
            System.out.println("Stable result class: " + stableResult.getClass().getName());
            System.out.println("Mirror result class: " + mirrorResult.getClass().getName());
            System.out.println("Stable result keys: " + stableResult.keySet());
            System.out.println("Mirror result keys: " + mirrorResult.keySet());
            
            // Check each key
            for (String key : stableResult.keySet()) {
                Object stableValue = stableResult.get(key);
                Object mirrorValue = mirrorResult.get(key);
                boolean keyMatch = java.util.Objects.equals(stableValue, mirrorValue);
                System.out.println("Key '" + key + "' match: " + keyMatch);
                if (!keyMatch) {
                    System.out.println("  Stable: " + stableValue + " (" + (stableValue != null ? stableValue.getClass().getSimpleName() : "null") + ")");
                    System.out.println("  Mirror: " + mirrorValue + " (" + (mirrorValue != null ? mirrorValue.getClass().getSimpleName() : "null") + ")");
                }
            }
        } else {
            System.out.println("✅ No issue detected - comparison works correctly");
        }

        assertTrue(resultsMatch, "Identical JSON responses should match in Shadow comparison");
    }

    @Test
    @DisplayName("Test with different object types but same data")
    void testWithDifferentObjectTypesButSameData() throws Exception {
        String responseJson = """
            {
              "data": [
                {
                  "id": "item-001",
                  "categoryId": "cat-100",
                  "entityId": 1001,
                  "name": "Sample Item",
                  "price": 25.00,
                  "active": true
                }
              ],
              "metadata": {
                "totalCount": 1,
                "page": 1
              },
              "success": true,
              "message": "Request processed successfully",
              "statusCode": 200
            }""";

        ObjectMapper objectMapper = new ObjectMapper();
        
        // Parse as Map (like most REST clients do)
        @SuppressWarnings("unchecked")
        Map<String, Object> mapResult = objectMapper.readValue(responseJson, Map.class);
        
        // Parse as Map again (simulating different parsing)
        @SuppressWarnings("unchecked")
        Map<String, Object> mapResult2 = objectMapper.readValue(responseJson, Map.class);

        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        System.out.println("=== DIFFERENT PARSING SIMULATION ===");
        System.out.println("Map1: " + mapResult);
        System.out.println("Map2: " + mapResult2);
        System.out.println("Same reference: " + (mapResult == mapResult2));
        System.out.println("Objects.equals: " + java.util.Objects.equals(mapResult, mapResult2));

        boolean areEqual = comparator.areEqual(mapResult, mapResult2);
        System.out.println("Deep comparator result: " + areEqual);

        assertTrue(areEqual, "Same JSON parsed twice should be equal");
    }

    @Test
    @DisplayName("Test potential numeric type issues")
    void testPotentialNumericTypeIssues() {
        // Simulate potential issue where one system returns Integer, another Long
        Map<String, Object> response1 = Map.of(
                "entityId", 12345,  // Integer
                "count", 100,       // Integer
                "success", true,
                "statusCode", 200
        );

        Map<String, Object> response2 = Map.of(
                "entityId", 12345L, // Long
                "count", 100L,      // Long
                "success", true,
                "statusCode", 200
        );

        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        System.out.println("=== NUMERIC TYPE ISSUE SIMULATION ===");
        System.out.println("Response1 entityId type: " + response1.get("entityId").getClass().getSimpleName());
        System.out.println("Response2 entityId type: " + response2.get("entityId").getClass().getSimpleName());
        System.out.println("Response1 count type: " + response1.get("count").getClass().getSimpleName());
        System.out.println("Response2 count type: " + response2.get("count").getClass().getSimpleName());

        boolean areEqual = comparator.areEqual(response1, response2);
        System.out.println("Are equal (Integer vs Long): " + areEqual);

        // This should now pass due to our numeric equivalence fix!
        if (!areEqual) {
            System.out.println("❌ POTENTIAL ROOT CAUSE: Integer vs Long type mismatch!");
            System.out.println("Different systems might return different numeric types");
        } else {
            System.out.println("✅ SUCCESS: Numeric equivalence working correctly!");
        }

        assertTrue(areEqual, "Integer and Long values should be considered equal");
    }
}

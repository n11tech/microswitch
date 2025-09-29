package com.microswitch.domain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Numeric Type Equivalence Test - Verify Fix")
class NumericTypeEquivalenceTest {

    @Test
    @DisplayName("Should handle Integer vs Long equivalence")
    void shouldHandleIntegerVsLongEquivalence() {
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        // Test the exact scenario from your issue
        Map<String, Object> response1 = Map.of(
                "productId", 260230994,  // Integer
                "success", true,
                "statusCode", 200
        );

        Map<String, Object> response2 = Map.of(
                "productId", 260230994L, // Long
                "success", true,
                "statusCode", 200
        );

        System.out.println("=== NUMERIC TYPE EQUIVALENCE TEST ===");
        System.out.println("response1 productId type: " + response1.get("productId").getClass().getSimpleName());
        System.out.println("response2 productId type: " + response2.get("productId").getClass().getSimpleName());

        boolean areEqual = comparator.areEqual(response1, response2);
        System.out.println("Are equal (after fix): " + areEqual);

        assertTrue(areEqual, "Integer vs Long with same value should be considered equal");
    }

    @Test
    @DisplayName("Should handle various numeric type combinations")
    void shouldHandleVariousNumericTypeCombinations() {
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        // Test various numeric type combinations
        Object[][] testCases = {
                {123, 123L},           // Integer vs Long
                {123, 123.0},          // Integer vs Double
                {123L, 123.0},         // Long vs Double
                {123.0f, 123.0},       // Float vs Double
                {(short) 123, 123},    // Short vs Integer
                {(byte) 123, 123}      // Byte vs Integer
        };

        for (Object[] testCase : testCases) {
            Object value1 = testCase[0];
            Object value2 = testCase[1];

            boolean areEqual = comparator.areEqual(value1, value2);
            System.out.println(value1.getClass().getSimpleName() + "(" + value1 + ") vs " +
                    value2.getClass().getSimpleName() + "(" + value2 + ") = " + areEqual);

            assertTrue(areEqual, value1.getClass().getSimpleName() + " vs " +
                    value2.getClass().getSimpleName() + " should be equal");
        }
    }

    @Test
    @DisplayName("Should still detect actual numeric differences")
    void shouldStillDetectActualNumericDifferences() {
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        // These should NOT be equal
        assertFalse(comparator.areEqual(123, 124), "Different values should not be equal");
        assertFalse(comparator.areEqual(123L, 124L), "Different Long values should not be equal");
        assertFalse(comparator.areEqual(123.0, 123.1), "Different Double values should not be equal");
    }

    @Test
    @DisplayName("Should handle your exact JSON scenario")
    void shouldHandleYourExactJsonScenario() {
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .withMaxDepth(10)
                .compareNullsAsEqual(false)
                .ignoreFields("timestamp", "requestId", "traceId")
                .build();

        // Simulate response (Integer productIds)
        Map<String, Object> data1 = Map.of(
                "data", java.util.List.of(
                        Map.of("id", "26638251", "contentId", "5818237950", "productId", 260252994),
                        Map.of("id", "26638250", "contentId", "5818237950", "productId", 260252995),
                        Map.of("id", "26521850", "contentId", "5818237950", "productId", 43826956)
                ),
                "success", true,
                "message", "Success",
                "statusCode", 200
        );

        // Simulate response (Long productIds)
        Map<String, Object> data2 = Map.of(
                "data", java.util.List.of(
                        Map.of("id", "26638251", "contentId", "5818237950", "productId", 260252994L),
                        Map.of("id", "26638250", "contentId", "5818237950", "productId", 260252995L),
                        Map.of("id", "26521850", "contentId", "5818237950", "productId", 43826956L)
                ),
                "success", true,
                "message", "Success",
                "statusCode", 200
        );

        System.out.println("=== YOUR EXACT SCENARIO TEST ===");
        System.out.println("Testing (Integer) vs (Long) responses...");

        boolean areEqual = comparator.areEqual(data1, data2);
        System.out.println("Results match: " + areEqual);

        if (areEqual) {
            System.out.println("✅ SUCCESS: The fix resolves your Shadow comparison issue!");
        } else {
            System.out.println("❌ ISSUE: Still detecting differences - may need further investigation");
        }

        assertTrue(areEqual, "data1 and data2 responses should be considered equal despite numeric type differences");
    }
}

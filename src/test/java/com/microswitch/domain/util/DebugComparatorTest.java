package com.microswitch.domain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Debug Comparator Test - Find Exact Issue")
class DebugComparatorTest {

    @Test
    @DisplayName("Debug simple Integer vs Long comparison")
    void debugSimpleIntegerVsLongComparison() {
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.REFLECTION_BASED)
                .build();

        Integer int1 = 123;
        Long long1 = 123L;

        System.out.println("=== SIMPLE INTEGER VS LONG DEBUG ===");
        System.out.println("Integer: " + int1 + " (class: " + int1.getClass().getName() + ")");
        System.out.println("Long: " + long1 + " (class: " + long1.getClass().getName() + ")");
        System.out.println("Objects.equals: " + false);
        System.out.println("int1.equals(long1): " + false);
        System.out.println("long1.equals(int1): " + false);

        boolean result = comparator.areEqual(int1, long1);
        System.out.println("DeepObjectComparator result: " + result);

        // This should work with REFLECTION_BASED strategy
        assertTrue(result, "Integer vs Long should be equal with reflection-based strategy");
    }

    @Test
    @DisplayName("Debug with all strategies")
    void debugWithAllStrategies() {
        Integer int1 = 123;
        Long long1 = 123L;

        DeepObjectComparator.ComparisonStrategy[] strategies = DeepObjectComparator.ComparisonStrategy.values();
        
        System.out.println("=== ALL STRATEGIES DEBUG ===");
        for (DeepObjectComparator.ComparisonStrategy strategy : strategies) {
            DeepObjectComparator comparator = DeepObjectComparator.builder()
                    .withStrategy(strategy)
                    .build();
            
            boolean result = comparator.areEqual(int1, long1);
            System.out.println(strategy + ": " + result);
        }
    }

    @Test
    @DisplayName("Test the exact areEqual method entry point")
    void testExactAreEqualMethodEntryPoint() {
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        Integer int1 = 123;
        Long long1 = 123L;

        System.out.println("=== ENTRY POINT DEBUG ===");
        System.out.println("int1 == long1: Cannot compare directly (different types)");
        System.out.println("int1 == null: " + false);
        System.out.println("long1 == null: " + false);
        System.out.println("int1.getClass().equals(long1.getClass()): " + false);

        // The areEqual method should fail at the class comparison check!
        // Let's see what happens
        boolean result = comparator.areEqual(int1, long1);
        System.out.println("Final result: " + result);

        if (!result) {
            System.out.println("❌ The comparison fails because int1.getClass() != long1.getClass()");
            System.out.println("This happens BEFORE any strategy-specific logic is called!");
        }
    }
}

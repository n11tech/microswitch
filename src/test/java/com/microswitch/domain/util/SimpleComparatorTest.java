package com.microswitch.domain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Simple Comparator Test - Debug Core Issue")
class SimpleComparatorTest {

    @Test
    @DisplayName("Debug: Test with simple objects")
    void debugWithSimpleObjects() {
        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .build();

        // Test with simple strings
        String str1 = "test";
        String str2 = "test";
        boolean stringResult = comparator.areEqual(str1, str2);
        System.out.println("String comparison result: " + stringResult);
        assertTrue(stringResult, "Identical strings should be equal");

        // Test with integers
        Integer int1 = 123;
        Integer int2 = 123;
        boolean intResult = comparator.areEqual(int1, int2);
        System.out.println("Integer comparison result: " + intResult);
        assertTrue(intResult, "Identical integers should be equal");

        // Test with Maps
        Map<String, Object> map1 = Map.of("key", "value", "number", 123);
        Map<String, Object> map2 = Map.of("key", "value", "number", 123);
        boolean mapResult = comparator.areEqual(map1, map2);
        System.out.println("Map comparison result: " + mapResult);
        assertTrue(mapResult, "Identical maps should be equal");

        // Test with Lists
        List<String> list1 = List.of("a", "b", "c");
        List<String> list2 = List.of("a", "b", "c");
        boolean listResult = comparator.areEqual(list1, list2);
        System.out.println("List comparison result: " + listResult);
        assertTrue(listResult, "Identical lists should be equal");
    }

    @Test
    @DisplayName("Debug: Test all strategies with simple data")
    void debugAllStrategiesWithSimpleData() {
        Map<String, Object> data1 = Map.of(
                "id", "123",
                "name", "test",
                "value", 456
        );
        Map<String, Object> data2 = Map.of(
                "id", "123", 
                "name", "test",
                "value", 456
        );

        DeepObjectComparator.ComparisonStrategy[] strategies = DeepObjectComparator.ComparisonStrategy.values();
        
        for (DeepObjectComparator.ComparisonStrategy strategy : strategies) {
            DeepObjectComparator comparator = DeepObjectComparator.builder()
                    .withStrategy(strategy)
                    .build();
            
            boolean result = comparator.areEqual(data1, data2);
            System.out.println(strategy + ": " + result);
            
            if (!result) {
                System.out.println("❌ ISSUE FOUND: " + strategy + " failed to detect identical maps as equal");
            }
        }
    }
}

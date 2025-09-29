package com.microswitch.domain.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Efficient deep object comparator for shadow traffic validation.
 * Provides multiple comparison strategies optimized for different scenarios.
 */
@Slf4j
public class DeepObjectComparator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Short HEAD_TAIL_THRESHOLD = 1000;
    private static final Short STRIDE_THRESHOLD = 1;
    private static final Short COLLECTION_OPTIMIZED_THRESHOLD = 500;

    private final ComparisonStrategy strategy;
    private final Set<String> fieldsToIgnore;
    private final int maxDepth;
    private final int maxCollectionElements;
    private final long maxCompareTimeMillis;
    private final boolean enableSamplingOnHuge;
    private final int stride;
    private final int maxFieldsPerClass;

    public enum ComparisonStrategy {
        /**
         * Uses Jackson ObjectMapper for JSON-based comparison.
         * Good for complex objects with proper Jackson annotations.
         * Handles circular references and complex types well.
         */
        JSON_BASED,

        /**
         * Uses reflection to compare fields directly.
         * Faster for simple objects, but requires careful handling of circular references.
         */
        REFLECTION_BASED,

        /**
         * Hybrid approach: tries equals() first, falls back to JSON.
         * Best for mixed object types where some have proper equals() implementation.
         */
        HYBRID,

        /**
         * Optimized for collections and arrays.
         * Uses element-wise comparison with early exit.
         */
        COLLECTION_OPTIMIZED
    }

    public static class Builder {
        private ComparisonStrategy strategy = ComparisonStrategy.HYBRID;
        private final Set<String> fieldsToIgnore = new HashSet<>();
        private int maxDepth = 10;
        private boolean compareNullsAsEqual = true;
        private int maxCollectionElements = 1000;
        private long maxCompareTimeMillis = 200L;
        private boolean enableSamplingOnHuge = true;
        private int stride = 10;
        private int maxFieldsPerClass = 100;

        public Builder withStrategy(ComparisonStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder ignoreFields(String... fields) {
            this.fieldsToIgnore.addAll(Arrays.asList(fields));
            return this;
        }

        public Builder withMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder compareNullsAsEqual(boolean compareNullsAsEqual) {
            this.compareNullsAsEqual = compareNullsAsEqual;
            return this;
        }

        public Builder withMaxCollectionElements(int maxCollectionElements) {
            this.maxCollectionElements = Math.max(0, maxCollectionElements);
            return this;
        }

        public Builder withMaxCompareTimeMillis(long maxCompareTimeMillis) {
            this.maxCompareTimeMillis = Math.max(0L, maxCompareTimeMillis);
            return this;
        }

        public Builder enableSamplingOnHuge(boolean enableSamplingOnHuge) {
            this.enableSamplingOnHuge = enableSamplingOnHuge;
            return this;
        }

        public Builder withStride(int stride) {
            this.stride = Math.max(1, stride);
            return this;
        }

        public Builder withMaxFieldsPerClass(int maxFieldsPerClass) {
            int sanitized = Math.max(1, maxFieldsPerClass);
            this.maxFieldsPerClass = Math.min(100, sanitized);
            return this;
        }

        public DeepObjectComparator build() {
            return new DeepObjectComparator(strategy, fieldsToIgnore, maxDepth,
                    maxCollectionElements, maxCompareTimeMillis, enableSamplingOnHuge, stride, maxFieldsPerClass);
        }
    }

    private DeepObjectComparator(ComparisonStrategy strategy, Set<String> fieldsToIgnore,
                                 int maxDepth, int maxCollectionElements, long maxCompareTimeMillis,
                                 boolean enableSamplingOnHuge, int stride, int maxFieldsPerClass) {
        this.strategy = strategy;
        this.fieldsToIgnore = new HashSet<>(fieldsToIgnore);
        this.maxDepth = maxDepth;
        this.maxCollectionElements = maxCollectionElements;
        this.maxCompareTimeMillis = maxCompareTimeMillis;
        this.enableSamplingOnHuge = enableSamplingOnHuge;
        this.stride = stride;
        this.maxFieldsPerClass = maxFieldsPerClass;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default comparator with hybrid strategy
     */
    public static DeepObjectComparator defaultComparator() {
        return new Builder().build();
    }

    /**
     * Fast comparator optimized for performance
     */
    public static DeepObjectComparator fastComparator() {
        return new Builder()
                .withStrategy(ComparisonStrategy.REFLECTION_BASED)
                .withMaxDepth(5)
                .build();
    }

    /**
     * Thorough comparator for complete validation
     */
    public static DeepObjectComparator thoroughComparator() {
        return new Builder()
                .withStrategy(ComparisonStrategy.JSON_BASED)
                .withMaxDepth(Integer.MAX_VALUE)
                .build();
    }

    /**
     * Main comparison method
     */
    public <T> boolean areEqual(T obj1, T obj2) {
        if (obj1 == obj2) {
            return true;
        }

        if (obj1 == null || obj2 == null) {
            return false;
        }

        // Handle numeric type equivalence before class check
        if (areNumericTypesEquivalent(obj1, obj2)) {
            return true;
        }

        if (!obj1.getClass().equals(obj2.getClass())) {
            return false;
        }

        long startedAtNanos = System.nanoTime();
        try {
            return switch (strategy) {
                case JSON_BASED -> compareUsingJson(obj1, obj2);
                case REFLECTION_BASED -> compareUsingReflection(obj1, obj2, 0, new HashSet<>(), startedAtNanos);
                case COLLECTION_OPTIMIZED -> compareOptimized(obj1, obj2, startedAtNanos);
                default -> compareHybrid(obj1, obj2);
            };
        } catch (Exception e) {
            log.warn("[MICROSWITCH-COMPARATOR] - Deep comparison failed, falling back to equals(): {}", e.getMessage());
            return Objects.equals(obj1, obj2);
        }
    }

    /**
     * JSON-based comparison using Jackson
     */
    private <T> boolean compareUsingJson(T obj1, T obj2) {
        try {
            String json1 = MAPPER.writeValueAsString(filterFields(obj1));
            String json2 = MAPPER.writeValueAsString(filterFields(obj2));
            return json1.equals(json2);
        } catch (Exception e) {
            log.debug("[MICROSWITCH-COMPARATOR] - JSON comparison failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Filter fields to ignore during comparison
     */
    private Object filterFields(Object obj) {
        if (fieldsToIgnore.isEmpty()) {
            return obj;
        }

        try {
            ObjectNode node = MAPPER.valueToTree(obj);
            fieldsToIgnore.forEach(node::remove);
            return node;
        } catch (Exception e) {
            return obj;
        }
    }

    /**
     * Reflection-based comparison
     */
    private boolean compareUsingReflection(Object obj1, Object obj2, int depth, Set<Integer> visited, long startedAtNanos) {
        if (depth > maxDepth) {
            return true; // Assume equal at max depth to prevent stack overflow
        }
        if (timeBudgetExceededAndLog("reflection", startedAtNanos)) {
            return false;
        }

        // Circular reference detection using identity hash codes
        int hash1 = System.identityHashCode(obj1);
        int hash2 = System.identityHashCode(obj2);
        int combinedHash = hash1 * 31 + hash2;

        if (visited.contains(combinedHash)) {
            return true; // Already comparing these objects
        }
        visited.add(combinedHash);

        Class<?> clazz = obj1.getClass();

        // Handle primitives and common types
        if (isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            return Objects.equals(obj1, obj2);
        }

        // Handle collections
        if (obj1 instanceof Collection) {
            return compareCollections((Collection<?>) obj1, (Collection<?>) obj2, depth, visited, startedAtNanos);
        }

        // Handle maps
        if (obj1 instanceof Map) {
            return compareMaps((Map<?, ?>) obj1, (Map<?, ?>) obj2, depth, visited, startedAtNanos);
        }

        // Handle arrays
        if (clazz.isArray()) {
            return compareArrays(obj1, obj2, depth, visited, startedAtNanos);
        }

        // Compare fields
        Field[] fields = getFields(clazz);
        for (Field field : fields) {
            if (shouldSkipField(field)) {
                continue;
            }

            try {
                Object value1 = field.get(obj1);
                Object value2 = field.get(obj2);

                if (areFieldValuesDifferent(value1, value2, depth + 1, visited, startedAtNanos, field.getName())) {
                    return false;
                }

                if (timeBudgetExceededAndLog("fields", startedAtNanos)) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                log.warn("[MICROSWITCH-COMPARATOR] - Skipping inaccessible field {}: {}", field.getName(), e.getMessage());
                // Skip inaccessible fields instead of failing the comparison - no continue needed as it's end of iteration
            }
        }

        return true;
    }

    /**
     * Check if field values are different (returns true if different, false if equal)
     */
    private boolean areFieldValuesDifferent(Object value1, Object value2, int depth, Set<Integer> visited, long startedAtNanos) {
        return areFieldValuesDifferent(value1, value2, depth, visited, startedAtNanos, null);
    }

    /**
     * Check if field values are different with field context for logging
     */
    private boolean areFieldValuesDifferent(Object value1, Object value2, int depth, Set<Integer> visited, long startedAtNanos, String fieldContext) {
        if (value1 == value2) {
            return false; // Same reference, not different
        }

        if (value1 == null || value2 == null) {
            if (log.isWarnEnabled() && fieldContext != null) {
                log.warn("[MICROSWITCH-COMPARATOR] - Comparison mismatch at field '{}': null vs non-null - values: ({}, {})", 
                        fieldContext, value1, value2);
            }
            return true; // One is null, other isn't - they are different
        }

        Class<?> type1 = value1.getClass();
        Class<?> type2 = value2.getClass();

        // Debug logging for numeric type comparison
        if (log.isDebugEnabled() && (isNumericType(type1) || isNumericType(type2))) {
            log.debug("[MICROSWITCH-COMPARATOR] - Comparing values: {} ({}) vs {} ({})", 
                    value1, type1.getSimpleName(), value2, type2.getSimpleName());
        }

        if ((isPrimitiveOrWrapper(type1) || type1 == String.class || type1.isEnum()) &&
            (isPrimitiveOrWrapper(type2) || type2 == String.class || type2.isEnum())) {
            
            // Handle numeric type equivalence (Integer vs Long, etc.)
            if (areNumericTypesEquivalent(value1, value2)) {
                return false; // Numerically equivalent, not different
            }
            
            boolean areEqual = Objects.equals(value1, value2);
            if (!areEqual && log.isWarnEnabled() && fieldContext != null) {
                log.warn("[MICROSWITCH-COMPARATOR] - Comparison mismatch at field '{}': values ({}, {}) - types: ({}, {})", 
                        fieldContext, value1, value2, type1.getSimpleName(), type2.getSimpleName());
            }
            return !areEqual; // Invert: true if NOT equal
        }

        boolean areEqual = compareUsingReflection(value1, value2, depth, visited, startedAtNanos);
        if (!areEqual && log.isWarnEnabled() && fieldContext != null) {
            log.warn("[MICROSWITCH-COMPARATOR] - Comparison mismatch at field '{}': complex objects differ - types: ({}, {})", 
                    fieldContext, type1.getSimpleName(), type2.getSimpleName());
        }
        return !areEqual; // Invert: true if NOT equal
    }

    /**
     * Hybrid comparison approach
     */
    private <T> boolean compareHybrid(T obj1, T obj2) {
        // Handle Collections and Maps specially to ensure numeric equivalence works
        long startedAtNanos = System.nanoTime();
        if (obj1 instanceof Collection) {
            return compareCollections((Collection<?>) obj1, (Collection<?>) obj2, 0, new HashSet<>(), startedAtNanos);
        } else if (obj1 instanceof Map) {
            return compareMaps((Map<?, ?>) obj1, (Map<?, ?>) obj2, 0, new HashSet<>(), startedAtNanos);
        }
        
        // For other objects, try equals() if it's overridden
        Class<?> clazz = obj1.getClass();
        try {
            if (clazz.getMethod("equals", Object.class).getDeclaringClass() != Object.class) {
                // equals() is overridden, use it
                return obj1.equals(obj2);
            }
        } catch (NoSuchMethodException e) {
            // Should not happen
        }

        // Fall back to reflection-based comparison (which handles numeric type equivalence)
        return compareUsingReflection(obj1, obj2, 0, new HashSet<>(), startedAtNanos);
    }

    /**
     * Optimized comparison for collections and simple objects
     */
    private <T> boolean compareOptimized(T obj1, T obj2, long startedAtNanos) {
        if (obj1 instanceof Collection) {
            return compareCollections((Collection<?>) obj1, (Collection<?>) obj2, 0, new HashSet<>(), startedAtNanos);
        } else if (obj1 instanceof Map) {
            return compareMaps((Map<?, ?>) obj1, (Map<?, ?>) obj2, 0, new HashSet<>(), startedAtNanos);
        } else {
            return compareUsingReflection(obj1, obj2, 0, new HashSet<>(), startedAtNanos);
        }
    }

    /**
     * Compare collections efficiently
     */
    @SuppressWarnings("unchecked")
    private boolean compareCollections(Collection<?> col1, Collection<?> col2, int depth, Set<Integer> visited, long startedAtNanos) {
        if (timeBudgetExceededAndLog("collections", startedAtNanos)) {
            return false;
        }
        if (col1.size() != col2.size()) {
            return false;
        }

        // For lists, compare in order
        if (col1 instanceof List<?> l1 && col2 instanceof List<?> l2) {
            int size = l1.size();
            if (enableSamplingOnHuge) {
                if (maxCollectionElements > 0 && size >= maxCollectionElements) {
                    int headTail = Math.min(HEAD_TAIL_THRESHOLD, maxCollectionElements);
                    if (log.isWarnEnabled()) {
                        log.info("[MICROSWITCH-COMPARATOR] - Deep comparison sampling activated with maxCollectionElements={} for large list (size={}) - stride={}, headTail={}",
                                maxCollectionElements, size, this.stride, headTail);
                        if (headTail < maxCollectionElements)
                            log.info("[MICROSWITCH-COMPARATOR] - Headtail is limited to the default size of 1000.");
                    }
                    for (int i = 0; i < headTail; i++) {
                        if (areFieldValuesDifferent(l1.get(i), l2.get(i), depth + 1, visited, startedAtNanos, "list[" + i + "]")) {
                            return false;
                        }
                        if (areFieldValuesDifferent(l1.get(size - 1 - i), l2.get(size - 1 - i), depth + 1, visited, startedAtNanos, "list[" + (size - 1 - i) + "]")) {
                            return false;
                        }
                        if (timeBudgetExceededAndLog("collections[list-sampling]", startedAtNanos)) {
                            return false;
                        }
                    }
                }
                if (stride > STRIDE_THRESHOLD) {
                    for (int i = 0; i < size; i += stride) {
                        if (areFieldValuesDifferent(l1.get(i), l2.get(i), depth + 1, visited, startedAtNanos, "list[" + i + "]")) {
                            return false;
                        }
                        if (timeBudgetExceededAndLog("collections[list-sampling]", startedAtNanos)) {
                            return false;
                        }
                    }
                    if (log.isWarnEnabled()) {
                        log.info("[MICROSWITCH-COMPARATOR] - Deep comparison sampling activated with stride for large list (size={}) - maxCollectionElements={}, stride={}",
                                size, maxCollectionElements, this.stride);
                    }
                }
            } else {
                if (size <= COLLECTION_OPTIMIZED_THRESHOLD) {
                    for (int i = 0; i < size; i++) {
                        if (areFieldValuesDifferent(l1.get(i), l2.get(i), depth + 1, visited, startedAtNanos, "list[" + i + "]")) {
                            return false;
                        }
                        if (timeBudgetExceededAndLog("collections[list]", startedAtNanos)) {
                            return false;
                        }
                    }
                } else
                    log.warn("[MICROSWITCH-COMPARATOR] - Large list comparison skipped for size {}. Consider enable sampling on huge list with config(enableSamplingOnHuge)", size);
            }
            return true;
        }

        // For sets, check containment
        if (col1 instanceof Set && col2 instanceof Set) {
            // Convert to lists and sort if possible (for comparable elements)
            try {
                List<?> list1 = new ArrayList<>(col1);
                List<?> list2 = new ArrayList<>(col2);

                // Check if elements are comparable before sorting
                if (!list1.isEmpty() && list1.getFirst() instanceof Comparable) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    List<Comparable> sortableList1 = (List<Comparable>) list1;
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    List<Comparable> sortableList2 = (List<Comparable>) list2;

                    // Sort both lists - suppress unchecked warnings for Collections.sort calls
                    Collections.sort(sortableList1);
                    Collections.sort(sortableList2);

                    return compareCollections(sortableList1, sortableList2, depth, visited, startedAtNanos);
                } else {
                    // Elements not comparable, use default equals
                    log.warn("[MICROSWITCH-COMPARATOR] - Elements not comparable, using default equals for collections");
                    return col1.equals(col2);
                }
            } catch (Exception e) {
                // Can't sort, use containment check
                log.error("[MICROSWITCH-COMPARATOR] - Exception while comparing collections: {}, using default equals for collections", e.getMessage());
                return col1.equals(col2);
            }
        }

        return col1.equals(col2);
    }

    /**
     * Compare maps efficiently
     */
    private boolean compareMaps(Map<?, ?> map1, Map<?, ?> map2, int depth, Set<Integer> visited, long startedAtNanos) {
        if (timeBudgetExceededAndLog("maps", startedAtNanos)) {
            return false;
        }
        if (map1.size() != map2.size()) {
            if (log.isWarnEnabled()) {
                log.warn("[MICROSWITCH-COMPARATOR] - Comparison mismatch: map size difference - map1.size()={}, map2.size()={}", 
                        map1.size(), map2.size());
            }
            return false;
        }

        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            Object key = entry.getKey();
            if (!map2.containsKey(key)) {
                if (log.isWarnEnabled()) {
                    log.warn("[MICROSWITCH-COMPARATOR] - Comparison mismatch: missing key '{}' in second map", key);
                }
                return false;
            }

            if (areFieldValuesDifferent(entry.getValue(), map2.get(key), depth + 1, visited, startedAtNanos, "map[" + key + "]")) {
                return false;
            }
            if (timeBudgetExceededAndLog("maps", startedAtNanos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare arrays
     */
    private boolean compareArrays(Object arr1, Object arr2, int depth, Set<Integer> visited, long startedAtNanos) {
        if (timeBudgetExceededAndLog("arrays", startedAtNanos)) {
            return false;
        }
        if (arr1.getClass().getComponentType().isPrimitive()) {
            return comparePrimitiveArrays(arr1, arr2);
        }

        Object[] array1 = (Object[]) arr1;
        Object[] array2 = (Object[]) arr2;

        if (array1.length != array2.length) {
            if (log.isWarnEnabled()) {
                log.warn("[MICROSWITCH-COMPARATOR] - Comparison mismatch: array length difference - array1.length={}, array2.length={}", 
                        array1.length, array2.length);
            }
            return false;
        }

        for (int i = 0; i < array1.length; i++) {
            if (areFieldValuesDifferent(array1[i], array2[i], depth + 1, visited, startedAtNanos, "array[" + i + "]")) {
                return false;
            }
            if (timeBudgetExceededAndLog("arrays", startedAtNanos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare primitive arrays
     */
    private boolean comparePrimitiveArrays(Object arr1, Object arr2) {
        Class<?> type = arr1.getClass().getComponentType();

        if (type == int.class) {
            return Arrays.equals((int[]) arr1, (int[]) arr2);
        } else if (type == long.class) {
            return Arrays.equals((long[]) arr1, (long[]) arr2);
        } else if (type == double.class) {
            return Arrays.equals((double[]) arr1, (double[]) arr2);
        } else if (type == float.class) {
            return Arrays.equals((float[]) arr1, (float[]) arr2);
        } else if (type == boolean.class) {
            return Arrays.equals((boolean[]) arr1, (boolean[]) arr2);
        } else if (type == byte.class) {
            return Arrays.equals((byte[]) arr1, (byte[]) arr2);
        } else if (type == char.class) {
            return Arrays.equals((char[]) arr1, (char[]) arr2);
        } else if (type == short.class) {
            return Arrays.equals((short[]) arr1, (short[]) arr2);
        }

        return false;
    }

    /**
     * Get fields with caching
     */
    private Field[] getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            while (c != null && c != Object.class) {
                fields.addAll(Arrays.asList(c.getDeclaredFields()));
                c = c.getSuperclass();
            }
            if (fields.size() > maxFieldsPerClass) {
                if (log.isWarnEnabled()) {
                    log.warn("[MICROSWITCH-COMPARATOR] - Deep comparison field cap reached for class {}: {} fields > maxFieldsPerClass={}, truncating",
                            clazz.getName(), fields.size(), maxFieldsPerClass);
                }
                return fields.subList(0, maxFieldsPerClass).toArray(new Field[0]);
            }
            return fields.toArray(new Field[0]);
        });
    }

    /**
     * Check if field should be skipped
     */
    private boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) ||
                Modifier.isTransient(modifiers) ||
                fieldsToIgnore.contains(field.getName());
    }

    /**
     * Check if type is primitive or wrapper
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Integer.class ||
                type == Long.class ||
                type == Double.class ||
                type == Float.class ||
                type == Boolean.class ||
                type == Byte.class ||
                type == Character.class ||
                type == Short.class;
    }

    /**
     * Check if two numeric values are equivalent despite different types
     * (e.g., Integer(123) vs Long(123L))
     */
    private boolean areNumericTypesEquivalent(Object value1, Object value2) {
        if (value1 == null || value2 == null) {
            return false;
        }

        Class<?> type1 = value1.getClass();
        Class<?> type2 = value2.getClass();

        // If same type, use regular equals
        if (type1.equals(type2)) {
            return Objects.equals(value1, value2);
        }

        // Check if both are numeric types
        if (isNumericType(type1) && isNumericType(type2)) {
            // Convert both to BigDecimal for accurate comparison
            try {
                java.math.BigDecimal bd1 = convertToBigDecimal(value1);
                java.math.BigDecimal bd2 = convertToBigDecimal(value2);
                return bd1.compareTo(bd2) == 0;
            } catch (Exception e) {
                // If conversion fails, fall back to string comparison
                return value1.toString().equals(value2.toString());
            }
        }

        return false;
    }

    /**
     * Check if a type is numeric
     */
    private boolean isNumericType(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Double.class || 
               type == Float.class || type == Short.class || type == Byte.class ||
               type == int.class || type == long.class || type == double.class ||
               type == float.class || type == short.class || type == byte.class ||
               type == java.math.BigDecimal.class || type == java.math.BigInteger.class;
    }

    /**
     * Convert numeric value to BigDecimal for comparison
     */
    private java.math.BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof Integer) {
            return java.math.BigDecimal.valueOf((Integer) value);
        } else if (value instanceof Long) {
            return java.math.BigDecimal.valueOf((Long) value);
        } else if (value instanceof Double) {
            return java.math.BigDecimal.valueOf((Double) value);
        } else if (value instanceof Float) {
            return java.math.BigDecimal.valueOf((Float) value);
        } else if (value instanceof Short) {
            return java.math.BigDecimal.valueOf((Short) value);
        } else if (value instanceof Byte) {
            return java.math.BigDecimal.valueOf((Byte) value);
        } else if (value instanceof java.math.BigDecimal) {
            return (java.math.BigDecimal) value;
        } else if (value instanceof java.math.BigInteger) {
            return new java.math.BigDecimal((java.math.BigInteger) value);
        } else {
            // Try to parse as string
            return new java.math.BigDecimal(value.toString());
        }
    }

    private boolean timeBudgetExceededAndLog(String where, long startedAtNanos) {
        if (this.maxCompareTimeMillis <= 0L) {
            return false;
        }
        long elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L;
        if (elapsedMillis > this.maxCompareTimeMillis) {
            if (log.isWarnEnabled()) {
                log.warn("[MICROSWITCH-COMPARATOR] - Deep comparison time budget exceeded (>{} ms) at {} after {} ms; returning early",
                        this.maxCompareTimeMillis, where, elapsedMillis);
            }
            return true;
        }
        return false;
    }
}

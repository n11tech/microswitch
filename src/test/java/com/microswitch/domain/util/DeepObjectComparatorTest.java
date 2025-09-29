package com.microswitch.domain.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeepObjectComparator Tests")
class DeepObjectComparatorTest {

    private ObjectMapper objectMapper;
    private DeepObjectComparator defaultComparator;
    private DeepObjectComparator hybridComparator;
    private DeepObjectComparator jsonComparator;
    private DeepObjectComparator reflectionComparator;

    // Use Map instead of custom classes to avoid Jackson module issues
    private Map<String, Object> createSampleApiResponse() {
        return Map.of(
                "data", List.of(
                        Map.of("_id", "26645811", "itemId_", "46768576453", "contentId_", "5818237330", "productId_", 760250794),
                        Map.of("_id", "26638850", "itemId_", "46760576453", "contentId_", "5813237950", "productId_", 267259995),
                        Map.of("_id", "26521750", "itemId_", "46730576453", "contentId_", "5818437950", "productId_", 43822946)
                ),
                "statusCode", 200
        );
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        defaultComparator = DeepObjectComparator.defaultComparator();
        
        hybridComparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.HYBRID)
                .withMaxDepth(10)
                .compareNullsAsEqual(false)
                .ignoreFields("timestamp", "requestId", "traceId")
                .build();
                
        jsonComparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.JSON_BASED)
                .build();
                
        reflectionComparator = DeepObjectComparator.builder()
                .withStrategy(DeepObjectComparator.ComparisonStrategy.REFLECTION_BASED)
                .build();
    }

    @Test
    @DisplayName("Should detect identical JSON responses as equal")
    void shouldDetectIdenticalJsonResponsesAsEqual() throws Exception {
        // Given: Identical JSON strings
        String testJson = """
            {
              "data": [
                {
                  "_id": "26645811",
                  "itemId_": "46768576453",
                  "contentId_": "5818237330",
                  "productId_": 760250794
                },
                {
                  "_id": "26638850",
                  "itemId_": "46760576453",
                  "contentId_": "5813237950",
                  "productId_": 267259995
                }
              ],
              "statusCode": 200
            }""";

        // When: Parse JSON to Maps
        @SuppressWarnings("unchecked")
        Map<String, Object> apiResponse1 = objectMapper.readValue(testJson, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> apiResponse2 = objectMapper.readValue(testJson, Map.class);

        // Then: All comparison strategies should detect them as equal
        assertTrue(defaultComparator.areEqual(apiResponse1, apiResponse2),
                "Default comparator should detect identical responses as equal");
        assertTrue(hybridComparator.areEqual(apiResponse1, apiResponse2),
                "Hybrid comparator should detect identical responses as equal");
        assertTrue(jsonComparator.areEqual(apiResponse1, apiResponse2),
                "JSON comparator should detect identical responses as equal");
        assertTrue(reflectionComparator.areEqual(apiResponse1, apiResponse2),
                "Reflection comparator should detect identical responses as equal");
    }

    @ParameterizedTest
    @EnumSource(DeepObjectComparator.ComparisonStrategy.class)
    @DisplayName("Should detect identical objects as equal across all strategies")
    void shouldDetectIdenticalObjectsAsEqualAcrossAllStrategies(DeepObjectComparator.ComparisonStrategy strategy) {
        // Given: Identical objects
        Map<String, Object> response1 = createSampleApiResponse();
        Map<String, Object> response2 = createSampleApiResponse();

        DeepObjectComparator comparator = DeepObjectComparator.builder()
                .withStrategy(strategy)
                .build();

        // When & Then
        assertTrue(comparator.areEqual(response1, response2),
                "Strategy " + strategy + " should detect identical objects as equal");
    }

    @Test
    @DisplayName("Should detect differences in data content")
    void shouldDetectDifferencesInDataContent() {
        // Given: Responses with different data
        Map<String, Object> response1 = createSampleApiResponse();
        Map<String, Object> response2 = Map.of(
                "data", List.of(
                        Map.of("_id", "26645811", "itemId_", "46768576453", "contentId_", "5818237330", "productId_", 999999), // Different product ID
                        Map.of("_id", "26638850", "itemId_", "46760576453", "contentId_", "5813237950", "productId_", 267259995),
                        Map.of("_id", "26521750", "itemId_", "46730576453", "contentId_", "5818437950", "productId_", 43822946)
                ),
                "statusCode", 200
        );

        // When & Then
        assertFalse(defaultComparator.areEqual(response1, response2),
                "Should detect difference in product ID");
    }

    @Test
    @DisplayName("Should detect differences in status fields")
    void shouldDetectDifferencesInStatusFields() {
        // Given: Responses with different status
        Map<String, Object> response1 = createSampleApiResponse();
        Map<String, Object> response2 = Map.of(
                "data", List.of(
                        Map.of("_id", "26645811", "itemId_", "46768576453", "contentId_", "5818237330", "productId_", 760250794),
                        Map.of("_id", "26638850", "itemId_", "46760576453", "contentId_", "5813237950", "productId_", 267259995),
                        Map.of("_id", "26521750", "itemId_", "46730576453", "contentId_", "5818437950", "productId_", 43822946)
                ),
                "statusCode", 500 // Different status code
        );

        // When & Then
        assertFalse(defaultComparator.areEqual(response1, response2),
                "Should detect differences in status fields");
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void shouldHandleNullValuesCorrectly() {
        // Given: One null, one non-null
        Map<String, Object> response1 = createSampleApiResponse();
        Map<String, Object> response2 = null;

        // When & Then
        assertFalse(defaultComparator.areEqual(response1, response2),
                "Should detect null vs non-null as different");
        assertFalse(defaultComparator.areEqual(response2, response1),
                "Should detect null vs non-null as different (reversed)");
        assertTrue(defaultComparator.areEqual(response2, response2),
                "Should detect null vs null as equal");
    }

    @Test
    @DisplayName("Should handle empty data lists")
    void shouldHandleEmptyDataLists() {
        // Given: Responses with empty data
        Map<String, Object> response1 = Map.of(
                "data", List.of(),
                "statusCode", 200
        );
        Map<String, Object> response2 = Map.of(
                "data", List.of(),
                "statusCode", 200
        );

        // When & Then
        assertTrue(defaultComparator.areEqual(response1, response2),
                "Should detect empty lists as equal");
    }

    @Test
    @DisplayName("Should detect order differences in lists")
    void shouldDetectOrderDifferencesInLists() {
        // Given: Same data but different order
        Map<String, Object> response1 = Map.of(
                "data", List.of(
                        Map.of("_id", "1", "itemId_", "content1", "productId_", 100),
                        Map.of("_id", "2", "itemId_", "content2", "productId_", 200)
                ),
                "statusCode", 200
        );
        Map<String, Object> response2 = Map.of(
                "data", List.of(
                        Map.of("_id", "2", "itemId_", "content2", "productId_", 200),
                        Map.of("_id", "1", "itemId_", "content1", "productId_", 100)
                ),
                "statusCode", 200
        );

        // When & Then
        assertFalse(defaultComparator.areEqual(response1, response2),
                "Should detect order differences in lists");
    }

    @Test
    @DisplayName("Should ignore specified fields")
    void shouldIgnoreSpecifiedFields() {
        // Given: Comparator that ignores 'statusCode' field
        DeepObjectComparator ignoringComparator = DeepObjectComparator.builder()
                .ignoreFields("statusCode")
                .withStrategy(DeepObjectComparator.ComparisonStrategy.JSON_BASED)
                .build();

        Map<String, Object> response1 = createSampleApiResponse(); // statusCode: 200
        Map<String, Object> response2 = Map.of(
                "data", List.of(
                        Map.of("_id", "26645811", "itemId_", "46768576453", "contentId_", "5818237330", "productId_", 760250794),
                        Map.of("_id", "26638850", "itemId_", "46760576453", "contentId_", "5813237950", "productId_", 267259995),
                        Map.of("_id", "26521750", "itemId_", "46730576453", "contentId_", "5818437950", "productId_", 43822946)
                ),
                "statusCode", 500 // Different status code - should be ignored
        );

        // When & Then
        assertTrue(ignoringComparator.areEqual(response1, response2),
                "Should ignore differences in 'statusCode' field");
        assertFalse(defaultComparator.areEqual(response1, response2),
                "Default comparator should detect differences in 'statusCode' field");
    }

    @Test
    @DisplayName("Should handle Map-like objects correctly")
    void shouldHandleMapLikeObjectsCorrectly() {
        // Given: Map representations of the same data
        Map<String, Object> map1 = Map.of(
                "success", true,
                "message", "Success",
                "statusCode", 200
        );
        Map<String, Object> map2 = Map.of(
                "success", true,
                "message", "Success",
                "statusCode", 200
        );

        // When & Then
        assertTrue(defaultComparator.areEqual(map1, map2),
                "Should detect identical maps as equal");
    }

    @Test
    @DisplayName("Should detect subtle differences in nested objects")
    void shouldDetectSubtleDifferencesInNestedObjects() throws Exception {
        // Given: JSON with subtle differences (extra whitespace, different formatting)
        String json1 = """
            {
              "data": [
                {
                  "id": "26638251",
                  "contentId": "5818237950",
                  "productId": 260250994
                }
              ],
              "success": true,
              "message": "Success",
              "statusCode": 200
            }""";

        String json2 = """
            {"data":[{"id":"26638251","contentId":"5818237950","productId":260250994}],"success":true,"message":"Success","statusCode":200}""";

        Map response1 = objectMapper.readValue(json1, Map.class);
        Map response2 = objectMapper.readValue(json2, Map.class);

        // When & Then: Should be equal despite formatting differences
        assertTrue(defaultComparator.areEqual(response1, response2),
                "Should ignore JSON formatting differences");
    }

    @Test
    @DisplayName("Should handle numeric type differences")
    void shouldHandleNumericTypeDifferences() {
        // Given: Simple numeric values with different types
        Long longValue = 100L;
        Integer intValue = 100;
        
        // Test direct numeric comparison first
        assertTrue(defaultComparator.areEqual(longValue, intValue),
                "Should handle Long vs Integer correctly");
        
        // Test in Map context
        Map<String, Object> item1 = Map.of("productId_", 100L);
        Map<String, Object> item2 = Map.of("productId_", 100);
        
        assertTrue(defaultComparator.areEqual(item1, item2),
                "Should handle same numeric values in Maps correctly");
    }

    @Test
    @DisplayName("Should provide detailed debugging information")
    void shouldProvideDetailedDebuggingInformation() {
        // Given: Objects that might have subtle differences
        Map<String, Object> response1 = createSampleApiResponse();
        Map<String, Object> response2 = Map.of(
                "data", List.of(
                        Map.of("_id", "26645811 ", "itemId_", "46768576453", "contentId_", "5818237330", "productId_", 760250794), // Extra space in _id
                        Map.of("_id", "26638850", "itemId_", "46760576453", "contentId_", "5813237950", "productId_", 267259995),
                        Map.of("_id", "26521750", "itemId_", "46730576453", "contentId_", "5818437950", "productId_", 43822946)
                ),
                "statusCode", 200
        );

        // When & Then
        assertFalse(defaultComparator.areEqual(response1, response2),
                "Should detect subtle string differences (extra whitespace)");
    }

}

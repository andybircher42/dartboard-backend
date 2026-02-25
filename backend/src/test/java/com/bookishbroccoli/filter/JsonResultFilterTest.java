package com.bookishbroccoli.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonResultFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private JsonResultFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JsonResultFilter(objectMapper);
  }

  // ==================== HELPERS ====================

  private ObjectNode item(long price, String category) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("price", price);
    node.put("name", price + " Item");
    if (category != null) {
      ObjectNode details = objectMapper.createObjectNode();
      details.put("category", category);
      node.set("details", details);
    }
    return node;
  }

  private ArrayNode array(ObjectNode... items) {
    ArrayNode arr = objectMapper.createArrayNode();
    for (ObjectNode item : items) {
      arr.add(item);
    }
    return arr;
  }

  private ObjectNode wrapper(String itemsField, ObjectNode... items) {
    ObjectNode result = objectMapper.createObjectNode();
    result.put("label", "test-set");
    result.put("count", items.length);
    ArrayNode arr = objectMapper.createArrayNode();
    for (ObjectNode item : items) {
      arr.add(item);
    }
    result.set(itemsField, arr);
    return result;
  }

  private static final Map<String, List<String>> TYPE_ALIASES =
      Map.of(
          "houses", List.of("SINGLE_FAMILY"),
          "condos", List.of("CONDO", "APARTMENT"),
          "townhomes", List.of("TOWNHOUSE"));

  // ==================== filterArray ====================

  @Nested
  class FilterArrayTests {

    @Test
    void noRules_returnsOriginal() {
      ArrayNode input = array(item(100, "A"), item(200, "B"));

      ArrayNode result = filter.filterArray(input, List.of());

      assertSame(input, result);
    }

    @Test
    void nullRules_returnsOriginal() {
      ArrayNode input = array(item(100, "A"));

      ArrayNode result = filter.filterArray(input, null);

      assertSame(input, result);
    }

    @Test
    void singleRule_filters() {
      ArrayNode input = array(item(100, null), item(500, null), item(900, null));

      ArrayNode result = filter.filterArray(input, List.of(new RangeFilter("price", 200.0, 800.0)));

      assertEquals(1, result.size());
      assertEquals(500, result.get(0).get("price").asLong());
    }

    @Test
    void multipleRules_allMustMatch() {
      ArrayNode input =
          array(
              item(200, "SINGLE_FAMILY"),
              item(400, "SINGLE_FAMILY"),
              item(400, "CONDO"),
              item(600, "SINGLE_FAMILY"));

      ArrayNode result =
          filter.filterArray(
              input,
              List.of(
                  new RangeFilter("price", 300.0, 500.0),
                  new CategoryFilter("details.category", Set.of("SINGLE_FAMILY"))));

      assertEquals(1, result.size());
      assertEquals(400, result.get(0).get("price").asLong());
    }

    @Test
    void emptyArray_returnsEmpty() {
      ArrayNode input = array();

      ArrayNode result = filter.filterArray(input, List.of(new RangeFilter("price", 100.0, 200.0)));

      assertEquals(0, result.size());
    }

    @Test
    void allFilteredOut_returnsEmpty() {
      ArrayNode input = array(item(100, null), item(200, null));

      ArrayNode result = filter.filterArray(input, List.of(new RangeFilter("price", 500.0, null)));

      assertEquals(0, result.size());
    }
  }

  // ==================== filter (wrapper) ====================

  @Nested
  class FilterWrapperTests {

    @Test
    void noRules_returnsOriginal() {
      ObjectNode input = wrapper("items", item(100, null));

      JsonNode result = filter.filter(input, "items", List.of());

      assertSame(input, result);
    }

    @Test
    void nullRules_returnsOriginal() {
      ObjectNode input = wrapper("items", item(100, null));

      JsonNode result = filter.filter(input, "items", null);

      assertSame(input, result);
    }

    @Test
    void missingItemsField_returnsOriginal() {
      ObjectNode input = wrapper("items", item(100, null));

      JsonNode result =
          filter.filter(input, "nonexistent", List.of(new RangeFilter("price", 500.0, null)));

      assertSame(input, result);
    }

    @Test
    void filtersAndUpdatesCount() {
      ObjectNode input =
          wrapper(
              "items",
              item(200, "SINGLE_FAMILY"),
              item(400, "SINGLE_FAMILY"),
              item(600, "SINGLE_FAMILY"));

      JsonNode result =
          filter.filter(input, "items", List.of(new RangeFilter("price", 300.0, null)));

      assertEquals(2, result.get("count").asInt());
      assertEquals(400, result.get("items").get(0).get("price").asLong());
      assertEquals(600, result.get("items").get(1).get("price").asLong());
    }

    @Test
    void preservesOtherFields() {
      ObjectNode input = wrapper("items", item(400, null));
      input.put("label", "my-label");
      input.put("source", "api");

      JsonNode result =
          filter.filter(input, "items", List.of(new RangeFilter("price", 300.0, 500.0)));

      assertEquals("my-label", result.get("label").asText());
      assertEquals("api", result.get("source").asText());
    }

    @Test
    void allFilteredOut_zeroCount() {
      ObjectNode input = wrapper("items", item(100, null), item(200, null));

      JsonNode result =
          filter.filter(input, "items", List.of(new RangeFilter("price", 500.0, null)));

      assertEquals(0, result.get("count").asInt());
      assertTrue(result.get("items").isEmpty());
    }

    @Test
    void emptyItems_zeroCount() {
      ObjectNode input = wrapper("items");

      JsonNode result =
          filter.filter(input, "items", List.of(new RangeFilter("price", 100.0, 200.0)));

      assertEquals(0, result.get("count").asInt());
    }

    @Test
    void customItemsFieldName_works() {
      ObjectNode input = objectMapper.createObjectNode();
      input.put("total", 2);
      ArrayNode properties = objectMapper.createArrayNode();
      properties.add(item(300, null));
      properties.add(item(600, null));
      input.set("properties", properties);

      JsonNode result =
          filter.filter(input, "properties", List.of(new RangeFilter("price", null, 400.0)));

      assertEquals(1, result.get("count").asInt());
      assertEquals(300, result.get("properties").get(0).get("price").asLong());
    }

    @Test
    void combinedRangeAndCategory_filters() {
      ObjectNode input =
          wrapper(
              "items",
              item(200, "SINGLE_FAMILY"),
              item(400, "SINGLE_FAMILY"),
              item(400, "CONDO"),
              item(600, "SINGLE_FAMILY"));

      JsonNode result =
          filter.filter(
              input,
              "items",
              List.of(
                  new RangeFilter("price", 300.0, 500.0),
                  new CategoryFilter("details.category", "houses", TYPE_ALIASES)));

      assertEquals(1, result.get("count").asInt());
      assertEquals(400, result.get("items").get(0).get("price").asLong());
    }
  }
}

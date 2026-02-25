package com.dartboardbackend.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class RangeFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private ObjectNode item(long price) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("price", price);
    node.put("name", price + " Item");
    return node;
  }

  // ==================== matches ====================

  @Test
  void minOnly_excludesBelow() {
    RangeFilter rule = new RangeFilter("price", 300000.0, null);

    assertFalse(rule.matches(item(200000)));
    assertTrue(rule.matches(item(300000)));
    assertTrue(rule.matches(item(500000)));
  }

  @Test
  void maxOnly_excludesAbove() {
    RangeFilter rule = new RangeFilter("price", null, 500000.0);

    assertTrue(rule.matches(item(200000)));
    assertTrue(rule.matches(item(500000)));
    assertFalse(rule.matches(item(600000)));
  }

  @Test
  void minAndMax_keepsInRange() {
    RangeFilter rule = new RangeFilter("price", 300000.0, 500000.0);

    assertFalse(rule.matches(item(200000)));
    assertTrue(rule.matches(item(300000)));
    assertTrue(rule.matches(item(400000)));
    assertTrue(rule.matches(item(500000)));
    assertFalse(rule.matches(item(600000)));
  }

  @Test
  void boundariesAreInclusive() {
    RangeFilter rule = new RangeFilter("price", 300000.0, 500000.0);

    assertTrue(rule.matches(item(300000)));
    assertTrue(rule.matches(item(500000)));
  }

  @Test
  void missingField_passes() {
    RangeFilter rule = new RangeFilter("price", 300000.0, 500000.0);
    ObjectNode noPrice = objectMapper.createObjectNode();
    noPrice.put("name", "no price");

    assertTrue(rule.matches(noPrice));
  }

  @Test
  void nestedField_resolves() {
    RangeFilter rule = new RangeFilter("details.score", 50.0, null);

    ObjectNode node = objectMapper.createObjectNode();
    ObjectNode details = objectMapper.createObjectNode();
    details.put("score", 80);
    node.set("details", details);

    assertTrue(rule.matches(node));

    details.put("score", 30);
    assertFalse(rule.matches(node));
  }

  @Test
  void nonNumericField_passes() {
    RangeFilter rule = new RangeFilter("name", 10.0, 100.0);

    assertTrue(rule.matches(item(50)));
  }

  @Test
  void nullMinAndMax_alwaysPasses() {
    RangeFilter rule = new RangeFilter("price", null, null);

    assertTrue(rule.matches(item(0)));
    assertTrue(rule.matches(item(999999999)));
  }

  // ==================== field path validation ====================

  @Test
  void nullFieldPath_throws() {
    assertThrows(IllegalArgumentException.class, () -> new RangeFilter(null, 0.0, 100.0));
  }

  @Test
  void emptyFieldPath_throws() {
    assertThrows(IllegalArgumentException.class, () -> new RangeFilter("", 0.0, 100.0));
  }

  @Test
  void fieldPathWithNumbers_throws() {
    assertThrows(IllegalArgumentException.class, () -> new RangeFilter("field123", 0.0, 100.0));
  }

  @Test
  void fieldPathWithSpecialChars_throws() {
    assertThrows(IllegalArgumentException.class, () -> new RangeFilter("field@name", 0.0, 100.0));
  }

  @Test
  void validDottedFieldPath_accepted() {
    assertDoesNotThrow(() -> new RangeFilter("details.price", 0.0, 100.0));
  }

  // ==================== min/max validation ====================

  @Test
  void minGreaterThanMax_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new RangeFilter("price", 500000.0, 300000.0));
  }

  @Test
  void minEqualsMax_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new RangeFilter("price", 300000.0, 300000.0));
  }

  @Test
  void minLessThanMax_accepted() {
    assertDoesNotThrow(() -> new RangeFilter("price", 100.0, 500.0));
  }

  // ==================== resolveField ====================

  @Test
  void resolveField_topLevel() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("name", "test");

    assertEquals("test", RangeFilter.resolveField(node, "name").orElseThrow().asText());
  }

  @Test
  void resolveField_nested() {
    ObjectNode node = objectMapper.createObjectNode();
    ObjectNode inner = objectMapper.createObjectNode();
    inner.put("value", 42);
    node.set("outer", inner);

    assertEquals(42, RangeFilter.resolveField(node, "outer.value").orElseThrow().asInt());
  }

  @Test
  void resolveField_deeplyNested() {
    ObjectNode node = objectMapper.createObjectNode();
    ObjectNode a = objectMapper.createObjectNode();
    ObjectNode b = objectMapper.createObjectNode();
    b.put("c", "deep");
    a.set("b", b);
    node.set("a", a);

    assertEquals("deep", RangeFilter.resolveField(node, "a.b.c").orElseThrow().asText());
  }

  @Test
  void resolveField_missingField_returnsNull() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("name", "test");

    assertTrue(RangeFilter.resolveField(node, "missing").isEmpty());
  }

  @Test
  void resolveField_missingNestedField_returnsNull() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("name", "test");

    assertTrue(RangeFilter.resolveField(node, "a.b.c").isEmpty());
  }
}

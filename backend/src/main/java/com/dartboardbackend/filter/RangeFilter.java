package com.dartboardbackend.filter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Filters items by a numeric field within an inclusive [min, max] range. Supports dot-separated
 * field paths for nested objects (e.g. "details.price"). Items missing the field or with
 * non-numeric values pass through (are not excluded). Min and max are each independently optional —
 * null means unbounded.
 */
public class RangeFilter implements FilterRule {

  private final String fieldPath;
  private final Double min;
  private final Double max;

  /**
   * Creates a range filter for the given numeric field.
   *
   * @param fieldPath dot-separated path to the numeric field (e.g. "details.price")
   * @param min minimum inclusive bound, or {@code null} for unbounded
   * @param max maximum inclusive bound, or {@code null} for unbounded
   */
  public RangeFilter(String fieldPath, Double min, Double max) {
    this.fieldPath = fieldPath;
    this.min = min;
    this.max = max;
  }

  /**
   * Returns true if the item's numeric field value falls within [min, max]. Items missing the field
   * or with non-numeric values pass through.
   *
   * @param item the JSON object to test
   * @return true if the value is within range or the field is absent/non-numeric; false otherwise
   */
  @Override
  public boolean matches(JsonNode item) {
    Optional<JsonNode> valueNode = resolveField(item, fieldPath);
    if (valueNode.isEmpty() || !valueNode.get().isNumber()) {
      return true;
    }

    double value = valueNode.get().doubleValue();
    if (min != null && value < min) {
      return false;
    }
    if (max != null && value > max) {
      return false;
    }
    return true;
  }

  /**
   * Walks a dot-separated field path on a JSON node, returning the leaf node or empty if any
   * segment is missing.
   *
   * @param node the root JSON node to traverse
   * @param path dot-separated field path (e.g. "details.price")
   * @return an {@link Optional} containing the leaf node, or empty if any segment is missing
   */
  static Optional<JsonNode> resolveField(JsonNode node, String path) {
    JsonNode current = node;
    for (String segment : path.split("\\.")) {
      if (current == null || current.isMissingNode()) {
        return Optional.empty();
      }
      current = current.get(segment);
    }
    return Optional.ofNullable(current);
  }

  /**
   * Returns the dot-separated field path this filter inspects.
   *
   * @return the field path
   */
  public String getFieldPath() {
    return fieldPath;
  }

  /**
   * Returns the minimum bound, or {@code null} if unbounded.
   *
   * @return the minimum inclusive bound, or null
   */
  public Double getMin() {
    return min;
  }

  /**
   * Returns the maximum bound, or {@code null} if unbounded.
   *
   * @return the maximum inclusive bound, or null
   */
  public Double getMax() {
    return max;
  }
}

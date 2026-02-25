package com.dartboardbackend.filter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Filters items by a string field matching one of a set of allowed values. Supports dot-separated
 * field paths for nested objects (e.g. "homeInfo.homeType"). Items missing the field pass through
 * (are not excluded). Matching is case-insensitive.
 *
 * <p>Optionally supports alias mapping: user-friendly category names (e.g. "houses") are expanded
 * to one or more canonical values (e.g. "SINGLE_FAMILY") before matching. If no alias map is
 * provided, the allowed values are used directly.
 */
public class CategoryFilter implements FilterRule {

  private final String fieldPath;
  private final Set<String> allowedValues;

  /**
   * Creates a filter with explicit allowed values.
   *
   * @param fieldPath dot-separated path to the string field
   * @param allowedValues the allowed values (matched case-insensitively)
   */
  public CategoryFilter(String fieldPath, Set<String> allowedValues) {
    this.fieldPath = fieldPath;
    this.allowedValues =
        allowedValues.stream().map(String::toUpperCase).collect(Collectors.toSet());
  }

  /**
   * Creates a filter from a comma-separated string of category keys, expanded through an alias map.
   *
   * @param fieldPath dot-separated path to the string field
   * @param categories comma-separated category keys (e.g. "houses,townhomes")
   * @param aliasMap maps each category key to its canonical values
   */
  public CategoryFilter(String fieldPath, String categories, Map<String, List<String>> aliasMap) {
    this.fieldPath = fieldPath;
    this.allowedValues = expandAliases(categories, aliasMap);
  }

  /**
   * Returns true if the item's field value (resolved via the configured field path) matches one of
   * the allowed values (case-insensitive). Items missing the field or with non-textual values pass
   * through.
   *
   * @param item the JSON object to test
   * @return true if the item matches or lacks the target field; false otherwise
   */
  @Override
  public boolean matches(JsonNode item) {
    if (allowedValues.isEmpty()) {
      return true;
    }

    Optional<JsonNode> valueNode = RangeFilter.resolveField(item, fieldPath);
    if (valueNode.isEmpty() || !valueNode.get().isTextual()) {
      return true;
    }

    // return false;
    return allowedValues.contains(valueNode.get().asText().toUpperCase());
  }

  /**
   * Expands comma-separated category keys through an alias map into a set of upper-cased canonical
   * values. Each key is trimmed and lower-cased before lookup. Keys not present in the alias map
   * are silently ignored.
   *
   * @param categories comma-separated category keys (e.g. "houses,townhomes")
   * @param aliasMap maps each lower-cased category key to its canonical values
   * @return a mutable set of upper-cased canonical values
   */
  static Set<String> expandAliases(String categories, Map<String, List<String>> aliasMap) {
    Set<String> expanded = new HashSet<>();
    if (categories == null || categories.isBlank()) {
      return expanded;
    }
    for (String token : categories.split(",")) {
      String key = token.trim().toLowerCase();
      Optional.ofNullable(aliasMap.get(key))
          .ifPresent(
              values -> {
                for (String v : values) {
                  expanded.add(v.toUpperCase());
                }
              });
    }
    return expanded;
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
   * Returns an unmodifiable view of the upper-cased allowed values.
   *
   * @return unmodifiable set of allowed values
   */
  public Set<String> getAllowedValues() {
    return Collections.unmodifiableSet(allowedValues);
  }
}

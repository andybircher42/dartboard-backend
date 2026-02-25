package com.dartboardbackend.filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.dartboardbackend.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A {@link FilterRule} that keeps JSON items whose string field value is one of a set of allowed
 * values. Used by {@link JsonResultFilter} to narrow down API results by category — for example,
 * keeping only "SINGLE_FAMILY" and "TOWNHOUSE" listings from a real-estate response.
 *
 * <h3>Field paths</h3>
 *
 * The {@code fieldPath} is a dot-separated path that navigates into nested JSON objects. For
 * example, the path {@code "homeInfo.homeType"} resolves the {@code homeType} field inside the
 * {@code homeInfo} object:
 *
 * <pre>{@code
 * { "homeInfo": { "homeType": "SINGLE_FAMILY" } }
 * }</pre>
 *
 * A top-level field like {@code "status"} works as well. Field paths must contain only letters and
 * dots (validated on construction).
 *
 * <h3>Two ways to specify allowed values</h3>
 *
 * <p><b>1. Explicit values</b> — pass the canonical values directly:
 *
 * <pre>{@code
 * // Only keep items where homeInfo.homeType is "SINGLE_FAMILY" or "TOWNHOUSE"
 * new CategoryFilter("homeInfo.homeType", Set.of("SINGLE_FAMILY", "TOWNHOUSE"));
 * }</pre>
 *
 * <p><b>2. Alias expansion</b> — pass user-friendly category names and a map that translates each
 * name to one or more canonical values. This is useful when an API uses internal codes (like
 * {@code "SINGLE_FAMILY"}) but the caller uses simpler names (like {@code "houses"}):
 *
 * <pre>{@code
 * Map<String, List<String>> aliases = Map.of(
 *     "houses",    List.of("SINGLE_FAMILY"),
 *     "condos",    List.of("CONDO", "APARTMENT"),
 *     "townhomes", List.of("TOWNHOUSE"));
 *
 * // "houses,townhomes" expands to {"SINGLE_FAMILY", "TOWNHOUSE"}
 * new CategoryFilter("homeInfo.homeType", "houses,townhomes", aliases);
 * }</pre>
 *
 * Category names that don't appear in the alias map are silently ignored. If every name is unknown
 * (or the input is null/blank), the filter allows all items through.
 *
 * <h3>Matching behaviour</h3>
 *
 * <ul>
 *   <li>Matching is <b>case-insensitive</b> — a field value of {@code "single_family"} matches an
 *       allowed value of {@code "SINGLE_FAMILY"}.
 *   <li>Items that are <b>missing the target field</b> (or whose field is not a text node) are
 *       <b>not excluded</b> — they pass through the filter.
 *   <li>An <b>empty allowed-values set</b> (including when all alias keys are unknown) means no
 *       restriction: every item passes.
 * </ul>
 */
public class CategoryFilter implements FilterRule {

  /** Letters and dots only — e.g. {@code "homeInfo.homeType"}. */
  private static final Pattern FIELD_PATH_PATTERN = Pattern.compile("[a-zA-Z.]+");

  /** Letters and underscores only — e.g. {@code "SINGLE_FAMILY"}. */
  private static final Pattern ALLOWED_VALUE_PATTERN = Pattern.compile("[a-zA-Z_]+");

  private final String fieldPath;
  private final Set<String> allowedValues;

  /**
   * Creates a filter that keeps items whose field value matches one of the given values.
   *
   * <p>The {@code fieldPath} is trimmed and lower-cased internally. Each value in
   * {@code allowedValues} is stored upper-cased for case-insensitive comparison. An empty set means
   * no restriction (all items pass).
   *
   * @param fieldPath dot-separated path to the target field (letters and dots only, e.g.
   *     {@code "homeInfo.homeType"})
   * @param allowedValues canonical values to match against (letters and underscores only, e.g.
   *     {@code Set.of("SINGLE_FAMILY", "TOWNHOUSE")})
   * @throws IllegalArgumentException if {@code fieldPath} is null/empty or contains characters
   *     other than letters and dots, or if any value in {@code allowedValues} contains characters
   *     other than letters and underscores
   */
  public CategoryFilter(String fieldPath, Set<String> allowedValues) {
    validateFieldPath(fieldPath);
    validateAllowedValues(allowedValues);
    this.fieldPath = StringUtils.toLower(fieldPath);
    this.allowedValues =
        allowedValues.stream().map(StringUtils::toUpper).collect(Collectors.toSet());
  }

  /**
   * Creates a filter from user-friendly category names that are expanded into canonical values via
   * an alias map.
   *
   * <p>Each comma-separated key in {@code categories} is trimmed, lower-cased, and looked up in
   * {@code aliasMap}. The resulting canonical values are collected and upper-cased. Keys that don't
   * exist in the map are silently ignored. If {@code categories} is null, blank, or contains only
   * unknown keys, the allowed-values set will be empty and all items will pass through.
   *
   * <p>Example: given an alias map {@code {"houses" -> ["SINGLE_FAMILY"], "condos" -> ["CONDO",
   * "APARTMENT"]}}, the categories string {@code "houses, condos"} produces the allowed-values set
   * {@code {"SINGLE_FAMILY", "CONDO", "APARTMENT"}}.
   *
   * @param fieldPath dot-separated path to the target field (letters and dots only, e.g.
   *     {@code "homeInfo.homeType"})
   * @param categories comma-separated category keys (letters, commas, underscores, and spaces
   *     only, e.g. {@code "houses,townhomes"}), or null/blank for no restriction
   * @param aliasMap maps each lower-cased category key to its list of canonical field values
   * @throws IllegalArgumentException if {@code fieldPath} is null/empty or contains characters
   *     other than letters and dots, or if {@code categories} contains characters other than
   *     letters, commas, underscores, and spaces
   */
  public CategoryFilter(String fieldPath, String categories, Map<String, List<String>> aliasMap) {
    this(fieldPath, expandAliases(categories, aliasMap));
  }

  private static void validateFieldPath(String fieldPath) {
    validateField(fieldPath, FIELD_PATH_PATTERN);
  }

  private static void validateAllowedValues(Set<String> allowedValues) {
    for (String value : allowedValues) {
      validateField(value, ALLOWED_VALUE_PATTERN);
    }
  }

  /**
   * Validates that a value is non-null and, after trimming, fully matches the given pattern.
   *
   * @param value the value to validate (may be {@code null})
   * @param pattern the regex pattern the trimmed value must fully match
   * @throws IllegalArgumentException if {@code value} is null, blank after trimming, or contains
   *     characters not permitted by the pattern
   */
  static void validateField(String value, Pattern pattern) {
    if (value == null || !pattern.matcher(value.trim()).matches()) {
      throw new IllegalArgumentException(
          String.format("field must match [%s], got: %s", pattern, value));
    }
  }

  /**
   * Tests whether this item should be kept.
   *
   * <p>Returns {@code true} (keep the item) when any of the following hold:
   *
   * <ul>
   *   <li>The allowed-values set is empty (no restriction).
   *   <li>The item does not contain the target field (missing fields are not penalised).
   *   <li>The target field is not a text node (e.g. it is a number or object).
   *   <li>The target field's text value, compared case-insensitively, matches one of the allowed
   *       values.
   * </ul>
   *
   * <p>Returns {@code false} only when the target field is present, is textual, and its value does
   * not match any allowed value.
   *
   * @param item the JSON object to test
   * @return {@code true} if the item should be kept; {@code false} if it should be filtered out
   */
  @Override
  public boolean matches(JsonNode item) {
    if (allowedValues.isEmpty()) {
      return true;
    }

    Optional<String> value =
        RangeFilter.resolveField(item, fieldPath).flatMap(StringUtils::toString);

    return value.map(v -> allowedValues.contains(StringUtils.toUpper(v)))
    .orElse(true);
  }

  /**
   * Splits a comma-separated string of category keys, looks each one up in the alias map, and
   * collects all resulting canonical values into a single upper-cased set.
   *
   * <p>Each key is trimmed and lower-cased before lookup so that {@code " Houses "} matches a map
   * entry keyed by {@code "houses"}. Keys not found in the map are silently skipped. Returns an
   * empty set when {@code categories} is null, blank, or contains only unknown keys.
   *
   * @param categories comma-separated category keys (e.g. {@code "houses,townhomes"})
   * @param aliasMap maps each lower-cased category key to its list of canonical values
   * @return a mutable set of upper-cased canonical values, or an empty set
   */
  static Set<String> expandAliases(String categories, Map<String, List<String>> aliasMap) {
    return StringUtils.split(categories, ",").stream()
    .map(StringUtils::normalize)
        .map(aliasMap::get)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .map(String::toUpperCase)
        .collect(Collectors.toSet());
  }

  /**
   * Returns the dot-separated field path this filter inspects (e.g. {@code "homeinfo.hometype"}).
   * For filters created with the Set constructor, this value is trimmed and lower-cased.
   *
   * @return the field path
   */
  public String getFieldPath() {
    return fieldPath;
  }

  /**
   * Returns an unmodifiable view of the upper-cased allowed values that this filter matches
   * against. An empty set means the filter has no restriction and all items pass.
   *
   * @return unmodifiable set of upper-cased allowed values
   */
  public Set<String> getAllowedValues() {
    return Collections.unmodifiableSet(allowedValues);
  }
}

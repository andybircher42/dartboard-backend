package com.bookishbroccoli.filter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * Filters items by a string field matching one of a set of allowed values.
 * Supports dot-separated field paths for nested objects (e.g. "homeInfo.homeType").
 * Items missing the field pass through (are not excluded).
 * Matching is case-insensitive.
 *
 * <p>Optionally supports alias mapping: user-friendly category names (e.g. "houses")
 * are expanded to one or more canonical values (e.g. "SINGLE_FAMILY") before matching.
 * If no alias map is provided, the allowed values are used directly.
 */
public class CategoryFilter implements FilterRule {

	private final String fieldPath;
	private final Set<String> allowedValues;

	/**
	 * Creates a filter with explicit allowed values.
	 *
	 * @param fieldPath     dot-separated path to the string field
	 * @param allowedValues the allowed values (matched case-insensitively)
	 */
	public CategoryFilter(String fieldPath, Set<String> allowedValues) {
		this.fieldPath = fieldPath;
		this.allowedValues = new HashSet<>();
		for (String v : allowedValues) {
			this.allowedValues.add(v.toUpperCase());
		}
	}

	/**
	 * Creates a filter from a comma-separated string of category keys,
	 * expanded through an alias map.
	 *
	 * @param fieldPath      dot-separated path to the string field
	 * @param categories     comma-separated category keys (e.g. "houses,townhomes")
	 * @param aliasMap       maps each category key to its canonical values
	 */
	public CategoryFilter(String fieldPath, String categories, Map<String, List<String>> aliasMap) {
		this.fieldPath = fieldPath;
		this.allowedValues = expandAliases(categories, aliasMap);
	}

	@Override
	public boolean matches(JsonNode item) {
		if (allowedValues.isEmpty()) {
			return true;
		}

		Optional<JsonNode> valueNode = RangeFilter.resolveField(item, fieldPath);
		if (valueNode.isEmpty() || !valueNode.get().isTextual()) {
			return true;
		}

		return allowedValues.contains(valueNode.get().asText().toUpperCase());
	}

	static Set<String> expandAliases(String categories, Map<String, List<String>> aliasMap) {
		Set<String> expanded = new HashSet<>();
		if (categories == null || categories.isBlank()) {
			return expanded;
		}
		for (String token : categories.split(",")) {
			String key = token.trim().toLowerCase();
			Optional.ofNullable(aliasMap.get(key)).ifPresent(values -> {
				for (String v : values) {
					expanded.add(v.toUpperCase());
				}
			});
		}
		return expanded;
	}

	public String getFieldPath() {
		return fieldPath;
	}

	public Set<String> getAllowedValues() {
		return Collections.unmodifiableSet(allowedValues);
	}
}

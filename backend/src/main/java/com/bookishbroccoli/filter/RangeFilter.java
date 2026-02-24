package com.bookishbroccoli.filter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Filters items by a numeric field within an inclusive [min, max] range.
 * Supports dot-separated field paths for nested objects (e.g. "details.price").
 * Items missing the field or with non-numeric values pass through (are not excluded).
 * Min and max are each independently optional — null means unbounded.
 */
public class RangeFilter implements FilterRule {

	private final String fieldPath;
	private final Double min;
	private final Double max;

	public RangeFilter(String fieldPath, Double min, Double max) {
		this.fieldPath = fieldPath;
		this.min = min;
		this.max = max;
	}

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

	public String getFieldPath() {
		return fieldPath;
	}

	public Double getMin() {
		return min;
	}

	public Double getMax() {
		return max;
	}
}

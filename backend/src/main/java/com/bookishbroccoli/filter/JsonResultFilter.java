package com.bookishbroccoli.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Applies a list of {@link FilterRule}s to filter items in a JSON array.
 * Stateless — safe to reuse across requests.
 *
 * <p>Provides two modes:
 * <ul>
 *   <li>{@link #filterArray} — filters a bare JSON array</li>
 *   <li>{@link #filter} — filters items inside a wrapper object
 *       (e.g. {@code {"count":3,"items":[...]}}), updating the count</li>
 * </ul>
 */
public class JsonResultFilter {

	private static final Logger logger = LoggerFactory.getLogger(JsonResultFilter.class);

	private final ObjectMapper objectMapper;

	public JsonResultFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Filters a JSON array, keeping only items that match all rules.
	 *
	 * @param items the array of JSON objects to filter
	 * @param rules the filter rules to apply (all must match)
	 * @return a new array containing only matching items
	 */
	public ArrayNode filterArray(ArrayNode items, List<FilterRule> rules) {
		if (rules == null || rules.isEmpty()) {
			return items;
		}

		ArrayNode filtered = objectMapper.createArrayNode();
		for (JsonNode item : items) {
			if (matchesAll(item, rules)) {
				filtered.add(item);
			}
		}

		logger.info("Filtered items: {} -> {} results", items.size(), filtered.size());
		return filtered;
	}

	/**
	 * Filters items inside a wrapper object. Reads the array from {@code itemsField},
	 * applies rules, and returns a copy of the wrapper with the filtered array and
	 * an updated "count" field.
	 *
	 * <p>If no rules are provided or the items field is missing, returns the original unchanged.
	 *
	 * @param result     the wrapper JSON object
	 * @param itemsField the field name containing the array (e.g. "properties", "results")
	 * @param rules      the filter rules to apply
	 * @return filtered result with updated count
	 */
	public JsonNode filter(JsonNode result, String itemsField, List<FilterRule> rules) {
		if (rules == null || rules.isEmpty()) {
			return result;
		}

		JsonNode itemsNode = result.get(itemsField);
		if (itemsNode == null || !itemsNode.isArray()) {
			return result;
		}

		ArrayNode filtered = filterArray((ArrayNode) itemsNode, rules);

		ObjectNode copy = objectMapper.createObjectNode();
		result.fields().forEachRemaining(entry -> {
			if (entry.getKey().equals(itemsField)) {
				copy.set(itemsField, filtered);
			} else if (entry.getKey().equals("count")) {
				copy.put("count", filtered.size());
			} else {
				copy.set(entry.getKey(), entry.getValue());
			}
		});

		if (!copy.has("count")) {
			copy.put("count", filtered.size());
		}

		return copy;
	}

	private boolean matchesAll(JsonNode item, List<FilterRule> rules) {
		for (FilterRule rule : rules) {
			if (!rule.matches(item)) {
				return false;
			}
		}
		return true;
	}
}

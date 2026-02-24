package com.bookishbroccoli.filter;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A predicate that decides whether a JSON item passes a filter criterion.
 * Implementations should be stateless and safe to reuse across requests.
 */
public interface FilterRule {

	/**
	 * Returns true if the item matches this rule (should be kept).
	 * Items that lack the target field should generally pass (not be excluded).
	 */
	boolean matches(JsonNode item);
}

package com.bookishbroccoli.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryFilterTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final Map<String, List<String>> TYPE_ALIASES = Map.of(
			"houses", List.of("SINGLE_FAMILY"),
			"condos", List.of("CONDO", "APARTMENT"),
			"townhomes", List.of("TOWNHOUSE")
	);

	private ObjectNode item(String category) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("price", 0);
		if (category != null) {
			ObjectNode details = objectMapper.createObjectNode();
			details.put("category", category);
			node.set("details", details);
		}
		return node;
	}

	private ObjectNode itemTopLevelCategory(String category) {
		ObjectNode node = objectMapper.createObjectNode();
		if (category != null) {
			node.put("category", category);
		}
		return node;
	}

	// ==================== matches ====================

	@Test
	void explicitValues_matchesCaseInsensitive() {
		CategoryFilter rule = new CategoryFilter("details.category",
				Set.of("SINGLE_FAMILY"));

		assertTrue(rule.matches(item("SINGLE_FAMILY")));
		assertTrue(rule.matches(item("single_family")));
		assertFalse(rule.matches(item("CONDO")));
	}

	@Test
	void topLevelField_works() {
		CategoryFilter rule = new CategoryFilter("category",
				Set.of("ACTIVE"));

		assertTrue(rule.matches(itemTopLevelCategory("ACTIVE")));
		assertTrue(rule.matches(itemTopLevelCategory("active")));
		assertFalse(rule.matches(itemTopLevelCategory("SOLD")));
	}

	@Test
	void missingField_passes() {
		CategoryFilter rule = new CategoryFilter("details.category",
				Set.of("SINGLE_FAMILY"));

		assertTrue(rule.matches(item(null)));
	}

	@Test
	void emptyAllowedValues_matchesAll() {
		CategoryFilter rule = new CategoryFilter("details.category",
				Set.of());

		assertTrue(rule.matches(item("ANYTHING")));
	}

	@Test
	void aliasMap_expandsCategories() {
		CategoryFilter rule = new CategoryFilter("details.category",
				"houses", TYPE_ALIASES);

		assertTrue(rule.matches(item("SINGLE_FAMILY")));
		assertFalse(rule.matches(item("CONDO")));
		assertFalse(rule.matches(item("TOWNHOUSE")));
	}

	@Test
	void aliasMap_multipleCategories() {
		CategoryFilter rule = new CategoryFilter("details.category",
				"houses,townhomes", TYPE_ALIASES);

		assertTrue(rule.matches(item("SINGLE_FAMILY")));
		assertFalse(rule.matches(item("CONDO")));
		assertTrue(rule.matches(item("TOWNHOUSE")));
	}

	@Test
	void aliasMap_condosMatchesCondoAndApartment() {
		CategoryFilter rule = new CategoryFilter("details.category",
				"condos", TYPE_ALIASES);

		assertFalse(rule.matches(item("SINGLE_FAMILY")));
		assertTrue(rule.matches(item("CONDO")));
		assertTrue(rule.matches(item("APARTMENT")));
		assertFalse(rule.matches(item("TOWNHOUSE")));
	}

	@Test
	void aliasMap_allThreeTypes() {
		CategoryFilter rule = new CategoryFilter("details.category",
				"houses,condos,townhomes", TYPE_ALIASES);

		assertTrue(rule.matches(item("SINGLE_FAMILY")));
		assertTrue(rule.matches(item("CONDO")));
		assertTrue(rule.matches(item("APARTMENT")));
		assertTrue(rule.matches(item("TOWNHOUSE")));
	}

	@Test
	void aliasMap_unknownCategory_matchesAll() {
		CategoryFilter rule = new CategoryFilter("details.category",
				"unknown_type", TYPE_ALIASES);

		assertTrue(rule.matches(item("SINGLE_FAMILY")));
		assertTrue(rule.matches(item("CONDO")));
	}

	@Test
	void aliasMap_nullCategories_matchesAll() {
		CategoryFilter rule = new CategoryFilter("details.category",
				null, TYPE_ALIASES);

		assertTrue(rule.matches(item("ANYTHING")));
	}

	@Test
	void aliasMap_blankCategories_matchesAll() {
		CategoryFilter rule = new CategoryFilter("details.category",
				"  ", TYPE_ALIASES);

		assertTrue(rule.matches(item("ANYTHING")));
	}

	// ==================== expandAliases ====================

	@Test
	void expandAliases_singleKey() {
		Set<String> result = CategoryFilter.expandAliases("houses", TYPE_ALIASES);

		assertEquals(Set.of("SINGLE_FAMILY"), result);
	}

	@Test
	void expandAliases_multipleKeys() {
		Set<String> result = CategoryFilter.expandAliases("houses,condos", TYPE_ALIASES);

		assertEquals(Set.of("SINGLE_FAMILY", "CONDO", "APARTMENT"), result);
	}

	@Test
	void expandAliases_unknownKey_returnsEmpty() {
		Set<String> result = CategoryFilter.expandAliases("unknown", TYPE_ALIASES);

		assertTrue(result.isEmpty());
	}

	@Test
	void expandAliases_null_returnsEmpty() {
		Set<String> result = CategoryFilter.expandAliases(null, TYPE_ALIASES);

		assertTrue(result.isEmpty());
	}

	@Test
	void expandAliases_blank_returnsEmpty() {
		Set<String> result = CategoryFilter.expandAliases("  ", TYPE_ALIASES);

		assertTrue(result.isEmpty());
	}

	@Test
	void expandAliases_trimming_works() {
		Set<String> result = CategoryFilter.expandAliases(" houses , townhomes ", TYPE_ALIASES);

		assertEquals(Set.of("SINGLE_FAMILY", "TOWNHOUSE"), result);
	}
}

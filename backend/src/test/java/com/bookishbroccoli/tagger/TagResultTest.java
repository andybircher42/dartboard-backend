package com.bookishbroccoli.tagger;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TagResultTest {

	private static final Tag LUXURY = Tag.of("luxury", "price", "Luxury");
	private static final Tag BUDGET = Tag.of("budget", "price", "Budget");

	@Test
	void strict_filtersStrictOnly() {
		TagResult result = new TagResult(
				List.of(LUXURY, BUDGET),
				Map.of("luxury", "strict", "budget", "relaxed"));

		assertEquals(List.of(LUXURY), result.strict());
	}

	@Test
	void relaxed_filtersRelaxedOnly() {
		TagResult result = new TagResult(
				List.of(LUXURY, BUDGET),
				Map.of("luxury", "strict", "budget", "relaxed"));

		assertEquals(List.of(BUDGET), result.relaxed());
	}

	@Test
	void hasTag_true() {
		TagResult result = new TagResult(List.of(LUXURY), Map.of("luxury", "strict"));

		assertTrue(result.hasTag(LUXURY));
	}

	@Test
	void hasTag_false() {
		TagResult result = new TagResult(List.of(LUXURY), Map.of("luxury", "strict"));

		assertFalse(result.hasTag(BUDGET));
	}
}

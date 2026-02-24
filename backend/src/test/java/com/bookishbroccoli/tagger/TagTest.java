package com.bookishbroccoli.tagger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TagTest {

	@Test
	void of_createsWithFields() {
		Tag tag = Tag.of("test", "category", "Test Tag");

		assertEquals("test", tag.getName());
		assertEquals("category", tag.getCategory());
		assertEquals("Test Tag", tag.getDisplayName());
	}

	@Test
	void equals_sameNameAndCategory() {
		Tag a = Tag.of("luxury", "price", "Luxury");
		Tag b = Tag.of("luxury", "price", "Luxury Home");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void notEquals_differentName() {
		Tag a = Tag.of("luxury", "price", "Luxury");
		Tag b = Tag.of("budget", "price", "Budget");

		assertNotEquals(a, b);
	}

	@Test
	void notEquals_differentCategory() {
		Tag a = Tag.of("large", "size", "Large");
		Tag b = Tag.of("large", "amenity", "Large");

		assertNotEquals(a, b);
	}

	@Test
	void toString_categoryColonName() {
		Tag tag = Tag.of("luxury", "price", "Luxury");

		assertEquals("price:luxury", tag.toString());
	}
}

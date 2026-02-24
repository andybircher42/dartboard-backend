package com.bookishbroccoli.freshness;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FreshnessResultTest {

	@Test
	void fresh_notStale() {
		Instant ts = Instant.parse("2026-01-15T10:00:00Z");
		FreshnessResult result = FreshnessResult.fresh(ts, Duration.ofHours(2));

		assertFalse(result.stale());
	}

	@Test
	void fresh_hasTimestampAndAge() {
		Instant ts = Instant.parse("2026-01-15T10:00:00Z");
		Duration age = Duration.ofHours(2);
		FreshnessResult result = FreshnessResult.fresh(ts, age);

		assertEquals(ts, result.timestamp());
		assertEquals(age, result.age());
		assertNull(result.reason());
	}

	@Test
	void stale_withReasonOnly() {
		FreshnessResult result = FreshnessResult.stale("no timestamp available");

		assertTrue(result.stale());
		assertNull(result.timestamp());
		assertNull(result.age());
		assertEquals("no timestamp available", result.reason());
	}

	@Test
	void stale_isFreshReturnsFalse() {
		FreshnessResult result = FreshnessResult.stale("missing");

		assertFalse(result.isFresh());
	}

	@Test
	void stale_withTimestampAndAge() {
		Instant ts = Instant.parse("2026-01-01T00:00:00Z");
		Duration age = Duration.ofDays(15);
		FreshnessResult result = FreshnessResult.stale(ts, age, "too old");

		assertTrue(result.stale());
		assertNotNull(result.timestamp());
		assertNotNull(result.age());
		assertEquals("too old", result.reason());
	}

	@Test
	void stale_withTimestampAndAge_preservesValues() {
		Instant ts = Instant.parse("2026-01-01T00:00:00Z");
		Duration age = Duration.ofDays(15);
		FreshnessResult result = FreshnessResult.stale(ts, age, "expired");

		assertEquals(ts, result.timestamp());
		assertEquals(age, result.age());
	}

	@Test
	void isFresh_fresh_returnsTrue() {
		FreshnessResult result = FreshnessResult.fresh(Instant.now(), Duration.ofMinutes(5));

		assertTrue(result.isFresh());
	}

	@Test
	void isFresh_stale_returnsFalse() {
		FreshnessResult result = FreshnessResult.stale("expired");

		assertFalse(result.isFresh());
	}
}

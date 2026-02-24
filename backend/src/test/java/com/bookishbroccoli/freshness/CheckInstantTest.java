package com.bookishbroccoli.freshness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CheckInstantTest {

	private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

	private FreshnessChecker checker;
	private FreshnessPolicy policy;

	@BeforeEach
	void setUp() {
		checker = new FreshnessChecker(Clock.fixed(NOW, ZoneOffset.UTC));
		policy = FreshnessPolicy.of("test", Duration.ofDays(7));
	}

	@Test
	void recent_fresh() {
		Instant recent = NOW.minus(Duration.ofHours(6));
		FreshnessResult result = checker.check(recent, policy);

		assertTrue(result.isFresh());
		assertEquals(recent, result.timestamp());
		assertEquals(Duration.ofHours(6), result.age());
	}

	@Test
	void old_stale() {
		Instant old = NOW.minus(Duration.ofDays(10));
		FreshnessResult result = checker.check(old, policy);

		assertTrue(result.stale());
		assertEquals(old, result.timestamp());
		assertNotNull(result.reason());
	}

	@Test
	void exactMaxAge_fresh() {
		Instant exact = NOW.minus(Duration.ofDays(7));
		FreshnessResult result = checker.check(exact, policy);

		assertTrue(result.isFresh());
	}

	@Test
	void null_staleWhenMissing_stale() {
		FreshnessResult result = checker.check((Instant) null, policy);

		assertTrue(result.stale());
		assertNull(result.timestamp());
		assertEquals("no timestamp available", result.reason());
	}

	@Test
	void null_lenient_fresh() {
		FreshnessPolicy lenient = FreshnessPolicy.of("lenient", Duration.ofDays(7), false);
		FreshnessResult result = checker.check((Instant) null, lenient);

		assertTrue(result.isFresh());
	}

	@Test
	void future_fresh() {
		Instant future = NOW.plus(Duration.ofHours(1));
		FreshnessResult result = checker.check(future, policy);

		assertTrue(result.isFresh());
		assertEquals(Duration.ZERO, result.age());
	}
}

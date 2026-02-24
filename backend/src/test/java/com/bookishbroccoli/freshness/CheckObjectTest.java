package com.bookishbroccoli.freshness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CheckObjectTest {

	private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

	private FreshnessChecker checker;
	private FreshnessPolicy policy;

	@BeforeEach
	void setUp() {
		checker = new FreshnessChecker(Clock.fixed(NOW, ZoneOffset.UTC));
		policy = FreshnessPolicy.of("test", Duration.ofDays(7));
	}

	@Test
	void instant_dispatches() {
		Instant recent = NOW.minus(Duration.ofHours(1));
		FreshnessResult result = checker.check((Object) recent, policy);

		assertTrue(result.isFresh());
	}

	@Test
	void string_dispatches() {
		FreshnessResult result = checker.check((Object) NOW.minus(Duration.ofHours(1)).toString(), policy);

		assertTrue(result.isFresh());
	}

	@Test
	void long_dispatches() {
		Long epochMillis = NOW.minus(Duration.ofHours(1)).toEpochMilli();
		FreshnessResult result = checker.check((Object) epochMillis, policy);

		assertTrue(result.isFresh());
	}

	@Test
	void date_dispatches() {
		Date date = Date.from(NOW.minus(Duration.ofHours(1)));
		FreshnessResult result = checker.check((Object) date, policy);

		assertTrue(result.isFresh());
	}

	@Test
	void null_stale() {
		FreshnessResult result = checker.check((Object) null, policy);

		assertTrue(result.stale());
	}

	@Test
	void unsupportedType_stale() {
		FreshnessResult result = checker.check((Object) 42, policy);

		assertTrue(result.stale());
		assertTrue(result.reason().contains("unsupported"));
	}
}

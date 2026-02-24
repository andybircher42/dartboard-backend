package com.bookishbroccoli.retry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

	@Test
	void of_createsWithAllFields() {
		RetryPolicy policy = RetryPolicy.of("api", 4, 10_000, 60_000);

		assertEquals("api", policy.getName());
		assertEquals(4, policy.getMaxAttempts());
		assertEquals(10_000, policy.getInitialBackoffMs());
		assertEquals(60_000, policy.getMaxBackoffMs());
	}

	@Test
	void of_defaultsBackoff() {
		RetryPolicy policy = RetryPolicy.of("default", 3);

		assertEquals("default", policy.getName());
		assertEquals(3, policy.getMaxAttempts());
		assertEquals(10_000, policy.getInitialBackoffMs());
		assertEquals(60_000, policy.getMaxBackoffMs());
	}

	@Test
	void of_nullName_throws() {
		assertThrows(NullPointerException.class, () -> RetryPolicy.of(null, 3));
	}

	@Test
	void of_zeroAttempts_throws() {
		assertThrows(IllegalArgumentException.class, () -> RetryPolicy.of("bad", 0));
	}

	@Test
	void getBackoffMs_exponential() {
		RetryPolicy policy = RetryPolicy.of("test", 5, 10_000, 60_000);

		assertEquals(10_000, policy.getBackoffMs(1));  // 10000 * 2^0
		assertEquals(20_000, policy.getBackoffMs(2));  // 10000 * 2^1
		assertEquals(40_000, policy.getBackoffMs(3));  // 10000 * 2^2
	}

	@Test
	void getBackoffMs_cappedAtMax() {
		RetryPolicy policy = RetryPolicy.of("test", 5, 10_000, 60_000);

		assertEquals(60_000, policy.getBackoffMs(4));   // 10000 * 2^3 = 80000, capped
		assertEquals(60_000, policy.getBackoffMs(10));  // way over cap
	}

	@Test
	void equals_sameName() {
		RetryPolicy a = RetryPolicy.of("api", 3, 1000, 5000);
		RetryPolicy b = RetryPolicy.of("api", 5, 2000, 10_000);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void notEquals_differentName() {
		RetryPolicy a = RetryPolicy.of("api", 3);
		RetryPolicy b = RetryPolicy.of("scraper", 3);

		assertNotEquals(a, b);
	}
}

package com.bookishbroccoli.retry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryResultTest {

	@Test
	void success_succeeded() {
		RetryResult<String> result = RetryResult.success("ok", 1);

		assertTrue(result.succeeded());
	}

	@Test
	void success_hasValueAndAttempts() {
		RetryResult<String> result = RetryResult.success("hello", 2);

		assertEquals("hello", result.value());
		assertEquals(2, result.attempts());
		assertNull(result.lastException());
	}

	@Test
	void failure_notSucceeded() {
		RetryResult<String> result = RetryResult.failure(3, new RuntimeException("boom"));

		assertFalse(result.succeeded());
	}

	@Test
	void failure_hasAttemptsAndException() {
		RuntimeException cause = new RuntimeException("boom");
		RetryResult<String> result = RetryResult.failure(4, cause);

		assertEquals(4, result.attempts());
		assertSame(cause, result.lastException());
		assertNull(result.value());
	}

	@Test
	void getOrThrow_success_returnsValue() {
		RetryResult<Integer> result = RetryResult.success(42, 1);

		assertEquals(42, result.getOrThrow());
	}

	@Test
	void getOrThrow_failure_throws() {
		RuntimeException cause = new RuntimeException("oops");
		RetryResult<Integer> result = RetryResult.failure(3, cause);

		RuntimeException ex = assertThrows(RuntimeException.class, result::getOrThrow);
		assertTrue(ex.getMessage().contains("3 attempts exhausted"));
		assertSame(cause, ex.getCause());
	}
}

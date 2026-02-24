package com.bookishbroccoli.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskResultTest {

	@Test
	void successWithCount() {
		TaskResult result = TaskResult.success(42);

		assertTrue(result.succeeded());
		assertEquals(42, result.resultCount());
		assertNull(result.error());
	}

	@Test
	void successNoArgs() {
		TaskResult result = TaskResult.success();

		assertTrue(result.succeeded());
		assertEquals(0, result.resultCount());
		assertNull(result.error());
	}

	@Test
	void failure() {
		TaskResult result = TaskResult.failure("something broke");

		assertFalse(result.succeeded());
		assertEquals(0, result.resultCount());
		assertEquals("something broke", result.error());
	}
}

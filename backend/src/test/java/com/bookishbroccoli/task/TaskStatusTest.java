package com.bookishbroccoli.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskStatusTest {

	@Test
	void isDone_completedAndFailed() {
		assertTrue(TaskStatus.COMPLETED.isDone());
		assertTrue(TaskStatus.FAILED.isDone());
	}

	@Test
	void isDone_pendingAndProcessing() {
		assertFalse(TaskStatus.PENDING.isDone());
		assertFalse(TaskStatus.PROCESSING.isDone());
	}

	@Test
	void isRunning() {
		assertTrue(TaskStatus.PROCESSING.isRunning());
		assertFalse(TaskStatus.PENDING.isRunning());
		assertFalse(TaskStatus.COMPLETED.isRunning());
		assertFalse(TaskStatus.FAILED.isRunning());
	}

	@Test
	void resolve_isAliasForFromString() {
		assertEquals(TaskStatus.PENDING, TaskStatus.resolve("pending"));
		assertEquals(TaskStatus.PROCESSING, TaskStatus.resolve("processing"));
		assertEquals(TaskStatus.COMPLETED, TaskStatus.resolve("completed"));
		assertEquals(TaskStatus.FAILED, TaskStatus.resolve("failed"));
	}

	@Test
	void fromString_byValue() {
		assertEquals(TaskStatus.PENDING, TaskStatus.fromString("pending"));
		assertEquals(TaskStatus.PROCESSING, TaskStatus.fromString("processing"));
		assertEquals(TaskStatus.COMPLETED, TaskStatus.fromString("completed"));
		assertEquals(TaskStatus.FAILED, TaskStatus.fromString("failed"));
	}

	@Test
	void fromString_byName() {
		assertEquals(TaskStatus.PENDING, TaskStatus.fromString("PENDING"));
		assertEquals(TaskStatus.PROCESSING, TaskStatus.fromString("PROCESSING"));
	}

	@Test
	void fromString_caseInsensitive() {
		assertEquals(TaskStatus.COMPLETED, TaskStatus.fromString("Completed"));
		assertEquals(TaskStatus.FAILED, TaskStatus.fromString("FAILED"));
	}

	@Test
	void fromString_unknownDefaultsToPending() {
		assertEquals(TaskStatus.PENDING, TaskStatus.fromString("bogus"));
		assertEquals(TaskStatus.PENDING, TaskStatus.fromString(""));
	}

	@Test
	void fromString_nullDefaultsToPending() {
		assertEquals(TaskStatus.PENDING, TaskStatus.fromString(null));
	}

	@Test
	void toStringReturnsValue() {
		assertEquals("pending", TaskStatus.PENDING.toString());
		assertEquals("processing", TaskStatus.PROCESSING.toString());
	}
}

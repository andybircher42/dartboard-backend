package com.bookishbroccoli.task;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskRecordTest {

	@Test
	void create_setsDefaults() {
		TaskRecord record = TaskRecord.create("test_type", Map.of("key", "val"), 3);

		assertNotNull(record.getId());
		assertEquals("test_type", record.getTaskType());
		assertEquals("val", record.getPayload().get("key"));
		assertEquals(TaskStatus.PENDING, record.getStatus());
		assertNotNull(record.getCreatedAt());
		assertNull(record.getStartedAt());
		assertNull(record.getCompletedAt());
		assertEquals(0, record.getAttempts());
		assertEquals(3, record.getMaxAttempts());
		assertNull(record.getError());
		assertNull(record.getWorkerId());
		assertEquals(0, record.getResultCount());
	}

	@Test
	void create_nullPayloadBecomesEmptyMap() {
		TaskRecord record = TaskRecord.create("test", null, 1);

		assertNotNull(record.getPayload());
		assertTrue(record.getPayload().isEmpty());
	}

	@Test
	void toMap_containsAllFields() {
		TaskRecord record = TaskRecord.create("my_type", Map.of("a", "b"), 5);

		Map<String, Object> map = record.toMap();

		assertEquals(record.getId(), map.get("id"));
		assertEquals("my_type", map.get("taskType"));
		assertEquals("pending", map.get("status"));
		assertEquals(0, map.get("attempts"));
		assertEquals(5, map.get("maxAttempts"));
		assertEquals(0, map.get("resultCount"));
		assertNotNull(map.get("createdAt"));
		assertNull(map.get("startedAt"));
		assertNull(map.get("completedAt"));
		assertNull(map.get("error"));
		assertNull(map.get("workerId"));
	}
}

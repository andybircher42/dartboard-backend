package com.bookishbroccoli.task;

import com.bookishbroccoli.service.ApifyNonRetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskProcessorTest {

	private InMemoryTaskRepository repo;
	private TaskProcessorProperties props;

	@BeforeEach
	void setUp() {
		repo = new InMemoryTaskRepository();
		props = new TaskProcessorProperties();
		props.setNumWorkers(2);
		props.setPollIntervalSeconds(60); // long interval so polling doesn't interfere
	}

	private TaskProcessor createProcessor(TaskHandler... handlers) {
		return new TaskProcessor(repo, props, List.of(handlers));
	}

	@Test
	void staleRecovery_resetsProcessingTasks() {
		TaskRecord stale = TaskRecord.create("test", Map.of(), 3);
		stale.setStatus(TaskStatus.PROCESSING);
		stale.setWorkerId("old-worker");
		repo.save(stale);

		TestHandler handler = new TestHandler("test", TaskResult.success());
		TaskProcessor processor = createProcessor(handler);

		int recovered = processor.recoverStaleTasks();

		assertEquals(1, recovered);
		assertEquals(TaskStatus.PENDING, repo.findById(stale.getId()).get().getStatus());
	}

	@Test
	void claimAndDispatch_claimsAndProcesses() throws Exception {
		TaskRecord task = TaskRecord.create("test", Map.of(), 3);
		repo.save(task);

		TestHandler handler = new TestHandler("test", TaskResult.success(5));
		TaskProcessor processor = createProcessor(handler);
		processor.init();

		try {
			processor.claimAndDispatch("test", 10);
			Thread.sleep(200); // allow async processing

			TaskRecord updated = repo.findById(task.getId()).get();
			assertEquals(TaskStatus.COMPLETED, updated.getStatus());
			assertEquals(5, updated.getResultCount());
		} finally {
			processor.shutdown();
		}
	}

	@Test
	void handlerRouting_dispatchesByType() throws Exception {
		TaskRecord taskA = TaskRecord.create("type_a", Map.of(), 3);
		TaskRecord taskB = TaskRecord.create("type_b", Map.of(), 3);
		repo.save(taskA);
		repo.save(taskB);

		TestHandler handlerA = new TestHandler("type_a", TaskResult.success(1));
		TestHandler handlerB = new TestHandler("type_b", TaskResult.success(2));
		TaskProcessor processor = createProcessor(handlerA, handlerB);
		processor.init();

		try {
			processor.processPendingTasks();
			Thread.sleep(200);

			assertEquals(TaskStatus.COMPLETED, repo.findById(taskA.getId()).get().getStatus());
			assertEquals(1, repo.findById(taskA.getId()).get().getResultCount());
			assertEquals(TaskStatus.COMPLETED, repo.findById(taskB.getId()).get().getStatus());
			assertEquals(2, repo.findById(taskB.getId()).get().getResultCount());
		} finally {
			processor.shutdown();
		}
	}

	@Test
	void successCompletion() {
		TaskRecord task = TaskRecord.create("test", Map.of(), 3);
		repo.save(task);
		repo.claimNextPending("test", "w1");

		TestHandler handler = new TestHandler("test", TaskResult.success(10));
		TaskProcessor processor = createProcessor(handler);

		processor.processTask(task);

		TaskRecord updated = repo.findById(task.getId()).get();
		assertEquals(TaskStatus.COMPLETED, updated.getStatus());
		assertEquals(10, updated.getResultCount());
	}

	@Test
	void retryableFailure_resetsToPending() {
		TaskRecord task = TaskRecord.create("test", Map.of(), 3);
		repo.save(task);
		repo.claimNextPending("test", "w1");

		TestHandler handler = new TestHandler("test", TaskResult.failure("transient error"));
		TaskProcessor processor = createProcessor(handler);

		processor.processTask(task);

		TaskRecord updated = repo.findById(task.getId()).get();
		assertEquals(TaskStatus.PENDING, updated.getStatus());
		assertEquals("transient error", updated.getError().orElseThrow());
	}

	@Test
	void nonRetryableException_failsPermanently() {
		TaskRecord task = TaskRecord.create("test", Map.of(), 3);
		repo.save(task);
		repo.claimNextPending("test", "w1");

		TaskHandler handler = new TaskHandler() {
			@Override
			public String getTaskType() {
				return "test";
			}

			@Override
			public TaskResult handle(TaskRecord t) {
				throw new ApifyNonRetryableException("permanent", 400, "bad request");
			}
		};
		TaskProcessor processor = createProcessor(handler);

		processor.processTask(task);

		TaskRecord updated = repo.findById(task.getId()).get();
		assertEquals(TaskStatus.FAILED, updated.getStatus());
		assertTrue(updated.getCompletedAt().isPresent());
	}

	@Test
	void unknownTaskType_failsPermanently() {
		TaskRecord task = TaskRecord.create("unknown_type", Map.of(), 3);
		repo.save(task);
		repo.claimNextPending("unknown_type", "w1");

		// no handler registered for "unknown_type"
		TestHandler handler = new TestHandler("other_type", TaskResult.success());
		TaskProcessor processor = createProcessor(handler);

		processor.processTask(task);

		TaskRecord updated = repo.findById(task.getId()).get();
		assertEquals(TaskStatus.FAILED, updated.getStatus());
		assertTrue(updated.getError().orElseThrow().contains("No handler registered"));
	}

	@Test
	void retryExhaustion_marksFailed() {
		TaskRecord task = TaskRecord.create("test", Map.of(), 1);
		repo.save(task);
		repo.claimNextPending("test", "w1"); // attempts=1, maxAttempts=1

		TestHandler handler = new TestHandler("test", TaskResult.failure("still failing"));
		TaskProcessor processor = createProcessor(handler);

		processor.processTask(task);

		TaskRecord updated = repo.findById(task.getId()).get();
		assertEquals(TaskStatus.FAILED, updated.getStatus());
	}

	@Test
	void exceptionInHandler_retriesTask() {
		TaskRecord task = TaskRecord.create("test", Map.of(), 3);
		repo.save(task);
		repo.claimNextPending("test", "w1");

		TaskHandler handler = new TaskHandler() {
			@Override
			public String getTaskType() {
				return "test";
			}

			@Override
			public TaskResult handle(TaskRecord t) {
				throw new RuntimeException("unexpected error");
			}
		};
		TaskProcessor processor = createProcessor(handler);

		processor.processTask(task);

		TaskRecord updated = repo.findById(task.getId()).get();
		assertEquals(TaskStatus.PENDING, updated.getStatus());
		assertEquals("unexpected error", updated.getError().orElseThrow());
	}

	@Test
	void getStatus_structure() {
		TestHandler handler = new TestHandler("my_type", TaskResult.success());
		TaskProcessor processor = createProcessor(handler);

		Map<String, Object> status = processor.getStatus();

		assertEquals(2, status.get("workers"));
		assertEquals(0, status.get("activeWorkers"));
		assertTrue(((List<?>) status.get("registeredTypes")).contains("my_type"));
		assertNotNull(status.get("queueCounts"));
	}

	@Test
	void shutdown_resetsProcessingTasks() throws Exception {
		TestHandler handler = new TestHandler("test", TaskResult.success());
		TaskProcessor processor = createProcessor(handler);
		processor.init();

		// Set task to PROCESSING after init so the scheduler doesn't pick it up
		TaskRecord processing = TaskRecord.create("other_type", Map.of(), 3);
		processing.setStatus(TaskStatus.PROCESSING);
		processing.setWorkerId("w1");
		repo.save(processing);

		processor.shutdown();

		assertEquals(TaskStatus.PENDING, repo.findById(processing.getId()).get().getStatus());
	}

	// ==================== Test Helper ====================

	static class TestHandler implements TaskHandler {
		private final String taskType;
		private final TaskResult result;

		TestHandler(String taskType, TaskResult result) {
			this.taskType = taskType;
			this.result = result;
		}

		@Override
		public String getTaskType() {
			return taskType;
		}

		@Override
		public TaskResult handle(TaskRecord task) {
			return result;
		}
	}
}

package com.bookishbroccoli.task;

import com.bookishbroccoli.service.ApifyNonRetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskProcessorTest {

	// ==================== TaskStatus ====================

	@Nested
	class TaskStatusTests {

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

	// ==================== TaskRecord ====================

	@Nested
	class TaskRecordTests {

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

	// ==================== TaskResult ====================

	@Nested
	class TaskResultTests {

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

	// ==================== InMemoryTaskRepository ====================

	@Nested
	class InMemoryTaskRepositoryTests {

		private InMemoryTaskRepository repo;

		@BeforeEach
		void setUp() {
			repo = new InMemoryTaskRepository();
		}

		@Test
		void save_andFindById() {
			TaskRecord task = TaskRecord.create("test", Map.of(), 3);
			repo.save(task);

			assertTrue(repo.findById(task.getId()).isPresent());
			assertEquals(task.getId(), repo.findById(task.getId()).get().getId());
		}

		@Test
		void findById_notFound() {
			assertTrue(repo.findById("nonexistent").isEmpty());
		}

		@Test
		void claimNextPending_returnsOldestFirst() {
			TaskRecord task1 = TaskRecord.create("type_a", Map.of(), 3);
			TaskRecord task2 = TaskRecord.create("type_a", Map.of(), 3);
			repo.save(task1);
			repo.save(task2);

			var claimed = repo.claimNextPending("type_a", "worker-1");

			assertTrue(claimed.isPresent());
			assertEquals(task1.getId(), claimed.get().getId());
			assertEquals(TaskStatus.PROCESSING, claimed.get().getStatus());
			assertEquals("worker-1", claimed.get().getWorkerId());
			assertEquals(1, claimed.get().getAttempts());
			assertNotNull(claimed.get().getStartedAt());
		}

		@Test
		void claimNextPending_skipsAlreadyClaimed() {
			TaskRecord task1 = TaskRecord.create("type_a", Map.of(), 3);
			TaskRecord task2 = TaskRecord.create("type_a", Map.of(), 3);
			repo.save(task1);
			repo.save(task2);

			repo.claimNextPending("type_a", "worker-1");
			var claimed2 = repo.claimNextPending("type_a", "worker-2");

			assertTrue(claimed2.isPresent());
			assertEquals(task2.getId(), claimed2.get().getId());
		}

		@Test
		void claimNextPending_wrongType_returnsEmpty() {
			TaskRecord task = TaskRecord.create("type_a", Map.of(), 3);
			repo.save(task);

			assertTrue(repo.claimNextPending("type_b", "worker-1").isEmpty());
		}

		@Test
		void claimNextPending_noPending_returnsEmpty() {
			assertTrue(repo.claimNextPending("type_a", "worker-1").isEmpty());
		}

		@Test
		void complete_setsStatusAndResults() {
			TaskRecord task = TaskRecord.create("test", Map.of(), 3);
			repo.save(task);
			repo.claimNextPending("test", "worker-1");

			repo.complete(task.getId(), 42);

			TaskRecord updated = repo.findById(task.getId()).get();
			assertEquals(TaskStatus.COMPLETED, updated.getStatus());
			assertEquals(42, updated.getResultCount());
			assertNotNull(updated.getCompletedAt());
			assertNull(updated.getWorkerId());
		}

		@Test
		void fail_withRetry_resetsTosPending() {
			TaskRecord task = TaskRecord.create("test", Map.of(), 3);
			repo.save(task);
			repo.claimNextPending("test", "worker-1");

			repo.fail(task.getId(), "transient error", true);

			TaskRecord updated = repo.findById(task.getId()).get();
			assertEquals(TaskStatus.PENDING, updated.getStatus());
			assertEquals("transient error", updated.getError());
			assertNull(updated.getWorkerId());
		}

		@Test
		void fail_retriesExhausted_marksFailed() {
			TaskRecord task = TaskRecord.create("test", Map.of(), 1);
			repo.save(task);
			repo.claimNextPending("test", "worker-1"); // attempts = 1, maxAttempts = 1

			repo.fail(task.getId(), "final error", true);

			TaskRecord updated = repo.findById(task.getId()).get();
			assertEquals(TaskStatus.FAILED, updated.getStatus());
			assertEquals("final error", updated.getError());
			assertNotNull(updated.getCompletedAt());
		}

		@Test
		void failPermanently_marksFailed() {
			TaskRecord task = TaskRecord.create("test", Map.of(), 5);
			repo.save(task);
			repo.claimNextPending("test", "worker-1");

			repo.failPermanently(task.getId(), "non-retryable error");

			TaskRecord updated = repo.findById(task.getId()).get();
			assertEquals(TaskStatus.FAILED, updated.getStatus());
			assertEquals("non-retryable error", updated.getError());
			assertNotNull(updated.getCompletedAt());
			assertNull(updated.getWorkerId());
		}

		@Test
		void findByFilters_byStatus() {
			TaskRecord pending = TaskRecord.create("test", Map.of(), 3);
			TaskRecord completed = TaskRecord.create("test", Map.of(), 3);
			completed.setStatus(TaskStatus.COMPLETED);
			repo.save(pending);
			repo.save(completed);

			List<TaskRecord> results = repo.findByFilters(TaskStatus.PENDING, null, 50);

			assertEquals(1, results.size());
			assertEquals(pending.getId(), results.get(0).getId());
		}

		@Test
		void findByFilters_byTaskType() {
			TaskRecord a = TaskRecord.create("type_a", Map.of(), 3);
			TaskRecord b = TaskRecord.create("type_b", Map.of(), 3);
			repo.save(a);
			repo.save(b);

			List<TaskRecord> results = repo.findByFilters(null, "type_a", 50);

			assertEquals(1, results.size());
			assertEquals(a.getId(), results.get(0).getId());
		}

		@Test
		void findByFilters_noFilters_returnsAll() {
			repo.save(TaskRecord.create("a", Map.of(), 1));
			repo.save(TaskRecord.create("b", Map.of(), 1));
			repo.save(TaskRecord.create("c", Map.of(), 1));

			List<TaskRecord> results = repo.findByFilters(null, null, 50);

			assertEquals(3, results.size());
		}

		@Test
		void findByFilters_respectsLimit() {
			for (int i = 0; i < 10; i++) {
				repo.save(TaskRecord.create("test", Map.of(), 1));
			}

			List<TaskRecord> results = repo.findByFilters(null, null, 3);

			assertEquals(3, results.size());
		}

		@Test
		void findPending_filtersCorrectly() {
			TaskRecord pending = TaskRecord.create("test", Map.of(), 3);
			TaskRecord processing = TaskRecord.create("test", Map.of(), 3);
			processing.setStatus(TaskStatus.PROCESSING);
			repo.save(pending);
			repo.save(processing);

			List<TaskRecord> results = repo.findPending("test", 10);

			assertEquals(1, results.size());
			assertEquals(pending.getId(), results.get(0).getId());
		}

		@Test
		void resetProcessingTasks_resetsTosPending() {
			TaskRecord processing1 = TaskRecord.create("test", Map.of(), 3);
			processing1.setStatus(TaskStatus.PROCESSING);
			processing1.setWorkerId("w1");
			TaskRecord processing2 = TaskRecord.create("test", Map.of(), 3);
			processing2.setStatus(TaskStatus.PROCESSING);
			processing2.setWorkerId("w2");
			TaskRecord pending = TaskRecord.create("test", Map.of(), 3);
			repo.save(processing1);
			repo.save(processing2);
			repo.save(pending);

			int reset = repo.resetProcessingTasks();

			assertEquals(2, reset);
			assertEquals(TaskStatus.PENDING, repo.findById(processing1.getId()).get().getStatus());
			assertEquals(TaskStatus.PENDING, repo.findById(processing2.getId()).get().getStatus());
			assertNull(repo.findById(processing1.getId()).get().getWorkerId());
			assertEquals(TaskStatus.PENDING, repo.findById(pending.getId()).get().getStatus());
		}

		@Test
		void getQueueCounts_countsAllStatuses() {
			repo.save(TaskRecord.create("a", Map.of(), 1));
			repo.save(TaskRecord.create("a", Map.of(), 1));
			TaskRecord completed = TaskRecord.create("a", Map.of(), 1);
			completed.setStatus(TaskStatus.COMPLETED);
			repo.save(completed);

			Map<String, Object> counts = repo.getQueueCounts();

			assertEquals(2L, counts.get("pending"));
			assertEquals(0L, counts.get("processing"));
			assertEquals(1L, counts.get("completed"));
			assertEquals(0L, counts.get("failed"));
			assertEquals(3L, counts.get("total"));
		}

		@Test
		void clear_removesAllTasks() {
			repo.save(TaskRecord.create("a", Map.of(), 1));
			repo.save(TaskRecord.create("b", Map.of(), 1));

			repo.clear();

			assertEquals(0L, repo.getQueueCounts().get("total"));
		}
	}

	// ==================== TaskProcessor ====================

	@Nested
	class TaskProcessorTests {

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
		void retryableFailure_resetsTosPending() {
			TaskRecord task = TaskRecord.create("test", Map.of(), 3);
			repo.save(task);
			repo.claimNextPending("test", "w1");

			TestHandler handler = new TestHandler("test", TaskResult.failure("transient error"));
			TaskProcessor processor = createProcessor(handler);

			processor.processTask(task);

			TaskRecord updated = repo.findById(task.getId()).get();
			assertEquals(TaskStatus.PENDING, updated.getStatus());
			assertEquals("transient error", updated.getError());
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
			assertNotNull(updated.getCompletedAt());
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
			assertTrue(updated.getError().contains("No handler registered"));
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
			assertEquals("unexpected error", updated.getError());
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
	}

	// ==================== TaskController ====================

	@Nested
	class TaskControllerTests {

		private InMemoryTaskRepository repo;
		private TaskProcessor processor;
		private TaskController controller;

		@BeforeEach
		void setUp() {
			repo = new InMemoryTaskRepository();
			TaskProcessorProperties props = new TaskProcessorProperties();
			props.setNumWorkers(1);
			props.setPollIntervalSeconds(60);
			processor = new TaskProcessor(repo, props, List.of());
			controller = new TaskController(repo, processor);
		}

		@Test
		void enqueue_validRequest() {
			var response = controller.enqueue(Map.of(
					"taskType", "my_task",
					"payload", Map.of("key", "val"),
					"maxAttempts", 5
			));

			assertEquals(200, response.getStatusCode().value());
			assertEquals("my_task", response.getBody().get("taskType"));
			assertEquals("pending", response.getBody().get("status"));
			assertEquals(5, response.getBody().get("maxAttempts"));
			assertNotNull(response.getBody().get("id"));
		}

		@Test
		void enqueue_defaultMaxAttempts() {
			var response = controller.enqueue(Map.of(
					"taskType", "my_task"
			));

			assertEquals(200, response.getStatusCode().value());
			assertEquals(3, response.getBody().get("maxAttempts"));
		}

		@Test
		void enqueue_missingTaskType_returnsBadRequest() {
			var response = controller.enqueue(Map.of("payload", Map.of()));

			assertEquals(400, response.getStatusCode().value());
			assertTrue(response.getBody().get("error").toString().contains("taskType"));
		}

		@Test
		void enqueue_blankTaskType_returnsBadRequest() {
			var response = controller.enqueue(Map.of("taskType", "  "));

			assertEquals(400, response.getStatusCode().value());
		}

		@Test
		void getTasks_noFilters() {
			controller.enqueue(Map.of("taskType", "a"));
			controller.enqueue(Map.of("taskType", "b"));

			var response = controller.getTasks(null, null, 50);

			assertEquals(200, response.getStatusCode().value());
			assertEquals(2, response.getBody().size());
		}

		@Test
		void getTasks_filterByTaskType() {
			controller.enqueue(Map.of("taskType", "a"));
			controller.enqueue(Map.of("taskType", "b"));

			var response = controller.getTasks(null, "a", 50);

			assertEquals(1, response.getBody().size());
			assertEquals("a", response.getBody().get(0).get("taskType"));
		}

		@Test
		void getTasks_filterByStatus() {
			controller.enqueue(Map.of("taskType", "a"));

			var response = controller.getTasks("pending", null, 50);

			assertEquals(1, response.getBody().size());
		}

		@Test
		void getTask_found() {
			var enqueued = controller.enqueue(Map.of("taskType", "test"));
			String taskId = (String) enqueued.getBody().get("id");

			var response = controller.getTask(taskId);

			assertEquals(200, response.getStatusCode().value());
			assertEquals(taskId, response.getBody().get("id"));
		}

		@Test
		void getTask_notFound() {
			var response = controller.getTask("nonexistent");

			assertEquals(404, response.getStatusCode().value());
		}

		@Test
		void getStatus_returnsProcessorStatus() {
			var response = controller.getStatus();

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody().get("workers"));
			assertNotNull(response.getBody().get("queueCounts"));
		}
	}

	// ==================== Test Helpers ====================

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

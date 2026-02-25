package com.dartboardbackend.task;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryTaskRepositoryTest {

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
    assertEquals("worker-1", claimed.get().getWorkerId().orElseThrow());
    assertEquals(1, claimed.get().getAttempts());
    assertTrue(claimed.get().getStartedAt().isPresent());
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
    assertTrue(updated.getCompletedAt().isPresent());
    assertTrue(updated.getWorkerId().isEmpty());
  }

  @Test
  void fail_withRetry_resetsToPending() {
    TaskRecord task = TaskRecord.create("test", Map.of(), 3);
    repo.save(task);
    repo.claimNextPending("test", "worker-1");

    repo.fail(task.getId(), "transient error", true);

    TaskRecord updated = repo.findById(task.getId()).get();
    assertEquals(TaskStatus.PENDING, updated.getStatus());
    assertEquals("transient error", updated.getError().orElseThrow());
    assertTrue(updated.getWorkerId().isEmpty());
  }

  @Test
  void fail_retriesExhausted_marksFailed() {
    TaskRecord task = TaskRecord.create("test", Map.of(), 1);
    repo.save(task);
    repo.claimNextPending("test", "worker-1"); // attempts = 1, maxAttempts = 1

    repo.fail(task.getId(), "final error", true);

    TaskRecord updated = repo.findById(task.getId()).get();
    assertEquals(TaskStatus.FAILED, updated.getStatus());
    assertEquals("final error", updated.getError().orElseThrow());
    assertTrue(updated.getCompletedAt().isPresent());
  }

  @Test
  void failPermanently_marksFailed() {
    TaskRecord task = TaskRecord.create("test", Map.of(), 5);
    repo.save(task);
    repo.claimNextPending("test", "worker-1");

    repo.failPermanently(task.getId(), "non-retryable error");

    TaskRecord updated = repo.findById(task.getId()).get();
    assertEquals(TaskStatus.FAILED, updated.getStatus());
    assertEquals("non-retryable error", updated.getError().orElseThrow());
    assertTrue(updated.getCompletedAt().isPresent());
    assertTrue(updated.getWorkerId().isEmpty());
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
  void resetProcessingTasks_resetsToPending() {
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
    assertTrue(repo.findById(processing1.getId()).get().getWorkerId().isEmpty());
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

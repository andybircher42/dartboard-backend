package com.dartboardbackend.task;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistence interface for task queue operations. */
public interface TaskRepository {

  /**
   * Persists a task record.
   *
   * @param task the task record to persist
   * @return the persisted task record
   */
  TaskRecord save(TaskRecord task);

  /**
   * Atomically claims the oldest pending task of the given type for the given worker.
   *
   * @param taskType the type of task to claim
   * @param workerId the identifier of the worker claiming the task
   * @return the claimed task, or empty if no pending tasks of that type exist
   */
  Optional<TaskRecord> claimNextPending(String taskType, String workerId);

  /**
   * Marks a task as completed with a result count.
   *
   * @param taskId the ID of the task to complete
   * @param resultCount the number of results produced
   */
  void complete(String taskId, int resultCount);

  /**
   * Marks a task as failed. If {@code shouldRetry} is {@code true} and attempts remain, the task is
   * requeued as {@link TaskStatus#PENDING}.
   *
   * @param taskId the ID of the task that failed
   * @param error a description of the failure
   * @param shouldRetry whether the task should be retried if attempts remain
   */
  void fail(String taskId, String error, boolean shouldRetry);

  /**
   * Marks a task as permanently failed with no retry.
   *
   * @param taskId the ID of the task that failed
   * @param error a description of the failure
   */
  void failPermanently(String taskId, String error);

  /**
   * Looks up a task by its ID.
   *
   * @param taskId the task ID to search for
   * @return the matching task, or empty if not found
   */
  Optional<TaskRecord> findById(String taskId);

  /**
   * Returns up to {@code limit} pending tasks of the given type, oldest first.
   *
   * @param taskType the type of tasks to find
   * @param limit the maximum number of tasks to return
   * @return a list of pending tasks ordered by creation time
   */
  List<TaskRecord> findPending(String taskType, int limit);

  /**
   * Queries tasks by optional status and type filters.
   *
   * @param status the status to filter by, or {@code null} for all statuses
   * @param taskType the type to filter by, or {@code null} for all types
   * @param limit the maximum number of tasks to return
   * @return a list of matching tasks ordered by creation time
   */
  List<TaskRecord> findByFilters(TaskStatus status, String taskType, int limit);

  /**
   * Resets all {@link TaskStatus#PROCESSING} tasks back to {@link TaskStatus#PENDING}. Used during
   * recovery after an unclean shutdown.
   *
   * @return the number of tasks that were reset
   */
  int resetProcessingTasks();

  /**
   * Returns a map of status to count, plus a {@code "total"} entry.
   *
   * @return queue count statistics
   */
  Map<String, Object> getQueueCounts();
}

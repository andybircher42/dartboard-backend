package com.bookishbroccoli.task;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TaskRepository {

	TaskRecord save(TaskRecord task);

	Optional<TaskRecord> claimNextPending(String taskType, String workerId);

	void complete(String taskId, int resultCount);

	void fail(String taskId, String error, boolean shouldRetry);

	void failPermanently(String taskId, String error);

	Optional<TaskRecord> findById(String taskId);

	List<TaskRecord> findPending(String taskType, int limit);

	List<TaskRecord> findByFilters(TaskStatus status, String taskType, int limit);

	int resetProcessingTasks();

	Map<String, Object> getQueueCounts();
}

package com.bookishbroccoli.task;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

	private final ConcurrentHashMap<String, TaskRecord> tasks = new ConcurrentHashMap<>();

	@Override
	public TaskRecord save(TaskRecord task) {
		tasks.put(task.getId(), task);
		return task;
	}

	@Override
	public synchronized Optional<TaskRecord> claimNextPending(String taskType, String workerId) {
		return tasks.values().stream()
				.filter(t -> t.getStatus() == TaskStatus.PENDING)
				.filter(t -> t.getTaskType().equals(taskType))
				.min(Comparator.comparing(TaskRecord::getCreatedAt))
				.map(task -> {
					task.setStatus(TaskStatus.PROCESSING);
					task.setWorkerId(workerId);
					task.setStartedAt(Instant.now());
					task.setAttempts(task.getAttempts() + 1);
					return task;
				});
	}

	@Override
	public void complete(String taskId, int resultCount) {
		Optional.ofNullable(tasks.get(taskId)).ifPresent(task -> {
			task.setStatus(TaskStatus.COMPLETED);
			task.setCompletedAt(Instant.now());
			task.setResultCount(resultCount);
			task.setWorkerId(null);
		});
	}

	@Override
	public void fail(String taskId, String error, boolean shouldRetry) {
		Optional.ofNullable(tasks.get(taskId)).ifPresent(task -> {
			task.setError(error);
			task.setWorkerId(null);
			if (shouldRetry && task.getAttempts() < task.getMaxAttempts()) {
				task.setStatus(TaskStatus.PENDING);
			} else {
				task.setStatus(TaskStatus.FAILED);
				task.setCompletedAt(Instant.now());
			}
		});
	}

	@Override
	public void failPermanently(String taskId, String error) {
		Optional.ofNullable(tasks.get(taskId)).ifPresent(task -> {
			task.setStatus(TaskStatus.FAILED);
			task.setError(error);
			task.setCompletedAt(Instant.now());
			task.setWorkerId(null);
		});
	}

	@Override
	public Optional<TaskRecord> findById(String taskId) {
		return Optional.ofNullable(tasks.get(taskId));
	}

	@Override
	public List<TaskRecord> findPending(String taskType, int limit) {
		return tasks.values().stream()
				.filter(t -> t.getStatus() == TaskStatus.PENDING)
				.filter(t -> t.getTaskType().equals(taskType))
				.sorted(Comparator.comparing(TaskRecord::getCreatedAt))
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	public List<TaskRecord> findByFilters(TaskStatus status, String taskType, int limit) {
		return tasks.values().stream()
				.filter(t -> status == null || t.getStatus() == status)
				.filter(t -> taskType == null || t.getTaskType().equals(taskType))
				.sorted(Comparator.comparing(TaskRecord::getCreatedAt))
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	public int resetProcessingTasks() {
		int count = 0;
		for (TaskRecord task : tasks.values()) {
			if (task.getStatus() == TaskStatus.PROCESSING) {
				task.setStatus(TaskStatus.PENDING);
				task.setWorkerId(null);
				count++;
			}
		}
		return count;
	}

	@Override
	public Map<String, Object> getQueueCounts() {
		Map<String, Object> counts = new LinkedHashMap<>();
		for (TaskStatus status : TaskStatus.values()) {
			long count = tasks.values().stream()
					.filter(t -> t.getStatus() == status)
					.count();
			counts.put(status.value(), count);
		}
		counts.put("total", (long) tasks.size());
		return counts;
	}

	public void clear() {
		tasks.clear();
	}
}

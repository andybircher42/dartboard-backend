package com.bookishbroccoli.task;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TaskRecord {

	private String id;
	private String taskType;
	private Map<String, Object> payload;
	private TaskStatus status;
	private Instant createdAt;
	private Instant startedAt;
	private Instant completedAt;
	private int attempts;
	private int maxAttempts;
	private String error;
	private String workerId;
	private int resultCount;

	public TaskRecord() {
	}

	public static TaskRecord create(String taskType, Map<String, Object> payload, int maxAttempts) {
		TaskRecord record = new TaskRecord();
		record.id = UUID.randomUUID().toString();
		record.taskType = taskType;
		record.payload = Optional.ofNullable(payload).map(LinkedHashMap::new).orElseGet(LinkedHashMap::new);
		record.status = TaskStatus.PENDING;
		record.createdAt = Instant.now();
		record.attempts = 0;
		record.maxAttempts = maxAttempts;
		return record;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", id);
		map.put("taskType", taskType);
		map.put("payload", payload);
		map.put("status", Optional.ofNullable(status).map(TaskStatus::value).orElse(null));
		map.put("createdAt", Optional.ofNullable(createdAt).map(Instant::toString).orElse(null));
		map.put("startedAt", getStartedAt().map(Instant::toString).orElse(null));
		map.put("completedAt", getCompletedAt().map(Instant::toString).orElse(null));
		map.put("attempts", attempts);
		map.put("maxAttempts", maxAttempts);
		map.put("error", getError().orElse(null));
		map.put("workerId", getWorkerId().orElse(null));
		map.put("resultCount", resultCount);
		return map;
	}

	// Getters and setters

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public void setPayload(Map<String, Object> payload) {
		this.payload = payload;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Optional<Instant> getStartedAt() {
		return Optional.ofNullable(startedAt);
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Optional<Instant> getCompletedAt() {
		return Optional.ofNullable(completedAt);
	}

	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public Optional<String> getError() {
		return Optional.ofNullable(error);
	}

	public void setError(String error) {
		this.error = error;
	}

	public Optional<String> getWorkerId() {
		return Optional.ofNullable(workerId);
	}

	public void setWorkerId(String workerId) {
		this.workerId = workerId;
	}

	public int getResultCount() {
		return resultCount;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}
}

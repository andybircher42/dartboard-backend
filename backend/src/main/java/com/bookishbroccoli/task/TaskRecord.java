package com.bookishbroccoli.task;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
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
		record.payload = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
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
		map.put("status", status != null ? status.value() : null);
		map.put("createdAt", createdAt != null ? createdAt.toString() : null);
		map.put("startedAt", startedAt != null ? startedAt.toString() : null);
		map.put("completedAt", completedAt != null ? completedAt.toString() : null);
		map.put("attempts", attempts);
		map.put("maxAttempts", maxAttempts);
		map.put("error", error);
		map.put("workerId", workerId);
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

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
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

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getWorkerId() {
		return workerId;
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

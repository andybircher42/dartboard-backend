package com.dartboardbackend.task;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Mutable data model representing a queued task with its lifecycle metadata. */
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

  public TaskRecord() {}

  /**
   * Factory method that builds a new {@link TaskStatus#PENDING} task with a random UUID.
   *
   * @param taskType the type identifier for this task
   * @param payload the task payload data, may be {@code null}
   * @param maxAttempts the maximum number of processing attempts before permanent failure
   * @return a new task record in the {@link TaskStatus#PENDING} state
   */
  public static TaskRecord create(String taskType, Map<String, Object> payload, int maxAttempts) {
    TaskRecord record = new TaskRecord();
    record.id = UUID.randomUUID().toString();
    record.taskType = taskType;
    record.payload =
        Optional.ofNullable(payload).map(LinkedHashMap::new).orElseGet(LinkedHashMap::new);
    record.status = TaskStatus.PENDING;
    record.createdAt = Instant.now();
    record.attempts = 0;
    record.maxAttempts = maxAttempts;
    return record;
  }

  /**
   * Serializes this record to an ordered map suitable for JSON responses.
   *
   * @return a {@link LinkedHashMap} containing all task fields
   */
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

  /**
   * Returns the unique task identifier.
   *
   * @return the task ID
   */
  public String getId() {
    return id;
  }

  /** Sets the unique task identifier. */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the task type identifier.
   *
   * @return the task type
   */
  public String getTaskType() {
    return taskType;
  }

  /** Sets the task type identifier. */
  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  /**
   * Returns the task payload data.
   *
   * @return the payload map
   */
  public Map<String, Object> getPayload() {
    return payload;
  }

  /** Sets the task payload data. */
  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }

  /**
   * Returns the current lifecycle status of this task.
   *
   * @return the task status
   */
  public TaskStatus getStatus() {
    return status;
  }

  /** Sets the current lifecycle status of this task. */
  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  /**
   * Returns the instant this task was created.
   *
   * @return the creation timestamp
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Sets the instant this task was created. */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Returns the instant processing started. Returns empty when processing has not yet begun.
   *
   * @return an {@link Optional} containing the start timestamp, or empty if not yet set
   */
  public Optional<Instant> getStartedAt() {
    return Optional.ofNullable(startedAt);
  }

  /** Sets the instant processing started. */
  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  /**
   * Returns the instant the task completed or failed. Returns empty when the value is not yet set.
   *
   * @return an {@link Optional} containing the completion timestamp, or empty if not yet set
   */
  public Optional<Instant> getCompletedAt() {
    return Optional.ofNullable(completedAt);
  }

  /** Sets the instant the task completed or failed. */
  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  /**
   * Returns the number of processing attempts made so far.
   *
   * @return the attempt count
   */
  public int getAttempts() {
    return attempts;
  }

  /** Sets the number of processing attempts made so far. */
  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  /**
   * Returns the maximum number of processing attempts allowed.
   *
   * @return the maximum attempt count
   */
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /** Sets the maximum number of processing attempts allowed. */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * Returns the error message from the most recent failure. Returns empty when the value is not yet
   * set.
   *
   * @return an {@link Optional} containing the error message, or empty if not yet set
   */
  public Optional<String> getError() {
    return Optional.ofNullable(error);
  }

  /** Sets the error message from the most recent failure. */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * Returns the ID of the worker currently processing this task. Returns empty when the value is
   * not yet set.
   *
   * @return an {@link Optional} containing the worker ID, or empty if not yet set
   */
  public Optional<String> getWorkerId() {
    return Optional.ofNullable(workerId);
  }

  /** Sets the ID of the worker processing this task. */
  public void setWorkerId(String workerId) {
    this.workerId = workerId;
  }

  /**
   * Returns the number of results produced by this task.
   *
   * @return the result count
   */
  public int getResultCount() {
    return resultCount;
  }

  /** Sets the number of results produced by this task. */
  public void setResultCount(int resultCount) {
    this.resultCount = resultCount;
  }
}

package com.dartboardbackend.job;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mutable data model representing a long-running job with its execution metadata.
 *
 * <p>Each record captures the job's identity, current status, timing information, result count, and
 * any associated metadata. Use the {@link #create(String, String, Map)} factory method to construct
 * new instances with sensible defaults.
 */
public class JobRecord {

  private String id;
  private String jobType;
  private String externalId;
  private JobStatus status;
  private Instant startedAt;
  private Instant finishedAt;
  private int resultCount;
  private String error;
  private Map<String, Object> metadata;

  public JobRecord() {}

  /**
   * Factory method that builds a new {@link JobStatus#RUNNING RUNNING} job with a random UUID.
   *
   * @param jobType the logical type of the job (e.g. {@code "actor-sync"})
   * @param externalId an external identifier associated with this job run
   * @param metadata optional key-value metadata to attach; may be {@code null}
   * @return a new {@link JobRecord} in the {@link JobStatus#RUNNING} state
   */
  public static JobRecord create(String jobType, String externalId, Map<String, Object> metadata) {
    JobRecord record = new JobRecord();
    record.id = UUID.randomUUID().toString();
    record.jobType = jobType;
    record.externalId = externalId;
    record.status = JobStatus.RUNNING;
    record.startedAt = Instant.now();
    record.metadata =
        Optional.ofNullable(metadata).map(LinkedHashMap::new).orElseGet(LinkedHashMap::new);
    return record;
  }

  /**
   * Serializes this record to an ordered map suitable for JSON responses.
   *
   * <p>The map preserves insertion order and includes all fields, with {@code null} values for
   * fields that are not set.
   *
   * @return an ordered {@link Map} representation of this record
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", id);
    map.put("jobType", jobType);
    map.put("externalId", externalId);
    map.put("status", Optional.ofNullable(status).map(JobStatus::value).orElse(null));
    map.put("startedAt", Optional.ofNullable(startedAt).map(Instant::toString).orElse(null));
    map.put("finishedAt", getFinishedAt().map(Instant::toString).orElse(null));
    map.put("resultCount", resultCount);
    map.put("error", getError().orElse(null));
    map.put("metadata", getMetadata().orElse(null));
    return map;
  }

  // Getters and setters

  /**
   * Returns the unique identifier for this job.
   *
   * @return the job ID
   */
  public String getId() {
    return id;
  }

  /** Sets the unique identifier for this job. */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the logical type of this job.
   *
   * @return the job type
   */
  public String getJobType() {
    return jobType;
  }

  /** Sets the logical type of this job. */
  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  /**
   * Returns the external identifier associated with this job.
   *
   * @return the external ID
   */
  public String getExternalId() {
    return externalId;
  }

  /** Sets the external identifier associated with this job. */
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  /**
   * Returns the current status of this job.
   *
   * @return the job status
   */
  public JobStatus getStatus() {
    return status;
  }

  /** Sets the current status of this job. */
  public void setStatus(JobStatus status) {
    this.status = status;
  }

  /**
   * Returns the instant when this job started.
   *
   * @return the start time
   */
  public Instant getStartedAt() {
    return startedAt;
  }

  /** Sets the instant when this job started. */
  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  /**
   * Returns the instant when this job finished, if available.
   *
   * @return an {@link Optional} containing the finish time, or empty if the job has not finished
   */
  public Optional<Instant> getFinishedAt() {
    return Optional.ofNullable(finishedAt);
  }

  /** Sets the instant when this job finished. */
  public void setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
  }

  /**
   * Returns the number of results produced by this job.
   *
   * @return the result count
   */
  public int getResultCount() {
    return resultCount;
  }

  /** Sets the number of results produced by this job. */
  public void setResultCount(int resultCount) {
    this.resultCount = resultCount;
  }

  /**
   * Returns the error message for a failed job, if available.
   *
   * @return an {@link Optional} containing the error message, or empty if no error is set
   */
  public Optional<String> getError() {
    return Optional.ofNullable(error);
  }

  /** Sets the error message for a failed job. */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * Returns the metadata map associated with this job, if available.
   *
   * @return an {@link Optional} containing the metadata map, or empty if no metadata is set
   */
  public Optional<Map<String, Object>> getMetadata() {
    return Optional.ofNullable(metadata);
  }

  /** Sets the metadata map associated with this job. */
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }
}

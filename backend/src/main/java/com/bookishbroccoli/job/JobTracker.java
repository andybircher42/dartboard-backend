package com.bookishbroccoli.job;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for tracking the lifecycle of long-running jobs.
 *
 * <p>Implementations are responsible for persisting job records and providing query methods for
 * monitoring and staleness detection.
 */
public interface JobTracker {

  /**
   * Creates and persists a new {@link JobStatus#RUNNING RUNNING} job.
   *
   * @param jobType the logical type of the job (e.g. {@code "actor-sync"})
   * @param externalId an external identifier associated with this job run
   * @param metadata optional key-value metadata to attach; may be {@code null}
   * @return the newly created {@link JobRecord}
   */
  JobRecord start(String jobType, String externalId, Map<String, Object> metadata);

  /**
   * Marks a job as {@link JobStatus#SUCCEEDED} with the given result count.
   *
   * @param jobId the ID of the job to complete
   * @param resultCount the number of results produced by the job
   */
  void complete(String jobId, int resultCount);

  /**
   * Marks a job as {@link JobStatus#FAILED} with an error message.
   *
   * @param jobId the ID of the job to fail
   * @param error a human-readable description of the failure
   */
  void fail(String jobId, String error);

  /**
   * Looks up a job by its unique identifier.
   *
   * @param jobId the ID of the job to find
   * @return an {@link Optional} containing the job record, or empty if not found
   */
  Optional<JobRecord> findById(String jobId);

  /**
   * Returns the most recent successful job of the given type.
   *
   * @param jobType the logical type of the job
   * @return an {@link Optional} containing the last successful job, or empty if none exists
   */
  Optional<JobRecord> getLastSuccessful(String jobType);

  /**
   * Returns up to {@code limit} jobs of the given type, ordered most recent first.
   *
   * @param jobType the logical type of the job
   * @param limit the maximum number of records to return
   * @return a list of matching job records, most recent first
   */
  List<JobRecord> findByType(String jobType, int limit);

  /**
   * Returns up to {@code limit} jobs with the given status, ordered most recent first.
   *
   * @param status the status to filter by
   * @param limit the maximum number of records to return
   * @return a list of matching job records, most recent first
   */
  List<JobRecord> findByStatus(JobStatus status, int limit);

  /**
   * Returns {@code true} if the given job type has no recent successful completion within {@code
   * maxAge}, and no run is currently in progress.
   *
   * @param jobType the logical type of the job
   * @param maxAge the maximum acceptable age of a successful completion
   * @return {@code true} if the job type is considered stale
   */
  boolean isStale(String jobType, Duration maxAge);
}

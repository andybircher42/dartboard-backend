package com.bookishbroccoli.job;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link JobTracker} backed by {@link ConcurrentHashMap}.
 *
 * <p>Caches the last successful job per type for fast staleness checks. Suitable for development
 * and testing; data is not persisted across application restarts.
 */
@Repository
public class InMemoryJobTracker implements JobTracker {

  private final ConcurrentHashMap<String, JobRecord> jobs = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, JobRecord> lastSuccessful = new ConcurrentHashMap<>();

  /** {@inheritDoc} */
  @Override
  public JobRecord start(String jobType, String externalId, Map<String, Object> metadata) {
    JobRecord record = JobRecord.create(jobType, externalId, metadata);
    jobs.put(record.getId(), record);
    return record;
  }

  /** {@inheritDoc} */
  @Override
  public void complete(String jobId, int resultCount) {
    Optional.ofNullable(jobs.get(jobId))
        .ifPresent(
            job -> {
              job.setStatus(JobStatus.SUCCEEDED);
              job.setFinishedAt(Instant.now());
              job.setResultCount(resultCount);
              lastSuccessful.put(job.getJobType(), job);
            });
  }

  /** {@inheritDoc} */
  @Override
  public void fail(String jobId, String error) {
    Optional.ofNullable(jobs.get(jobId))
        .ifPresent(
            job -> {
              job.setStatus(JobStatus.FAILED);
              job.setFinishedAt(Instant.now());
              job.setError(error);
            });
  }

  /** {@inheritDoc} */
  @Override
  public Optional<JobRecord> findById(String jobId) {
    return Optional.ofNullable(jobs.get(jobId));
  }

  /** {@inheritDoc} */
  @Override
  public Optional<JobRecord> getLastSuccessful(String jobType) {
    return Optional.ofNullable(lastSuccessful.get(jobType));
  }

  /** {@inheritDoc} */
  @Override
  public List<JobRecord> findByType(String jobType, int limit) {
    return jobs.values().stream()
        .filter(j -> j.getJobType().equals(jobType))
        .sorted(Comparator.comparing(JobRecord::getStartedAt).reversed())
        .limit(limit)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<JobRecord> findByStatus(JobStatus status, int limit) {
    return jobs.values().stream()
        .filter(j -> j.getStatus() == status)
        .sorted(Comparator.comparing(JobRecord::getStartedAt).reversed())
        .limit(limit)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStale(String jobType, Duration maxAge) {
    return Optional.ofNullable(lastSuccessful.get(jobType))
        .flatMap(last -> last.getFinishedAt().map(ft -> ft.plus(maxAge).isBefore(Instant.now())))
        .orElseGet(
            () -> {
              // No successful run — check if one is currently running
              boolean hasRunning =
                  jobs.values().stream()
                      .anyMatch(
                          j ->
                              j.getJobType().equals(jobType) && j.getStatus() == JobStatus.RUNNING);
              return !hasRunning;
            });
  }

  /** Removes all jobs and cached last-successful entries. */
  public void clear() {
    jobs.clear();
    lastSuccessful.clear();
  }
}

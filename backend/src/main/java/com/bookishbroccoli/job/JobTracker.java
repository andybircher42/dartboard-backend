package com.bookishbroccoli.job;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JobTracker {

	JobRecord start(String jobType, String externalId, Map<String, Object> metadata);

	void complete(String jobId, int resultCount);

	void fail(String jobId, String error);

	Optional<JobRecord> findById(String jobId);

	Optional<JobRecord> getLastSuccessful(String jobType);

	List<JobRecord> findByType(String jobType, int limit);

	List<JobRecord> findByStatus(JobStatus status, int limit);

	boolean isStale(String jobType, Duration maxAge);
}

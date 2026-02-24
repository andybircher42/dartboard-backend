package com.bookishbroccoli.job;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

	public JobRecord() {
	}

	public static JobRecord create(String jobType, String externalId, Map<String, Object> metadata) {
		JobRecord record = new JobRecord();
		record.id = UUID.randomUUID().toString();
		record.jobType = jobType;
		record.externalId = externalId;
		record.status = JobStatus.RUNNING;
		record.startedAt = Instant.now();
		record.metadata = Optional.ofNullable(metadata).map(LinkedHashMap::new).orElseGet(LinkedHashMap::new);
		return record;
	}

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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Optional<Instant> getFinishedAt() {
		return Optional.ofNullable(finishedAt);
	}

	public void setFinishedAt(Instant finishedAt) {
		this.finishedAt = finishedAt;
	}

	public int getResultCount() {
		return resultCount;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	public Optional<String> getError() {
		return Optional.ofNullable(error);
	}

	public void setError(String error) {
		this.error = error;
	}

	public Optional<Map<String, Object>> getMetadata() {
		return Optional.ofNullable(metadata);
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}
}

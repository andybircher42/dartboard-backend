package com.bookishbroccoli.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryJobTrackerTest {

	private InMemoryJobTracker tracker;

	@BeforeEach
	void setUp() {
		tracker = new InMemoryJobTracker();
	}

	@Test
	void start_createsRecord() {
		JobRecord job = tracker.start("data_sync", "ext-123", Map.of("source", "api"));

		assertNotNull(job.getId());
		assertEquals("data_sync", job.getJobType());
		assertEquals("ext-123", job.getExternalId());
		assertEquals(JobStatus.RUNNING, job.getStatus());
		assertNotNull(job.getStartedAt());
		assertEquals("api", job.getMetadata().get("source"));
	}

	@Test
	void start_nullMetadata() {
		JobRecord job = tracker.start("test", null, null);

		assertNotNull(job.getMetadata());
		assertTrue(job.getMetadata().isEmpty());
	}

	@Test
	void complete_setsStatusAndResultCount() {
		JobRecord job = tracker.start("test", null, null);

		tracker.complete(job.getId(), 42);

		JobRecord updated = tracker.findById(job.getId()).orElseThrow();
		assertEquals(JobStatus.SUCCEEDED, updated.getStatus());
		assertEquals(42, updated.getResultCount());
		assertNotNull(updated.getFinishedAt());
	}

	@Test
	void fail_setsStatusAndError() {
		JobRecord job = tracker.start("test", null, null);

		tracker.fail(job.getId(), "connection timeout");

		JobRecord updated = tracker.findById(job.getId()).orElseThrow();
		assertEquals(JobStatus.FAILED, updated.getStatus());
		assertEquals("connection timeout", updated.getError());
		assertNotNull(updated.getFinishedAt());
	}

	@Test
	void findById_found() {
		JobRecord job = tracker.start("test", null, null);

		assertTrue(tracker.findById(job.getId()).isPresent());
	}

	@Test
	void findById_notFound() {
		assertTrue(tracker.findById("nonexistent").isEmpty());
	}

	@Test
	void getLastSuccessful_updatedOnComplete() {
		JobRecord job1 = tracker.start("data_sync", null, null);
		tracker.complete(job1.getId(), 10);

		JobRecord job2 = tracker.start("data_sync", null, null);
		tracker.complete(job2.getId(), 20);

		JobRecord last = tracker.getLastSuccessful("data_sync").orElseThrow();
		assertEquals(job2.getId(), last.getId());
		assertEquals(20, last.getResultCount());
	}

	@Test
	void getLastSuccessful_notUpdatedOnFail() {
		JobRecord job1 = tracker.start("data_sync", null, null);
		tracker.complete(job1.getId(), 10);

		JobRecord job2 = tracker.start("data_sync", null, null);
		tracker.fail(job2.getId(), "error");

		JobRecord last = tracker.getLastSuccessful("data_sync").orElseThrow();
		assertEquals(job1.getId(), last.getId());
	}

	@Test
	void getLastSuccessful_emptyIfNeverSucceeded() {
		JobRecord job = tracker.start("data_sync", null, null);
		tracker.fail(job.getId(), "error");

		assertTrue(tracker.getLastSuccessful("data_sync").isEmpty());
	}

	@Test
	void findByType_ordering() {
		JobRecord job1 = tracker.start("data_sync", null, null);
		JobRecord job2 = tracker.start("data_sync", null, null);
		tracker.start("other_type", null, null);

		List<JobRecord> results = tracker.findByType("data_sync", 10);

		assertEquals(2, results.size());
		// Most recent first
		assertEquals(job2.getId(), results.get(0).getId());
		assertEquals(job1.getId(), results.get(1).getId());
	}

	@Test
	void findByType_respectsLimit() {
		for (int i = 0; i < 10; i++) {
			tracker.start("data_sync", null, null);
		}

		List<JobRecord> results = tracker.findByType("data_sync", 3);

		assertEquals(3, results.size());
	}

	@Test
	void findByStatus_filters() {
		JobRecord running = tracker.start("test", null, null);
		JobRecord succeeded = tracker.start("test", null, null);
		tracker.complete(succeeded.getId(), 5);

		List<JobRecord> runningJobs = tracker.findByStatus(JobStatus.RUNNING, 10);
		List<JobRecord> succeededJobs = tracker.findByStatus(JobStatus.SUCCEEDED, 10);

		assertEquals(1, runningJobs.size());
		assertEquals(running.getId(), runningJobs.get(0).getId());
		assertEquals(1, succeededJobs.size());
		assertEquals(succeeded.getId(), succeededJobs.get(0).getId());
	}

	@Test
	void isStale_noRuns_stale() {
		assertTrue(tracker.isStale("data_sync", Duration.ofHours(1)));
	}

	@Test
	void isStale_recentSuccess_notStale() {
		JobRecord job = tracker.start("data_sync", null, null);
		tracker.complete(job.getId(), 10);

		assertFalse(tracker.isStale("data_sync", Duration.ofHours(1)));
	}

	@Test
	void isStale_oldSuccess_stale() {
		JobRecord job = tracker.start("data_sync", null, null);
		tracker.complete(job.getId(), 10);
		// Manually backdate finishedAt
		job.setFinishedAt(Instant.now().minus(Duration.ofHours(2)));

		assertTrue(tracker.isStale("data_sync", Duration.ofHours(1)));
	}

	@Test
	void isStale_runningFallback_notStale() {
		tracker.start("data_sync", null, null); // status = RUNNING

		assertFalse(tracker.isStale("data_sync", Duration.ofHours(1)));
	}

	@Test
	void isStale_failedOnly_stale() {
		JobRecord job = tracker.start("data_sync", null, null);
		tracker.fail(job.getId(), "error");

		assertTrue(tracker.isStale("data_sync", Duration.ofHours(1)));
	}

	@Test
	void clear_removesAll() {
		JobRecord job = tracker.start("data_sync", null, null);
		tracker.complete(job.getId(), 10);

		tracker.clear();

		assertTrue(tracker.findById(job.getId()).isEmpty());
		assertTrue(tracker.getLastSuccessful("data_sync").isEmpty());
	}
}

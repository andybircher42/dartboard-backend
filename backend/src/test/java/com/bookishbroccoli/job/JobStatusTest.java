package com.bookishbroccoli.job;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JobStatusTest {

  @Test
  void isTerminal_succeededAndFailed() {
    assertTrue(JobStatus.SUCCEEDED.isTerminal());
    assertTrue(JobStatus.FAILED.isTerminal());
    assertFalse(JobStatus.PENDING.isTerminal());
    assertFalse(JobStatus.RUNNING.isTerminal());
  }

  @Test
  void isRunning_onlyRunning() {
    assertTrue(JobStatus.RUNNING.isRunning());
    assertFalse(JobStatus.PENDING.isRunning());
    assertFalse(JobStatus.SUCCEEDED.isRunning());
    assertFalse(JobStatus.FAILED.isRunning());
  }

  @Test
  void fromString_byValue() {
    assertEquals(JobStatus.PENDING, JobStatus.fromString("pending"));
    assertEquals(JobStatus.RUNNING, JobStatus.fromString("running"));
    assertEquals(JobStatus.SUCCEEDED, JobStatus.fromString("succeeded"));
    assertEquals(JobStatus.FAILED, JobStatus.fromString("failed"));
  }

  @Test
  void fromString_byName() {
    assertEquals(JobStatus.RUNNING, JobStatus.fromString("RUNNING"));
    assertEquals(JobStatus.SUCCEEDED, JobStatus.fromString("SUCCEEDED"));
  }

  @Test
  void fromString_caseInsensitive() {
    assertEquals(JobStatus.RUNNING, JobStatus.fromString("Running"));
    assertEquals(JobStatus.FAILED, JobStatus.fromString("FAILED"));
  }

  @Test
  void fromString_null_returnsPending() {
    assertEquals(JobStatus.PENDING, JobStatus.fromString(null));
  }

  @Test
  void fromString_unknown_returnsPending() {
    assertEquals(JobStatus.PENDING, JobStatus.fromString("unknown_status"));
  }

  @Test
  void value_returnsLowercase() {
    assertEquals("pending", JobStatus.PENDING.value());
    assertEquals("running", JobStatus.RUNNING.value());
    assertEquals("succeeded", JobStatus.SUCCEEDED.value());
    assertEquals("failed", JobStatus.FAILED.value());
  }

  @Test
  void toString_returnsValue() {
    assertEquals("pending", JobStatus.PENDING.toString());
    assertEquals("running", JobStatus.RUNNING.toString());
  }
}

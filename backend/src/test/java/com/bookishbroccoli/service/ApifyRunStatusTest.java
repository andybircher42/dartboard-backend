package com.bookishbroccoli.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApifyRunStatusTest {

  @Test
  void fromString_knownStatuses() {
    assertEquals(ApifyRunStatus.RUNNING, ApifyRunStatus.fromString("RUNNING"));
    assertEquals(ApifyRunStatus.SUCCEEDED, ApifyRunStatus.fromString("SUCCEEDED"));
    assertEquals(ApifyRunStatus.FAILED, ApifyRunStatus.fromString("FAILED"));
    assertEquals(ApifyRunStatus.ABORTED, ApifyRunStatus.fromString("ABORTED"));
    assertEquals(ApifyRunStatus.TIMED_OUT, ApifyRunStatus.fromString("TIMED-OUT"));
    assertEquals(ApifyRunStatus.READY, ApifyRunStatus.fromString("READY"));
  }

  @Test
  void fromString_unknownReturnsUnknown() {
    assertEquals(ApifyRunStatus.UNKNOWN, ApifyRunStatus.fromString("FOOBAR"));
    assertEquals(ApifyRunStatus.UNKNOWN, ApifyRunStatus.fromString(""));
  }

  @Test
  void terminalStatuses() {
    assertTrue(ApifyRunStatus.SUCCEEDED.isTerminal());
    assertTrue(ApifyRunStatus.FAILED.isTerminal());
    assertTrue(ApifyRunStatus.ABORTED.isTerminal());
    assertTrue(ApifyRunStatus.TIMED_OUT.isTerminal());
  }

  @Test
  void nonTerminalStatuses() {
    assertFalse(ApifyRunStatus.RUNNING.isTerminal());
    assertFalse(ApifyRunStatus.READY.isTerminal());
    assertFalse(ApifyRunStatus.UNKNOWN.isTerminal());
  }

  @Test
  void valueReturnsString() {
    assertEquals("SUCCEEDED", ApifyRunStatus.SUCCEEDED.value());
    assertEquals("TIMED-OUT", ApifyRunStatus.TIMED_OUT.value());
  }

  @Test
  void toStringReturnsValue() {
    assertEquals("RUNNING", ApifyRunStatus.RUNNING.toString());
  }
}

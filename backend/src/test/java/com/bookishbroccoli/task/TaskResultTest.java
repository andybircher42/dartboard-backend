package com.bookishbroccoli.task;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TaskResultTest {

  @Test
  void successWithCount() {
    TaskResult result = TaskResult.success(42);

    assertTrue(result.succeeded());
    assertEquals(42, result.resultCount());
    assertTrue(result.error().isEmpty());
  }

  @Test
  void successNoArgs() {
    TaskResult result = TaskResult.success();

    assertTrue(result.succeeded());
    assertEquals(0, result.resultCount());
    assertTrue(result.error().isEmpty());
  }

  @Test
  void failure() {
    TaskResult result = TaskResult.failure("something broke");

    assertFalse(result.succeeded());
    assertEquals(0, result.resultCount());
    assertEquals("something broke", result.error().orElseThrow());
  }
}

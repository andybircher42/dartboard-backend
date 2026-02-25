package com.bookishbroccoli.retry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RetryExecutorTest {

  private RetryExecutor executor;
  private List<Long> sleepCalls;

  @BeforeEach
  void setUp() {
    sleepCalls = new ArrayList<>();
    executor = new RetryExecutor(ms -> sleepCalls.add(ms));
  }

  @Test
  void succeedsFirstAttempt() {
    RetryPolicy policy = RetryPolicy.of("test", 3);

    RetryResult<String> result = executor.execute(policy, () -> "hello");

    assertTrue(result.succeeded());
    assertEquals("hello", result.value());
    assertEquals(1, result.attempts());
    assertTrue(sleepCalls.isEmpty());
  }

  @Test
  void succeedsAfterRetry() {
    RetryPolicy policy = RetryPolicy.of("test", 3);
    AtomicInteger calls = new AtomicInteger(0);

    RetryResult<String> result =
        executor.execute(
            policy,
            () -> {
              if (calls.incrementAndGet() < 3) {
                throw new RetryableException("transient");
              }
              return "recovered";
            });

    assertTrue(result.succeeded());
    assertEquals("recovered", result.value());
    assertEquals(3, result.attempts());
    assertEquals(2, sleepCalls.size());
  }

  @Test
  void nonRetryable_failsImmediately() {
    RetryPolicy policy = RetryPolicy.of("test", 4);

    RetryResult<String> result =
        executor.execute(
            policy,
            () -> {
              throw new NonRetryableException("permanent");
            });

    assertFalse(result.succeeded());
    assertEquals(1, result.attempts());
    assertInstanceOf(NonRetryableException.class, result.lastException());
    assertTrue(sleepCalls.isEmpty());
  }

  @Test
  void retryableExhausted_returnsFailureWithCorrectAttempts() {
    RetryPolicy policy = RetryPolicy.of("test", 4);

    RetryResult<String> result =
        executor.execute(
            policy,
            () -> {
              throw new RetryableException("keep failing");
            });

    assertFalse(result.succeeded());
    assertEquals(4, result.attempts());
    assertInstanceOf(RetryableException.class, result.lastException());
    assertEquals("keep failing", result.lastException().getMessage());
  }

  @Test
  void otherException_treatedAsNonRetryable() {
    RetryPolicy policy = RetryPolicy.of("test", 3);

    RetryResult<String> result =
        executor.execute(
            policy,
            () -> {
              throw new IllegalStateException("unexpected");
            });

    assertFalse(result.succeeded());
    assertEquals(1, result.attempts());
    assertInstanceOf(IllegalStateException.class, result.lastException());
    assertTrue(sleepCalls.isEmpty());
  }

  @Test
  void retryableThenNonRetryable_failsOnNonRetryable() {
    RetryPolicy policy = RetryPolicy.of("test", 4);
    AtomicInteger calls = new AtomicInteger(0);

    RetryResult<String> result =
        executor.execute(
            policy,
            () -> {
              if (calls.incrementAndGet() <= 2) {
                throw new RetryableException("transient");
              }
              throw new NonRetryableException("permanent");
            });

    assertFalse(result.succeeded());
    assertEquals(3, result.attempts());
    assertInstanceOf(NonRetryableException.class, result.lastException());
  }

  @Test
  void interruptedDuringSleep_returnsFailure() {
    RetryExecutor interruptingExecutor =
        new RetryExecutor(
            ms -> {
              Thread.currentThread().interrupt();
              throw new RuntimeException(new InterruptedException("interrupted"));
            });
    RetryPolicy policy = RetryPolicy.of("test", 3);
    AtomicInteger calls = new AtomicInteger(0);

    RetryResult<String> result =
        interruptingExecutor.execute(
            policy,
            () -> {
              calls.incrementAndGet();
              throw new RetryableException("fail");
            });

    assertFalse(result.succeeded());
    assertTrue(Thread.interrupted()); // clears the flag
  }

  @Test
  void backoff_sleeperCalledWithCorrectValues() {
    RetryPolicy policy = RetryPolicy.of("test", 4, 10_000, 60_000);
    AtomicInteger calls = new AtomicInteger(0);

    executor.execute(
        policy,
        () -> {
          if (calls.incrementAndGet() < 4) {
            throw new RetryableException("fail");
          }
          return "ok";
        });

    assertEquals(3, sleepCalls.size());
    assertEquals(10_000, sleepCalls.get(0)); // attempt 2: 10000 * 2^0
    assertEquals(20_000, sleepCalls.get(1)); // attempt 3: 10000 * 2^1
    assertEquals(40_000, sleepCalls.get(2)); // attempt 4: 10000 * 2^2
  }

  @Test
  void singleAttemptPolicy_noRetries() {
    RetryPolicy policy = RetryPolicy.of("once", 1);

    RetryResult<String> result =
        executor.execute(
            policy,
            () -> {
              throw new RetryableException("fail");
            });

    assertFalse(result.succeeded());
    assertEquals(1, result.attempts());
    assertTrue(sleepCalls.isEmpty());
  }

  @Test
  void resultGetOrThrow_afterExecuteSuccess() {
    RetryPolicy policy = RetryPolicy.of("test", 2);

    RetryResult<Integer> result = executor.execute(policy, () -> 42);

    assertEquals(42, result.getOrThrow());
  }

  @Test
  void resultGetOrThrow_afterExecuteFailure() {
    RetryPolicy policy = RetryPolicy.of("test", 2);

    RetryResult<Integer> result =
        executor.execute(
            policy,
            () -> {
              throw new RetryableException("nope");
            });

    RuntimeException ex = assertThrows(RuntimeException.class, result::getOrThrow);
    assertTrue(ex.getMessage().contains("2 attempts exhausted"));
  }

  @Test
  void nullReturnValue_succeeds() {
    RetryPolicy policy = RetryPolicy.of("test", 2);

    RetryResult<String> result = executor.execute(policy, () -> null);

    assertTrue(result.succeeded());
    assertNull(result.value());
    assertEquals(1, result.attempts());
  }
}

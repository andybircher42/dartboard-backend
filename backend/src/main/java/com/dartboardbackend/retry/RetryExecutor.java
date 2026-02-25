package com.dartboardbackend.retry;

import java.util.concurrent.Callable;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that executes operations with exponential-backoff retry logic. Distinguishes between
 * {@link RetryableException}s (which trigger retries), {@link NonRetryableException}s (which fail
 * immediately), and other exceptions (which also fail immediately). Uses a pluggable sleeper for
 * testability.
 */
@Service
public class RetryExecutor {

  private static final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);

  private final LongConsumer sleeper;

  /** Default constructor using {@link Thread#sleep(long)} for backoff delays. */
  public RetryExecutor() {
    this(
        ms -> {
          try {
            Thread.sleep(ms);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * Constructor accepting a custom sleeper (e.g. a no-op for testing).
   *
   * @param sleeper a consumer that pauses execution for the given number of milliseconds
   */
  public RetryExecutor(LongConsumer sleeper) {
    this.sleeper = sleeper;
  }

  /**
   * Executes the operation according to the retry policy, returning a {@link RetryResult}. On each
   * attempt: if it succeeds, returns success. If a {@link NonRetryableException} is thrown, returns
   * failure immediately. If a {@link RetryableException} is thrown, waits and retries. If any other
   * exception is thrown, returns failure immediately.
   *
   * @param <T> the type of the result produced by the operation
   * @param policy the retry policy governing attempt count and backoff timing
   * @param operation the operation to execute, which may be retried on transient failures
   * @return a {@link RetryResult} capturing the outcome of the retried operation
   */
  public <T> RetryResult<T> execute(RetryPolicy policy, Callable<T> operation) {
    Exception lastRetryable = null;

    for (int attempt = 1; attempt <= policy.getMaxAttempts(); attempt++) {
      if (attempt > 1) {
        long backoffMs = policy.getBackoffMs(attempt - 1);
        logger.warn(
            "Retry attempt {}/{} for policy '{}' after {}ms backoff",
            attempt,
            policy.getMaxAttempts(),
            policy.getName(),
            backoffMs);
        try {
          sleeper.accept(backoffMs);
        } catch (RuntimeException e) {
          if (e.getCause() instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return RetryResult.failure(attempt - 1, lastRetryable != null ? lastRetryable : e);
          }
          throw e;
        }
      }

      try {
        T result = operation.call();
        return RetryResult.success(result, attempt);
      } catch (NonRetryableException e) {
        return RetryResult.failure(attempt, e);
      } catch (RetryableException e) {
        lastRetryable = e;
        logger.warn(
            "Retryable error on attempt {}/{} for policy '{}': {}",
            attempt,
            policy.getMaxAttempts(),
            policy.getName(),
            e.getMessage());
      } catch (Exception e) {
        return RetryResult.failure(attempt, e);
      }
    }

    return RetryResult.failure(policy.getMaxAttempts(), lastRetryable);
  }
}

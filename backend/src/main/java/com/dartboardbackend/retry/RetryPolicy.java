package com.dartboardbackend.retry;

import java.util.Objects;

/**
 * Configuration for retry behavior with exponential backoff. Specifies the maximum number of
 * attempts and backoff timing (initial delay doubles each attempt, capped at a maximum). Policies
 * are identified by name and equality is based on name alone.
 */
public class RetryPolicy {

  private final String name;
  private final int maxAttempts;
  private final long initialBackoffMs;
  private final long maxBackoffMs;

  private RetryPolicy(String name, int maxAttempts, long initialBackoffMs, long maxBackoffMs) {
    this.name = Objects.requireNonNull(name, "name must not be null");
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be >= 1, got " + maxAttempts);
    }
    this.maxAttempts = maxAttempts;
    this.initialBackoffMs = initialBackoffMs;
    this.maxBackoffMs = maxBackoffMs;
  }

  /**
   * Creates a policy with default backoff (10s initial, 60s max).
   *
   * @param name the name identifying this policy
   * @param maxAttempts the maximum number of attempts (must be >= 1)
   * @return a new {@code RetryPolicy} with default backoff settings
   */
  public static RetryPolicy of(String name, int maxAttempts) {
    return new RetryPolicy(name, maxAttempts, 10_000, 60_000);
  }

  /**
   * Creates a policy with custom backoff timing.
   *
   * @param name the name identifying this policy
   * @param maxAttempts the maximum number of attempts (must be >= 1)
   * @param initialBackoffMs the initial backoff delay in milliseconds
   * @param maxBackoffMs the maximum backoff delay in milliseconds
   * @return a new {@code RetryPolicy} with the specified backoff settings
   */
  public static RetryPolicy of(
      String name, int maxAttempts, long initialBackoffMs, long maxBackoffMs) {
    return new RetryPolicy(name, maxAttempts, initialBackoffMs, maxBackoffMs);
  }

  /**
   * Computes the backoff delay for the given attempt number (1-based). Uses exponential backoff:
   * {@code initialBackoffMs * 2^(attempt-1)}, capped at {@code maxBackoffMs}.
   *
   * @param attempt the 1-based attempt number
   * @return the backoff delay in milliseconds for the given attempt
   */
  public long getBackoffMs(int attempt) {
    return Math.min(initialBackoffMs * (1L << (attempt - 1)), maxBackoffMs);
  }

  /**
   * Returns the policy name.
   *
   * @return the name identifying this policy
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the maximum number of attempts.
   *
   * @return the maximum number of attempts allowed by this policy
   */
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * Returns the initial backoff delay in milliseconds.
   *
   * @return the initial backoff delay in milliseconds
   */
  public long getInitialBackoffMs() {
    return initialBackoffMs;
  }

  /**
   * Returns the maximum backoff delay in milliseconds.
   *
   * @return the maximum backoff delay in milliseconds
   */
  public long getMaxBackoffMs() {
    return maxBackoffMs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RetryPolicy)) return false;
    RetryPolicy that = (RetryPolicy) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name
        + "[maxAttempts="
        + maxAttempts
        + ", initialBackoffMs="
        + initialBackoffMs
        + ", maxBackoffMs="
        + maxBackoffMs
        + "]";
  }
}

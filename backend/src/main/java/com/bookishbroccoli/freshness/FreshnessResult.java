package com.bookishbroccoli.freshness;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Immutable record capturing the result of a freshness check.
 *
 * <p>Includes whether the data is stale, the original timestamp and computed age (if available),
 * and an optional reason for staleness.
 *
 * @param stale whether the data is considered stale
 * @param timestamp the original timestamp that was checked, if available
 * @param age the computed age of the data, if available
 * @param reason an optional human-readable reason for staleness
 */
public record FreshnessResult(
    boolean stale, Optional<Instant> timestamp, Optional<Duration> age, Optional<String> reason) {

  /**
   * Creates a fresh result with the given timestamp and age.
   *
   * @param timestamp the original timestamp, or {@code null} if unavailable
   * @param age the computed age, or {@code null} if unavailable
   * @return a {@code FreshnessResult} marked as fresh
   */
  public static FreshnessResult fresh(Instant timestamp, Duration age) {
    return new FreshnessResult(
        false, Optional.ofNullable(timestamp), Optional.ofNullable(age), Optional.empty());
  }

  /**
   * Creates a stale result with a reason but no timestamp or age.
   *
   * @param reason a human-readable explanation of why the data is stale
   * @return a {@code FreshnessResult} marked as stale
   */
  public static FreshnessResult stale(String reason) {
    return new FreshnessResult(true, Optional.empty(), Optional.empty(), Optional.of(reason));
  }

  /**
   * Creates a stale result with full details including timestamp, age, and reason.
   *
   * @param timestamp the original timestamp that was checked
   * @param age the computed age of the data
   * @param reason a human-readable explanation of why the data is stale
   * @return a {@code FreshnessResult} marked as stale
   */
  public static FreshnessResult stale(Instant timestamp, Duration age, String reason) {
    return new FreshnessResult(true, Optional.of(timestamp), Optional.of(age), Optional.of(reason));
  }

  /**
   * Returns {@code true} if the data is not stale.
   *
   * @return {@code true} if fresh, {@code false} if stale
   */
  public boolean isFresh() {
    return !stale;
  }
}

package com.bookishbroccoli.freshness;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration defining how fresh data must be.
 *
 * <p>Specifies a maximum age and whether missing timestamps should be treated as stale. Policies
 * are identified by name and equality is based on name alone.
 */
public class FreshnessPolicy {

  private final String name;
  private final Duration maxAge;
  private final boolean staleWhenMissing;

  private FreshnessPolicy(String name, Duration maxAge, boolean staleWhenMissing) {
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.maxAge = Objects.requireNonNull(maxAge, "maxAge must not be null");
    this.staleWhenMissing = staleWhenMissing;
  }

  /**
   * Creates a policy that treats missing timestamps as stale.
   *
   * @param name the policy name
   * @param maxAge the maximum acceptable age for data
   * @return a new {@code FreshnessPolicy} with {@code staleWhenMissing} set to {@code true}
   */
  public static FreshnessPolicy of(String name, Duration maxAge) {
    return new FreshnessPolicy(name, maxAge, true);
  }

  /**
   * Creates a policy with explicit {@code staleWhenMissing} control.
   *
   * @param name the policy name
   * @param maxAge the maximum acceptable age for data
   * @param staleWhenMissing whether a missing timestamp should be considered stale
   * @return a new {@code FreshnessPolicy}
   */
  public static FreshnessPolicy of(String name, Duration maxAge, boolean staleWhenMissing) {
    return new FreshnessPolicy(name, maxAge, staleWhenMissing);
  }

  /**
   * Returns the policy name.
   *
   * @return the policy name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the maximum acceptable age for data under this policy.
   *
   * @return the maximum acceptable age
   */
  public Duration getMaxAge() {
    return maxAge;
  }

  /**
   * Returns whether a missing timestamp should be considered stale.
   *
   * @return {@code true} if a missing timestamp should be treated as stale
   */
  public boolean isStaleWhenMissing() {
    return staleWhenMissing;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FreshnessPolicy that = (FreshnessPolicy) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name + "[maxAge=" + maxAge + "]";
  }
}

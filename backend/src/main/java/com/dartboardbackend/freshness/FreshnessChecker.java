package com.dartboardbackend.freshness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Service that evaluates data freshness against a {@link FreshnessPolicy}.
 *
 * <p>Supports checking timestamps as {@link Instant}, ISO-8601 strings, epoch milliseconds ({@link
 * Long}), and {@link java.util.Date}. Also provides batch operations for filtering and finding the
 * most recent timestamp.
 */
@Service
public class FreshnessChecker {

  private final Clock clock;

  /** Creates a {@code FreshnessChecker} using the UTC system clock. */
  public FreshnessChecker() {
    this(Clock.systemUTC());
  }

  /**
   * Package-private constructor for testing with a controllable clock.
   *
   * @param clock the clock to use for determining the current time
   */
  FreshnessChecker(Clock clock) {
    this.clock = clock;
  }

  /**
   * Checks an {@link Instant} timestamp against the given policy.
   *
   * <p>Null timestamps delegate to the policy's {@link FreshnessPolicy#isStaleWhenMissing()}
   * setting. Future timestamps are treated as fresh.
   *
   * @param timestamp the timestamp to check, or {@code null} if unavailable
   * @param policy the freshness policy to evaluate against
   * @return the result of the freshness check
   */
  public FreshnessResult check(Instant timestamp, FreshnessPolicy policy) {
    if (timestamp == null) {
      return policy.isStaleWhenMissing()
          ? FreshnessResult.stale("no timestamp available")
          : FreshnessResult.fresh(null, null);
    }

    Instant now = clock.instant();
    if (timestamp.isAfter(now)) {
      return FreshnessResult.fresh(timestamp, Duration.ZERO);
    }

    Duration age = Duration.between(timestamp, now);
    if (age.compareTo(policy.getMaxAge()) <= 0) {
      return FreshnessResult.fresh(timestamp, age);
    }

    return FreshnessResult.stale(
        timestamp, age, "age " + age + " exceeds max " + policy.getMaxAge());
  }

  /**
   * Checks an ISO-8601 or {@link LocalDate} string against the given policy.
   *
   * <p>Blank or {@code null} strings delegate to the policy's {@link
   * FreshnessPolicy#isStaleWhenMissing()} setting. Unparseable strings are treated as stale.
   *
   * @param isoTimestamp the timestamp string to check, or {@code null} if unavailable
   * @param policy the freshness policy to evaluate against
   * @return the result of the freshness check
   */
  public FreshnessResult check(String isoTimestamp, FreshnessPolicy policy) {
    if (isoTimestamp == null || isoTimestamp.isBlank()) {
      return policy.isStaleWhenMissing()
          ? FreshnessResult.stale("no timestamp available")
          : FreshnessResult.fresh(null, null);
    }

    Optional<Instant> parsed = parseIsoOrLocalDate(isoTimestamp);
    if (parsed.isEmpty()) {
      return FreshnessResult.stale("unparseable timestamp: " + isoTimestamp);
    }

    return check(parsed.get(), policy);
  }

  /**
   * Polymorphic check that dispatches to the appropriate overload based on runtime type.
   *
   * <p>Supported types are {@link Instant}, {@link String}, {@link Long} (epoch milliseconds), and
   * {@link Date}. Unsupported types are treated as stale.
   *
   * @param value the timestamp value to check (may be {@code null})
   * @param policy the freshness policy to evaluate against
   * @return the result of the freshness check
   */
  public FreshnessResult check(Object value, FreshnessPolicy policy) {
    if (value == null) {
      return check((Instant) null, policy);
    }
    if (value instanceof Instant instant) {
      return check(instant, policy);
    }
    if (value instanceof String s) {
      return check(s, policy);
    }
    if (value instanceof Long epochMillis) {
      return check(Instant.ofEpochMilli(epochMillis), policy);
    }
    if (value instanceof Date date) {
      return check(date.toInstant(), policy);
    }
    return FreshnessResult.stale("unsupported timestamp type: " + value.getClass().getSimpleName());
  }

  /**
   * Returns items whose extracted timestamp is stale according to the given policy.
   *
   * @param <T> the type of items in the list
   * @param items the items to evaluate
   * @param extractor a function that extracts an {@link Instant} timestamp from each item
   * @param policy the freshness policy to evaluate against
   * @return a list of items whose timestamps are stale
   */
  public <T> List<T> findStale(
      List<T> items, Function<T, Instant> extractor, FreshnessPolicy policy) {
    return items.stream().filter(item -> check(extractor.apply(item), policy).stale()).toList();
  }

  /**
   * Returns items whose extracted timestamp is fresh according to the given policy.
   *
   * @param <T> the type of items in the list
   * @param items the items to evaluate
   * @param extractor a function that extracts an {@link Instant} timestamp from each item
   * @param policy the freshness policy to evaluate against
   * @return a list of items whose timestamps are fresh
   */
  public <T> List<T> findFresh(
      List<T> items, Function<T, Instant> extractor, FreshnessPolicy policy) {
    return items.stream().filter(item -> check(extractor.apply(item), policy).isFresh()).toList();
  }

  /**
   * Finds the most recent timestamp among items and checks it against the policy.
   *
   * <p>Returns {@link Optional#empty()} if the list is empty. If all extracted timestamps are
   * {@code null}, the result delegates to the policy's missing-timestamp behavior.
   *
   * @param <T> the type of items in the list
   * @param items the items to evaluate
   * @param extractor a function that extracts an {@link Instant} timestamp from each item
   * @param policy the freshness policy to evaluate against
   * @return a freshness result for the most recent timestamp, or empty if the list is empty
   */
  public <T> Optional<FreshnessResult> checkMostRecent(
      List<T> items, Function<T, Instant> extractor, FreshnessPolicy policy) {
    if (items.isEmpty()) {
      return Optional.empty();
    }

    Optional<Instant> mostRecent =
        items.stream().map(extractor).filter(ts -> ts != null).max(Instant::compareTo);

    if (mostRecent.isEmpty()) {
      return Optional.of(check((Instant) null, policy));
    }

    return Optional.of(check(mostRecent.get(), policy));
  }

  /**
   * Static utility that parses various timestamp types into an {@link Optional} {@link Instant}.
   *
   * <p>Supported types are {@link Instant}, {@link Long} (epoch milliseconds), {@link Date}, and
   * {@link String} (ISO-8601 or {@link LocalDate} format). Returns empty for {@code null} or
   * unsupported types.
   *
   * @param value the value to parse
   * @return an {@link Optional} containing the parsed {@link Instant}, or empty if parsing fails
   */
  public static Optional<Instant> parseTimestamp(Object value) {
    if (value == null) {
      return Optional.empty();
    }
    if (value instanceof Instant instant) {
      return Optional.of(instant);
    }
    if (value instanceof Long epochMillis) {
      return Optional.of(Instant.ofEpochMilli(epochMillis));
    }
    if (value instanceof Date date) {
      return Optional.of(date.toInstant());
    }
    if (value instanceof String s) {
      return parseIsoOrLocalDate(s);
    }
    return Optional.empty();
  }

  /**
   * Attempts to parse a string as a timestamp using two-stage fallback logic: tries ISO-8601
   * instant format first, then falls back to {@link LocalDate} format (converted to start-of-day
   * UTC).
   */
  private static Optional<Instant> parseIsoOrLocalDate(String value) {
    try {
      return Optional.of(Instant.parse(value));
    } catch (DateTimeParseException e) {
      // fall through to LocalDate attempt
    }
    try {
      return Optional.of(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant());
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }
}

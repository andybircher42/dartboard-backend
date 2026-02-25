package com.dartboardbackend.freshness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ParseTimestampTest {

  @Test
  void null_empty() {
    assertTrue(FreshnessChecker.parseTimestamp(null).isEmpty());
  }

  @Test
  void instant_present() {
    Instant ts = Instant.now();
    Optional<Instant> result = FreshnessChecker.parseTimestamp(ts);

    assertTrue(result.isPresent());
    assertEquals(ts, result.get());
  }

  @Test
  void long_present() {
    long millis = System.currentTimeMillis();
    Optional<Instant> result = FreshnessChecker.parseTimestamp(millis);

    assertTrue(result.isPresent());
    assertEquals(Instant.ofEpochMilli(millis), result.get());
  }

  @Test
  void date_present() {
    Date date = new Date();
    Optional<Instant> result = FreshnessChecker.parseTimestamp(date);

    assertTrue(result.isPresent());
    assertEquals(date.toInstant(), result.get());
  }

  @Test
  void isoString_present() {
    Optional<Instant> result = FreshnessChecker.parseTimestamp("2026-01-15T12:00:00Z");

    assertTrue(result.isPresent());
    assertEquals(Instant.parse("2026-01-15T12:00:00Z"), result.get());
  }

  @Test
  void localDateString_present() {
    Optional<Instant> result = FreshnessChecker.parseTimestamp("2026-01-15");

    assertTrue(result.isPresent());
    assertEquals(Instant.parse("2026-01-15T00:00:00Z"), result.get());
  }

  @Test
  void garbage_empty() {
    assertTrue(FreshnessChecker.parseTimestamp("not-a-date").isEmpty());
  }

  @Test
  void unsupportedType_empty() {
    assertTrue(FreshnessChecker.parseTimestamp(3.14).isEmpty());
  }
}

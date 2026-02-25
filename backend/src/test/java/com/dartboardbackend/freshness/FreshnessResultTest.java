package com.dartboardbackend.freshness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FreshnessResultTest {

  @Test
  void fresh_notStale() {
    Instant ts = Instant.parse("2026-01-15T10:00:00Z");
    FreshnessResult result = FreshnessResult.fresh(ts, Duration.ofHours(2));

    assertFalse(result.stale());
  }

  @Test
  void fresh_hasTimestampAndAge() {
    Instant ts = Instant.parse("2026-01-15T10:00:00Z");
    Duration age = Duration.ofHours(2);
    FreshnessResult result = FreshnessResult.fresh(ts, age);

    assertEquals(ts, result.timestamp().orElseThrow());
    assertEquals(age, result.age().orElseThrow());
    assertTrue(result.reason().isEmpty());
  }

  @Test
  void stale_withReasonOnly() {
    FreshnessResult result = FreshnessResult.stale("no timestamp available");

    assertTrue(result.stale());
    assertTrue(result.timestamp().isEmpty());
    assertTrue(result.age().isEmpty());
    assertEquals("no timestamp available", result.reason().orElseThrow());
  }

  @Test
  void stale_isFreshReturnsFalse() {
    FreshnessResult result = FreshnessResult.stale("missing");

    assertFalse(result.isFresh());
  }

  @Test
  void stale_withTimestampAndAge() {
    Instant ts = Instant.parse("2026-01-01T00:00:00Z");
    Duration age = Duration.ofDays(15);
    FreshnessResult result = FreshnessResult.stale(ts, age, "too old");

    assertTrue(result.stale());
    assertTrue(result.timestamp().isPresent());
    assertTrue(result.age().isPresent());
    assertEquals("too old", result.reason().orElseThrow());
  }

  @Test
  void stale_withTimestampAndAge_preservesValues() {
    Instant ts = Instant.parse("2026-01-01T00:00:00Z");
    Duration age = Duration.ofDays(15);
    FreshnessResult result = FreshnessResult.stale(ts, age, "expired");

    assertEquals(ts, result.timestamp().orElseThrow());
    assertEquals(age, result.age().orElseThrow());
  }

  @Test
  void isFresh_fresh_returnsTrue() {
    FreshnessResult result = FreshnessResult.fresh(Instant.now(), Duration.ofMinutes(5));

    assertTrue(result.isFresh());
  }

  @Test
  void isFresh_stale_returnsFalse() {
    FreshnessResult result = FreshnessResult.stale("expired");

    assertFalse(result.isFresh());
  }
}

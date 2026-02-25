package com.dartboardbackend.freshness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckStringTest {

  private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

  private FreshnessChecker checker;
  private FreshnessPolicy policy;

  @BeforeEach
  void setUp() {
    checker = new FreshnessChecker(Clock.fixed(NOW, ZoneOffset.UTC));
    policy = FreshnessPolicy.of("test", Duration.ofDays(7));
  }

  @Test
  void validIso_fresh() {
    String recent = NOW.minus(Duration.ofHours(3)).toString();
    FreshnessResult result = checker.check(recent, policy);

    assertTrue(result.isFresh());
  }

  @Test
  void validIso_stale() {
    String old = NOW.minus(Duration.ofDays(14)).toString();
    FreshnessResult result = checker.check(old, policy);

    assertTrue(result.stale());
  }

  @Test
  void localDate_fresh() {
    FreshnessResult result = checker.check("2026-01-14", policy);

    assertTrue(result.isFresh());
  }

  @Test
  void localDate_stale() {
    FreshnessResult result = checker.check("2025-12-01", policy);

    assertTrue(result.stale());
  }

  @Test
  void null_stale() {
    FreshnessResult result = checker.check((String) null, policy);

    assertTrue(result.stale());
  }

  @Test
  void blank_stale() {
    FreshnessResult result = checker.check("   ", policy);

    assertTrue(result.stale());
  }

  @Test
  void garbage_stale() {
    FreshnessResult result = checker.check("not-a-date", policy);

    assertTrue(result.stale());
    assertTrue(result.reason().orElseThrow().contains("unparseable"));
  }
}

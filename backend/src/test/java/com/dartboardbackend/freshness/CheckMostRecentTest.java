package com.dartboardbackend.freshness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckMostRecentTest {

  private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

  private FreshnessChecker checker;
  private FreshnessPolicy policy;

  @BeforeEach
  void setUp() {
    checker = new FreshnessChecker(Clock.fixed(NOW, ZoneOffset.UTC));
    policy = FreshnessPolicy.of("test", Duration.ofDays(7));
  }

  @Test
  void freshestIsFresh() {
    Instant old = NOW.minus(Duration.ofDays(10));
    Instant recent = NOW.minus(Duration.ofDays(1));

    Optional<FreshnessResult> result =
        checker.checkMostRecent(List.of(old, recent), ts -> ts, policy);

    assertTrue(result.isPresent());
    assertTrue(result.get().isFresh());
    assertEquals(recent, result.get().timestamp().orElseThrow());
  }

  @Test
  void freshestIsStale() {
    Instant old = NOW.minus(Duration.ofDays(20));
    Instant lessOld = NOW.minus(Duration.ofDays(10));

    Optional<FreshnessResult> result =
        checker.checkMostRecent(List.of(old, lessOld), ts -> ts, policy);

    assertTrue(result.isPresent());
    assertTrue(result.get().stale());
  }

  @Test
  void emptyList_empty() {
    List<Instant> empty = List.of();
    Optional<FreshnessResult> result = checker.checkMostRecent(empty, ts -> ts, policy);

    assertTrue(result.isEmpty());
  }

  @Test
  void allNullTimestamps_stale() {
    List<String> items = List.of("a", "b");
    Function<String, Instant> extractor = item -> null;

    Optional<FreshnessResult> result = checker.checkMostRecent(items, extractor, policy);

    assertTrue(result.isPresent());
    assertTrue(result.get().stale());
  }
}

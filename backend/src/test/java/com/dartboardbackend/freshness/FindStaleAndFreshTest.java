package com.dartboardbackend.freshness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FindStaleAndFreshTest {

  private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

  private FreshnessChecker checker;
  private FreshnessPolicy policy;

  @BeforeEach
  void setUp() {
    checker = new FreshnessChecker(Clock.fixed(NOW, ZoneOffset.UTC));
    policy = FreshnessPolicy.of("test", Duration.ofDays(7));
  }

  @Test
  void findStale_returnsStaleItems() {
    Instant fresh = NOW.minus(Duration.ofDays(1));
    Instant stale = NOW.minus(Duration.ofDays(10));

    List<Instant> items = List.of(fresh, stale);
    List<Instant> result = checker.findStale(items, ts -> ts, policy);

    assertEquals(1, result.size());
    assertEquals(stale, result.get(0));
  }

  @Test
  void findFresh_returnsFreshItems() {
    Instant fresh = NOW.minus(Duration.ofDays(1));
    Instant stale = NOW.minus(Duration.ofDays(10));

    List<Instant> items = List.of(fresh, stale);
    List<Instant> result = checker.findFresh(items, ts -> ts, policy);

    assertEquals(1, result.size());
    assertEquals(fresh, result.get(0));
  }

  @Test
  void findStale_emptyList_returnsEmpty() {
    List<Instant> result = checker.findStale(List.of(), ts -> ts, policy);

    assertTrue(result.isEmpty());
  }

  @Test
  void findFresh_emptyList_returnsEmpty() {
    List<Instant> result = checker.findFresh(List.of(), ts -> ts, policy);

    assertTrue(result.isEmpty());
  }
}

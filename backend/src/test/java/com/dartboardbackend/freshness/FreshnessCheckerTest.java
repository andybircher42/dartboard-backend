package com.dartboardbackend.freshness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FreshnessCheckerTest {

  private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

  private FreshnessChecker checker;
  private FreshnessPolicy policy;

  @BeforeEach
  void setUp() {
    checker = new FreshnessChecker(Clock.fixed(NOW, ZoneOffset.UTC));
    policy = FreshnessPolicy.of("test", Duration.ofDays(7));
  }

  @Nested
  class CheckInstant {

    @Test
    void recent_fresh() {
      Instant recent = NOW.minus(Duration.ofHours(6));
      FreshnessResult result = checker.check(recent, policy);

      assertTrue(result.isFresh());
      assertEquals(recent, result.timestamp().orElseThrow());
      assertEquals(Duration.ofHours(6), result.age().orElseThrow());
    }

    @Test
    void old_stale() {
      Instant old = NOW.minus(Duration.ofDays(10));
      FreshnessResult result = checker.check(old, policy);

      assertTrue(result.stale());
      assertEquals(old, result.timestamp().orElseThrow());
      assertTrue(result.reason().isPresent());
    }

    @Test
    void exactMaxAge_fresh() {
      Instant exact = NOW.minus(Duration.ofDays(7));
      FreshnessResult result = checker.check(exact, policy);

      assertTrue(result.isFresh());
    }

    @Test
    void null_staleWhenMissing_stale() {
      FreshnessResult result = checker.check((Instant) null, policy);

      assertTrue(result.stale());
      assertTrue(result.timestamp().isEmpty());
      assertEquals("no timestamp available", result.reason().orElseThrow());
    }

    @Test
    void null_lenient_fresh() {
      FreshnessPolicy lenient = FreshnessPolicy.of("lenient", Duration.ofDays(7), false);
      FreshnessResult result = checker.check((Instant) null, lenient);

      assertTrue(result.isFresh());
    }

    @Test
    void future_fresh() {
      Instant future = NOW.plus(Duration.ofHours(1));
      FreshnessResult result = checker.check(future, policy);

      assertTrue(result.isFresh());
      assertEquals(Duration.ZERO, result.age().orElseThrow());
    }
  }

  @Nested
  class CheckString {

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

  @Nested
  class CheckObject {

    @Test
    void instant_dispatches() {
      Instant recent = NOW.minus(Duration.ofHours(1));
      FreshnessResult result = checker.check((Object) recent, policy);

      assertTrue(result.isFresh());
    }

    @Test
    void string_dispatches() {
      FreshnessResult result =
          checker.check((Object) NOW.minus(Duration.ofHours(1)).toString(), policy);

      assertTrue(result.isFresh());
    }

    @Test
    void long_dispatches() {
      Long epochMillis = NOW.minus(Duration.ofHours(1)).toEpochMilli();
      FreshnessResult result = checker.check((Object) epochMillis, policy);

      assertTrue(result.isFresh());
    }

    @Test
    void date_dispatches() {
      Date date = Date.from(NOW.minus(Duration.ofHours(1)));
      FreshnessResult result = checker.check((Object) date, policy);

      assertTrue(result.isFresh());
    }

    @Test
    void null_stale() {
      FreshnessResult result = checker.check((Object) null, policy);

      assertTrue(result.stale());
    }

    @Test
    void unsupportedType_stale() {
      FreshnessResult result = checker.check((Object) 42, policy);

      assertTrue(result.stale());
      assertTrue(result.reason().orElseThrow().contains("unsupported"));
    }
  }

  @Nested
  class CheckMostRecent {

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

  @Nested
  class FindStaleAndFresh {

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

  @Nested
  class ParseTimestamp {

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
}

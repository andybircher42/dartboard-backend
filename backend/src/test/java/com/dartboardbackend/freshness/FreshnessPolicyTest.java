package com.dartboardbackend.freshness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class FreshnessPolicyTest {

  @Test
  void of_createsWithAllFields() {
    FreshnessPolicy policy = FreshnessPolicy.of("detail_freshness", Duration.ofHours(12), false);

    assertEquals("detail_freshness", policy.getName());
    assertEquals(Duration.ofHours(12), policy.getMaxAge());
    assertFalse(policy.isStaleWhenMissing());
  }

  @Test
  void of_defaultsStaleWhenMissingToTrue() {
    FreshnessPolicy policy = FreshnessPolicy.of("zip_freshness", Duration.ofDays(14));

    assertTrue(policy.isStaleWhenMissing());
  }

  @Test
  void of_staleWhenMissingFalse() {
    FreshnessPolicy policy = FreshnessPolicy.of("lenient", Duration.ofHours(1), false);

    assertFalse(policy.isStaleWhenMissing());
  }

  @Test
  void of_nullName_throws() {
    assertThrows(NullPointerException.class, () -> FreshnessPolicy.of(null, Duration.ofHours(1)));
  }

  @Test
  void of_nullMaxAge_throws() {
    assertThrows(NullPointerException.class, () -> FreshnessPolicy.of("test", null));
  }

  @Test
  void equals_sameName() {
    FreshnessPolicy a = FreshnessPolicy.of("detail", Duration.ofHours(12));
    FreshnessPolicy b = FreshnessPolicy.of("detail", Duration.ofDays(7));

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void notEquals_differentName() {
    FreshnessPolicy a = FreshnessPolicy.of("detail", Duration.ofHours(12));
    FreshnessPolicy b = FreshnessPolicy.of("zip", Duration.ofHours(12));

    assertNotEquals(a, b);
  }

  @Test
  void toString_includesNameAndMaxAge() {
    FreshnessPolicy policy = FreshnessPolicy.of("detail_freshness", Duration.ofHours(12));

    String result = policy.toString();
    assertTrue(result.contains("detail_freshness"));
    assertTrue(result.contains("PT12H"));
  }
}

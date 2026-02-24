package com.bookishbroccoli.freshness;

import java.time.Duration;
import java.time.Instant;

public record FreshnessResult(boolean stale, Instant timestamp, Duration age, String reason) {

	public static FreshnessResult fresh(Instant timestamp, Duration age) {
		return new FreshnessResult(false, timestamp, age, null);
	}

	public static FreshnessResult stale(String reason) {
		return new FreshnessResult(true, null, null, reason);
	}

	public static FreshnessResult stale(Instant timestamp, Duration age, String reason) {
		return new FreshnessResult(true, timestamp, age, reason);
	}

	public boolean isFresh() {
		return !stale;
	}
}

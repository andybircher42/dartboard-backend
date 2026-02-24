package com.bookishbroccoli.freshness;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record FreshnessResult(boolean stale, Optional<Instant> timestamp, Optional<Duration> age, Optional<String> reason) {

	public static FreshnessResult fresh(Instant timestamp, Duration age) {
		return new FreshnessResult(false, Optional.ofNullable(timestamp), Optional.ofNullable(age), Optional.empty());
	}

	public static FreshnessResult stale(String reason) {
		return new FreshnessResult(true, Optional.empty(), Optional.empty(), Optional.of(reason));
	}

	public static FreshnessResult stale(Instant timestamp, Duration age, String reason) {
		return new FreshnessResult(true, Optional.of(timestamp), Optional.of(age), Optional.of(reason));
	}

	public boolean isFresh() {
		return !stale;
	}
}

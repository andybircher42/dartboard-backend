package com.bookishbroccoli.freshness;

import java.time.Duration;
import java.util.Objects;

public class FreshnessPolicy {

	private final String name;
	private final Duration maxAge;
	private final boolean staleWhenMissing;

	private FreshnessPolicy(String name, Duration maxAge, boolean staleWhenMissing) {
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.maxAge = Objects.requireNonNull(maxAge, "maxAge must not be null");
		this.staleWhenMissing = staleWhenMissing;
	}

	public static FreshnessPolicy of(String name, Duration maxAge) {
		return new FreshnessPolicy(name, maxAge, true);
	}

	public static FreshnessPolicy of(String name, Duration maxAge, boolean staleWhenMissing) {
		return new FreshnessPolicy(name, maxAge, staleWhenMissing);
	}

	public String getName() {
		return name;
	}

	public Duration getMaxAge() {
		return maxAge;
	}

	public boolean isStaleWhenMissing() {
		return staleWhenMissing;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FreshnessPolicy that = (FreshnessPolicy) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return name + "[maxAge=" + maxAge + "]";
	}
}

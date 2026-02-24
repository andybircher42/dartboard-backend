package com.bookishbroccoli.retry;

import java.util.Objects;

public class RetryPolicy {

	private final String name;
	private final int maxAttempts;
	private final long initialBackoffMs;
	private final long maxBackoffMs;

	private RetryPolicy(String name, int maxAttempts, long initialBackoffMs, long maxBackoffMs) {
		this.name = Objects.requireNonNull(name, "name must not be null");
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be >= 1, got " + maxAttempts);
		}
		this.maxAttempts = maxAttempts;
		this.initialBackoffMs = initialBackoffMs;
		this.maxBackoffMs = maxBackoffMs;
	}

	public static RetryPolicy of(String name, int maxAttempts) {
		return new RetryPolicy(name, maxAttempts, 10_000, 60_000);
	}

	public static RetryPolicy of(String name, int maxAttempts, long initialBackoffMs, long maxBackoffMs) {
		return new RetryPolicy(name, maxAttempts, initialBackoffMs, maxBackoffMs);
	}

	public long getBackoffMs(int attempt) {
		return Math.min(initialBackoffMs * (1L << (attempt - 1)), maxBackoffMs);
	}

	public String getName() {
		return name;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public long getInitialBackoffMs() {
		return initialBackoffMs;
	}

	public long getMaxBackoffMs() {
		return maxBackoffMs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RetryPolicy that = (RetryPolicy) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return name + "[maxAttempts=" + maxAttempts
				+ ", initialBackoffMs=" + initialBackoffMs
				+ ", maxBackoffMs=" + maxBackoffMs + "]";
	}
}

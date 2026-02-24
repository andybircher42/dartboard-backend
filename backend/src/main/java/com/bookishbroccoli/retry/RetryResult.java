package com.bookishbroccoli.retry;

public record RetryResult<T>(boolean succeeded, T value, int attempts, Exception lastException) {

	public static <T> RetryResult<T> success(T value, int attempts) {
		return new RetryResult<>(true, value, attempts, null);
	}

	public static <T> RetryResult<T> failure(int attempts, Exception lastException) {
		return new RetryResult<>(false, null, attempts, lastException);
	}

	public T getOrThrow() {
		if (succeeded) {
			return value;
		}
		throw new RuntimeException(
				"All " + attempts + " attempts exhausted", lastException);
	}
}

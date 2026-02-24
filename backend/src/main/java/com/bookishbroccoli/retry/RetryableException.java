package com.bookishbroccoli.retry;

/**
 * Generic base for transient errors that should be retried.
 * Any service can throw this (or a subclass) to signal "retry me."
 */
public class RetryableException extends RuntimeException {

	public RetryableException(String message) {
		super(message);
	}

	public RetryableException(String message, Throwable cause) {
		super(message, cause);
	}
}

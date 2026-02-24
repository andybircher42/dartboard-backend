package com.bookishbroccoli.retry;

/**
 * Generic base for permanent errors that should NOT be retried.
 * Any service can throw this (or a subclass) to signal "don't retry."
 */
public class NonRetryableException extends RuntimeException {

	public NonRetryableException(String message) {
		super(message);
	}

	public NonRetryableException(String message, Throwable cause) {
		super(message, cause);
	}
}

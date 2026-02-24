package com.bookishbroccoli.service;

import com.bookishbroccoli.retry.NonRetryableException;

import java.util.Optional;

/**
 * Thrown for permanent Apify errors that should NOT be retried:
 * HTTP 400 (bad request), 401 (unauthorized), 404 (not found),
 * and body-level validation failures (isValid=false).
 */
public class ApifyNonRetryableException extends NonRetryableException {

	private final int statusCode;
	private final String responseBody;

	public ApifyNonRetryableException(String message, int statusCode, String responseBody) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public ApifyNonRetryableException(String message) {
		super(message);
		this.statusCode = -1;
		this.responseBody = null;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public Optional<String> getResponseBody() {
		return Optional.ofNullable(responseBody);
	}
}

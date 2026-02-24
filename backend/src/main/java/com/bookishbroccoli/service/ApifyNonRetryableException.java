package com.bookishbroccoli.service;

/**
 * Thrown for permanent Apify errors that should NOT be retried:
 * HTTP 400 (bad request), 401 (unauthorized), 404 (not found),
 * and body-level validation failures (isValid=false).
 */
public class ApifyNonRetryableException extends ApifyApiException {

	public ApifyNonRetryableException(String message, int statusCode, String responseBody) {
		super(message, statusCode, responseBody);
	}

	public ApifyNonRetryableException(String message) {
		super(message, -1, null);
	}
}

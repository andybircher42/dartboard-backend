package com.bookishbroccoli.service;

/**
 * Thrown for transient Apify errors that should be retried:
 * HTTP 429 (rate limit), 5xx (server errors), and network failures (IOException).
 */
public class ApifyRetryableException extends ApifyApiException {

	public ApifyRetryableException(String message, int statusCode, String responseBody) {
		super(message, statusCode, responseBody);
	}

	public ApifyRetryableException(String message, Throwable cause) {
		super(message, cause);
	}
}

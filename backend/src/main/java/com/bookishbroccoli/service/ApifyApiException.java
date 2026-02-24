package com.bookishbroccoli.service;

/**
 * Base exception for Apify API errors. Carries HTTP status code and response body
 * so callers can distinguish retryable from non-retryable failures.
 */
public class ApifyApiException extends RuntimeException {

	private final int statusCode;
	private final String responseBody;

	public ApifyApiException(String message, int statusCode, String responseBody) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public ApifyApiException(String message, Throwable cause) {
		super(message, cause);
		this.statusCode = -1;
		this.responseBody = null;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getResponseBody() {
		return responseBody;
	}
}

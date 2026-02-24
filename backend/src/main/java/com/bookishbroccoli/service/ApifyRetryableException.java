package com.bookishbroccoli.service;

import com.bookishbroccoli.retry.RetryableException;

/**
 * Thrown for transient Apify errors that should be retried:
 * HTTP 429 (rate limit), 5xx (server errors), and network failures (IOException).
 */
public class ApifyRetryableException extends RetryableException {

	private final int statusCode;
	private final String responseBody;

	public ApifyRetryableException(String message, int statusCode, String responseBody) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public ApifyRetryableException(String message, Throwable cause) {
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

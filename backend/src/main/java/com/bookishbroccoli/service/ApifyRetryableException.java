package com.bookishbroccoli.service;

import com.bookishbroccoli.retry.RetryableException;
import java.util.Optional;

/**
 * Thrown for transient Apify errors that should be retried: HTTP 429 (rate limit), 5xx (server
 * errors), and network failures (IOException).
 */
public class ApifyRetryableException extends RetryableException {

  private final int statusCode;
  private final String responseBody;

  /**
   * Constructs an exception for an HTTP-level retryable error.
   *
   * @param message human-readable description of the error
   * @param statusCode the HTTP status code (e.g. 429, 502)
   * @param responseBody the raw response body returned by Apify
   */
  public ApifyRetryableException(String message, int statusCode, String responseBody) {
    super(message);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  /**
   * Constructs an exception for a network-level retryable error, wrapping the underlying cause.
   *
   * @param message human-readable description of the error
   * @param cause the underlying exception (typically an {@link java.io.IOException})
   */
  public ApifyRetryableException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = -1;
    this.responseBody = null;
  }

  /**
   * Returns the HTTP status code associated with this error.
   *
   * @return the HTTP status code, or {@code -1} if this exception wraps a network error
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the raw HTTP response body, if available.
   *
   * @return an {@link Optional} containing the response body, or empty for network errors
   */
  public Optional<String> getResponseBody() {
    return Optional.ofNullable(responseBody);
  }
}

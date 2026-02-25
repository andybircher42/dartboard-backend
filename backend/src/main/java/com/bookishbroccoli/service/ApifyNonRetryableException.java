package com.bookishbroccoli.service;

import com.bookishbroccoli.retry.NonRetryableException;
import java.util.Optional;

/**
 * Thrown for permanent Apify errors that should NOT be retried: HTTP 400 (bad request), 401
 * (unauthorized), 404 (not found), and body-level validation failures (isValid=false).
 */
public class ApifyNonRetryableException extends NonRetryableException {

  private final int statusCode;
  private final String responseBody;

  /**
   * Constructs an exception for an HTTP-level non-retryable error.
   *
   * @param message human-readable description of the error
   * @param statusCode the HTTP status code (e.g. 400, 401, 404)
   * @param responseBody the raw response body returned by Apify
   */
  public ApifyNonRetryableException(String message, int statusCode, String responseBody) {
    super(message);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  /**
   * Constructs an exception for a body-level error detected in the Apify response payload.
   *
   * @param message human-readable description of the error found in the response body
   */
  public ApifyNonRetryableException(String message) {
    super(message);
    this.statusCode = -1;
    this.responseBody = null;
  }

  /**
   * Returns the HTTP status code associated with this error.
   *
   * @return the HTTP status code, or {@code -1} if this exception was created for a body-level
   *     error
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the raw HTTP response body, if available.
   *
   * @return an {@link Optional} containing the response body, or empty for body-level errors
   */
  public Optional<String> getResponseBody() {
    return Optional.ofNullable(responseBody);
  }
}

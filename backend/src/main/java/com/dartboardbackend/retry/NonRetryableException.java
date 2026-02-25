package com.dartboardbackend.retry;

/**
 * Generic base for permanent errors that should NOT be retried. Any service can throw this (or a
 * subclass) to signal "don't retry."
 */
public class NonRetryableException extends RuntimeException {

  /**
   * Creates an exception with a message describing the permanent failure.
   *
   * @param message a description of the permanent failure that triggered this exception
   */
  public NonRetryableException(String message) {
    super(message);
  }

  /**
   * Creates an exception wrapping the underlying cause.
   *
   * @param message a description of the permanent failure
   * @param cause the underlying cause of this exception
   */
  public NonRetryableException(String message, Throwable cause) {
    super(message, cause);
  }
}

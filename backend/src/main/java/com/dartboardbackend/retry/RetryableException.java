package com.dartboardbackend.retry;

/**
 * Generic base for transient errors that should be retried. Any service can throw this (or a
 * subclass) to signal "retry me."
 */
public class RetryableException extends RuntimeException {

  /**
   * Creates an exception with a message describing the transient failure.
   *
   * @param message a description of the transient failure that triggered this exception
   */
  public RetryableException(String message) {
    super(message);
  }

  /**
   * Creates an exception wrapping the underlying cause.
   *
   * @param message a description of the transient failure
   * @param cause the underlying cause of this exception
   */
  public RetryableException(String message, Throwable cause) {
    super(message, cause);
  }
}

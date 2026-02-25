package com.dartboardbackend.retry;

/**
 * Immutable record capturing the outcome of a retried operation. Contains whether it succeeded, the
 * result value (if successful), the number of attempts made, and the last exception (if failed).
 *
 * @param <T> the type of the result value
 */
public record RetryResult<T>(boolean succeeded, T value, int attempts, Exception lastException) {

  /**
   * Creates a successful result with the value and attempt count.
   *
   * @param <T> the type of the result value
   * @param value the value produced by the successful operation
   * @param attempts the number of attempts made before succeeding
   * @return a new {@code RetryResult} representing success
   */
  public static <T> RetryResult<T> success(T value, int attempts) {
    return new RetryResult<>(true, value, attempts, null);
  }

  /**
   * Creates a failed result with the attempt count and last exception.
   *
   * @param <T> the type of the result value
   * @param attempts the number of attempts made before failing
   * @param lastException the last exception encountered during execution
   * @return a new {@code RetryResult} representing failure
   */
  public static <T> RetryResult<T> failure(int attempts, Exception lastException) {
    return new RetryResult<>(false, null, attempts, lastException);
  }

  /**
   * Returns the value if successful, or throws a {@link RuntimeException} wrapping the last
   * exception.
   *
   * @return the result value if the operation succeeded
   * @throws RuntimeException if the operation failed, wrapping the last exception encountered
   */
  public T getOrThrow() {
    if (succeeded) {
      return value;
    }
    throw new RuntimeException("All " + attempts + " attempts exhausted", lastException);
  }
}

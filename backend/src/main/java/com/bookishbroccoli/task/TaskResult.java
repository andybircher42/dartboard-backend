package com.bookishbroccoli.task;

import java.util.Optional;

/**
 * Immutable record capturing the outcome of a single task execution.
 *
 * @param succeeded whether the task completed successfully
 * @param resultCount the number of results produced by the task
 * @param error an optional error message when the task failed
 */
public record TaskResult(boolean succeeded, int resultCount, Optional<String> error) {

  /**
   * Creates a successful result with the given result count.
   *
   * @param resultCount the number of results produced
   * @return a successful {@link TaskResult}
   */
  public static TaskResult success(int resultCount) {
    return new TaskResult(true, resultCount, Optional.empty());
  }

  /**
   * Creates a successful result with zero results.
   *
   * @return a successful {@link TaskResult} with a result count of zero
   */
  public static TaskResult success() {
    return new TaskResult(true, 0, Optional.empty());
  }

  /**
   * Creates a failed result with the given error message.
   *
   * @param error a description of why the task failed
   * @return a failed {@link TaskResult}
   */
  public static TaskResult failure(String error) {
    return new TaskResult(false, 0, Optional.of(error));
  }
}

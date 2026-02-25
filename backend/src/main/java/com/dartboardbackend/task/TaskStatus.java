package com.dartboardbackend.task;

/** Lifecycle states for a queued task. */
public enum TaskStatus {
  PENDING("pending"),
  PROCESSING("processing"),
  COMPLETED("completed"),
  FAILED("failed");

  private final String value;

  TaskStatus(String value) {
    this.value = value;
  }

  /**
   * Returns the lowercase string representation of this status.
   *
   * @return the status value as a lowercase string
   */
  public String value() {
    return value;
  }

  /**
   * Returns {@code true} if the task reached a terminal state ({@link #COMPLETED} or {@link
   * #FAILED}).
   *
   * @return whether this status represents a terminal state
   */
  public boolean isDone() {
    return this == COMPLETED || this == FAILED;
  }

  /**
   * Returns {@code true} if the task is currently being processed.
   *
   * @return whether this status is {@link #PROCESSING}
   */
  public boolean isRunning() {
    return this == PROCESSING;
  }

  /**
   * Alias for {@link #fromString(String)}.
   *
   * @param status the status string to resolve
   * @return the matching {@link TaskStatus}, defaulting to {@link #PENDING}
   */
  public static TaskStatus resolve(String status) {
    return fromString(status);
  }

  /**
   * Parses a status string (case-insensitive), defaulting to {@link #PENDING} for {@code null} or
   * unrecognized values.
   *
   * @param status the status string to parse, may be {@code null}
   * @return the matching {@link TaskStatus}, or {@link #PENDING} if not found
   */
  public static TaskStatus fromString(String status) {
    if (status == null) {
      return PENDING;
    }
    for (TaskStatus s : values()) {
      if (s.value.equalsIgnoreCase(status) || s.name().equalsIgnoreCase(status)) {
        return s;
      }
    }
    return PENDING;
  }

  @Override
  public String toString() {
    return value;
  }
}

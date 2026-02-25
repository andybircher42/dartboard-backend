package com.dartboardbackend.job;

/**
 * Lifecycle states for a long-running job.
 *
 * <p>A job progresses from {@link #PENDING} to {@link #RUNNING} and finally reaches one of the
 * terminal states: {@link #SUCCEEDED} or {@link #FAILED}.
 */
public enum JobStatus {
  PENDING("pending"),
  RUNNING("running"),
  SUCCEEDED("succeeded"),
  FAILED("failed");

  private final String value;

  JobStatus(String value) {
    this.value = value;
  }

  /**
   * Returns the lowercase string representation of this status.
   *
   * @return the lowercase status value (e.g. {@code "running"}, {@code "failed"})
   */
  public String value() {
    return value;
  }

  /**
   * Returns {@code true} if the job has finished, meaning the status is either {@link #SUCCEEDED}
   * or {@link #FAILED}.
   *
   * @return {@code true} if this is a terminal status
   */
  public boolean isTerminal() {
    return this == SUCCEEDED || this == FAILED;
  }

  /**
   * Returns {@code true} if the job is currently running.
   *
   * @return {@code true} if this status is {@link #RUNNING}
   */
  public boolean isRunning() {
    return this == RUNNING;
  }

  /**
   * Parses a status string (case-insensitive) into a {@link JobStatus}. If the input is {@code
   * null} or does not match any known status, {@link #PENDING} is returned as the default.
   *
   * @param status the status string to parse, may be {@code null}
   * @return the matching {@link JobStatus}, or {@link #PENDING} if unrecognized or {@code null}
   */
  public static JobStatus fromString(String status) {
    if (status == null) {
      return PENDING;
    }
    for (JobStatus s : values()) {
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

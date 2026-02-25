package com.dartboardbackend.service;

/**
 * Enum representing the possible states of an Apify actor run.
 *
 * <p>Each constant maps to the status string returned by the Apify REST API. Use {@link
 * #fromString(String)} to parse an API response value, and {@link #isTerminal()} to check whether
 * the run has finished.
 */
public enum ApifyRunStatus {
  UNKNOWN("UNKNOWN"),
  RUNNING("RUNNING"),
  SUCCEEDED("SUCCEEDED"),
  FAILED("FAILED"),
  ABORTED("ABORTED"),
  TIMED_OUT("TIMED-OUT"),
  READY("READY");

  private final String value;

  ApifyRunStatus(String value) {
    this.value = value;
  }

  /**
   * Returns the Apify API string representation of this status.
   *
   * @return the status string as used by the Apify REST API (e.g. {@code "RUNNING"})
   */
  public String value() {
    return value;
  }

  /**
   * Returns {@code true} if the run has reached a final state and will not change further.
   *
   * <p>Terminal states are {@link #SUCCEEDED}, {@link #FAILED}, {@link #ABORTED}, and {@link
   * #TIMED_OUT}.
   *
   * @return {@code true} if the status is terminal, {@code false} otherwise
   */
  public boolean isTerminal() {
    return this == SUCCEEDED || this == FAILED || this == ABORTED || this == TIMED_OUT;
  }

  /**
   * Parses an Apify API status string into the corresponding enum constant.
   *
   * <p>If the string does not match any known status, {@link #UNKNOWN} is returned.
   *
   * @param status the status string from the Apify API response
   * @return the matching {@code ApifyRunStatus}, or {@link #UNKNOWN} if unrecognized
   */
  public static ApifyRunStatus fromString(String status) {
    for (ApifyRunStatus s : values()) {
      if (s.value.equals(status)) {
        return s;
      }
    }
    return UNKNOWN;
  }

  @Override
  public String toString() {
    return value;
  }
}

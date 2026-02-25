package com.dartboardbackend.util;

import java.util.regex.Pattern;

/** Reusable field-validation helpers shared across filter classes. */
public final class ValidationUtils {

  private ValidationUtils() {}

  /** Letters and dots only — e.g. {@code "homeInfo.homeType"}. */
  public static final Pattern FIELD_PATH_PATTERN = Pattern.compile("[a-zA-Z.]+");

  /**
   * Validates that a value is non-null and, after trimming, fully matches the given pattern.
   *
   * @param value the value to validate (may be {@code null})
   * @param pattern the regex pattern the trimmed value must fully match
   * @throws IllegalArgumentException if {@code value} is null, blank after trimming, or contains
   *     characters not permitted by the pattern
   */
  public static void validateFieldPath(String value) {
    checkArgument(value != null, "field [%s] cannot be null", value);
    checkArgument(FIELD_PATH_PATTERN.matcher(value.trim()).matches(),"field must match [%s], got: %s", FIELD_PATH_PATTERN, value);
    }

  /**
   * Validates that a value is non-null and, after trimming, fully matches the given pattern.
   *
   * @param value the value to validate (may be {@code null})
   * @param pattern the regex pattern the trimmed value must fully match
   * @throws IllegalArgumentException if {@code value} is null, blank after trimming, or contains
   *     characters not permitted by the pattern
   */
  public static void validatePattern(String value, Pattern pattern) {
    checkArgument(value != null, "field cannot be null");
    checkArgument(pattern != null, "pattern cannot be null");
    checkArgument(pattern.matcher(value.trim()).matches(),"field must match [%s], got: %s", pattern, value);
    }

  public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkArgument(boolean condition, String format, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(format, args));
        }
    }
}

package com.dartboardbackend.util;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Null-safe string helpers for blank-checking, case conversion, and splitting. Replaces Apache
 * Commons Lang for the small subset of operations this project needs.
 *
 * <p>Every method treats {@code null} as "empty" — no method in this class throws
 * {@link NullPointerException}. Methods that return a {@code String} return {@code ""} for
 * {@code null} input (except {@link #defaultIfBlank}, which returns the caller-supplied default).
 */
public final class StringUtils {

  private StringUtils() {}

  /**
   * Returns {@code true} if the string is {@code null}, empty ({@code ""}), or contains only
   * whitespace characters.
   *
   * <pre>{@code
   * isBlank(null)   // true
   * isBlank("")     // true
   * isBlank("  ")   // true
   * isBlank("hi")   // false
   * }</pre>
   *
   * @param s the string to check (may be {@code null})
   * @return {@code true} if blank or {@code null}
   */
  public static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  /**
   * Returns {@code true} if the string is non-{@code null} and contains at least one
   * non-whitespace character. This is the inverse of {@link #isBlank(String)}.
   *
   * <pre>{@code
   * isNotBlank(null)  // false
   * isNotBlank("")    // false
   * isNotBlank(" x ") // true
   * }</pre>
   *
   * @param s the string to check (may be {@code null})
   * @return {@code true} if not blank
   */
  public static boolean isNotBlank(String s) {
    return !isBlank(s);
  }

  /**
   * Returns the input string unchanged if it is not blank, otherwise returns an empty string.
   * Shorthand for {@code defaultIfBlank(s, "")}.
   *
   * <pre>{@code
   * getOrEmpty(null)    // ""
   * getOrEmpty("  ")    // ""
   * getOrEmpty("hello") // "hello"
   * }</pre>
   *
   * @param s the string to check (may be {@code null})
   * @return {@code s} if not blank, otherwise {@code ""}
   */
  public static String getOrEmpty(String s) {
    return defaultIfBlank(s, "");
  }

  /**
   * Returns the input string unchanged if it is not blank, otherwise returns
   * {@code defaultValue}. The default value itself may be {@code null}.
   *
   * <pre>{@code
   * defaultIfBlank(null, "N/A")    // "N/A"
   * defaultIfBlank("  ", "N/A")    // "N/A"
   * defaultIfBlank("hello", "N/A") // "hello"
   * }</pre>
   *
   * @param s the string to check (may be {@code null})
   * @param defaultValue the fallback value to return when {@code s} is blank (may be {@code null})
   * @return {@code s} if not blank, otherwise {@code defaultValue}
   */
  public static String defaultIfBlank(String s, String defaultValue) {
    return isBlank(s) ? defaultValue : s;
  }

  /**
   * Trims leading/trailing whitespace and lower-cases the string. Equivalent to
   * {@link #toLower(String)} — kept as a convenience alias for call sites where "normalize" reads
   * more naturally (e.g. normalizing user input before a map lookup).
   *
   * <pre>{@code
   * normalize(null)          // ""
   * normalize("  HELLO  ")  // "hello"
   * }</pre>
   *
   * @param s the string to normalize (may be {@code null})
   * @return the trimmed, lower-cased string, or {@code ""} if {@code null}
   */
  public static String normalize(String s) {
    return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Trims leading/trailing whitespace and lower-cases the string. Useful as a method reference
   * (e.g. {@code stream.map(StringUtils::toLower)}).
   *
   * <pre>{@code
   * toLower(null)          // ""
   * toLower("  HELLO  ")  // "hello"
   * toLower("Already")    // "already"
   * }</pre>
   *
   * @param s the string to convert (may be {@code null})
   * @return the trimmed, lower-cased string, or {@code ""} if {@code null}
   */
  public static String toLower(String s) {
    return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Trims leading/trailing whitespace and upper-cases the string. Useful as a method reference
   * (e.g. {@code stream.map(StringUtils::toUpper)}).
   *
   * <pre>{@code
   * toUpper(null)          // ""
   * toUpper("  hello  ")  // "HELLO"
   * toUpper("Already")    // "ALREADY"
   * }</pre>
   *
   * @param s the string to convert (may be {@code null})
   * @return the trimmed, upper-cased string, or {@code ""} if {@code null}
   */
  public static String toUpper(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  /**
   * Splits a string by the given regex delimiter, following the same semantics as
   * {@link String#split(String)} (trailing empty strings are dropped). Returns an empty list if
   * the input is {@code null} or blank.
   *
   * <pre>{@code
   * split(null, ",")      // []
   * split("", ",")        // []
   * split("a,b,c", ",")   // ["a", "b", "c"]
   * split("a.b", "\\.")   // ["a", "b"]
   * }</pre>
   *
   * @param s the string to split (may be {@code null})
   * @param regex the delimiting regular expression
   * @return an immutable list of tokens, or an empty list if {@code s} is {@code null} or blank
   */
  public static List<String> split(String s, String regex) {
    if (isBlank(s)) {
      return List.of();
    }
    return List.of(s.split(regex));
  }

  /**
   * Extracts the text value from a {@link JsonNode}, returning {@link Optional#empty()} when the
   * node is {@code null}, a Jackson "missing" node, not a textual node, or contains only
   * whitespace.
   *
   * <pre>{@code
   * toString(null)                          // Optional.empty()
   * toString(MissingNode.getInstance())     // Optional.empty()
   * toString(IntNode.valueOf(42))           // Optional.empty()
   * toString(TextNode.valueOf("  "))        // Optional.empty()
   * toString(TextNode.valueOf("hello"))     // Optional.of("hello")
   * }</pre>
   *
   * @param json the JSON node to extract text from (may be {@code null})
   * @return an {@link Optional} containing the text value, or empty if the node is absent,
   *     non-textual, or blank
   */
  public static Optional<String> toString(JsonNode json) {
    if (json == null || json.isMissingNode() || !json.isTextual()) {
      return Optional.empty();
    }
    String text = json.asText();
    return isBlank(text) ? Optional.empty() : Optional.of(text);
  }
}

package com.dartboardbackend.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ValidationUtilsTest {

  private static final Pattern LETTERS_ONLY = Pattern.compile("[a-zA-Z]+");
  private static final Pattern LETTERS_AND_DOTS = Pattern.compile("[a-zA-Z.]+");
  private static final Pattern LETTERS_AND_UNDERSCORES = Pattern.compile("[a-zA-Z_]+");

  // ==================== checkArgument ====================

  @Test
  void checkArgument_trueCondition_doesNotThrow() {
    assertDoesNotThrow(() -> ValidationUtils.checkArgument(true, "should not throw"));
  }

  @Test
  void checkArgument_falseCondition_throws() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.checkArgument(false, "expected failure"));

    assertEquals("expected failure", ex.getMessage());
  }

  @Test
  void checkArgument_format_trueCondition_doesNotThrow() {
    assertDoesNotThrow(() -> ValidationUtils.checkArgument(true, "value is %s", "ok"));
  }

  @Test
  void checkArgument_format_falseCondition_formatsMessage() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.checkArgument(false, "min [%s] must be less than max [%s]", 10, 5));

    assertEquals("min [10] must be less than max [5]", ex.getMessage());
  }

  // ==================== validateFieldPath ====================

  @Test
  void validateFieldPath_simplePath_doesNotThrow() {
    assertDoesNotThrow(() -> ValidationUtils.validateFieldPath("price"));
  }

  @Test
  void validateFieldPath_dottedPath_doesNotThrow() {
    assertDoesNotThrow(() -> ValidationUtils.validateFieldPath("homeInfo.homeType"));
  }

  @Test
  void validateFieldPath_null_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> ValidationUtils.validateFieldPath(null));
  }

  @Test
  void validateFieldPath_numbers_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> ValidationUtils.validateFieldPath("field123"));
  }

  @Test
  void validateFieldPath_specialChars_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> ValidationUtils.validateFieldPath("field@name"));
  }

  // ==================== FIELD_PATH_PATTERN ====================

  @Test
  void fieldPathPattern_acceptsSimplePath() {
    assertDoesNotThrow(() ->
        ValidationUtils.validatePattern("price", ValidationUtils.FIELD_PATH_PATTERN));
  }

  @Test
  void fieldPathPattern_acceptsDottedPath() {
    assertDoesNotThrow(() ->
        ValidationUtils.validatePattern("homeInfo.homeType", ValidationUtils.FIELD_PATH_PATTERN));
  }

  @Test
  void fieldPathPattern_rejectsNumbers() {
    assertThrows(IllegalArgumentException.class, () ->
        ValidationUtils.validatePattern("field123", ValidationUtils.FIELD_PATH_PATTERN));
  }

  @Test
  void fieldPathPattern_rejectsSpecialChars() {
    assertThrows(IllegalArgumentException.class, () ->
        ValidationUtils.validatePattern("field@name", ValidationUtils.FIELD_PATH_PATTERN));
  }

  // ==================== validateField ====================

  @Test
  void validValue_doesNotThrow() {
    assertDoesNotThrow(() -> ValidationUtils.validatePattern("hello", LETTERS_ONLY));
  }

  @Test
  void validValue_withDots() {
    assertDoesNotThrow(() -> ValidationUtils.validatePattern("details.category", LETTERS_AND_DOTS));
  }

  @Test
  void validValue_withUnderscores() {
    assertDoesNotThrow(
        () -> ValidationUtils.validatePattern("SINGLE_FAMILY", LETTERS_AND_UNDERSCORES));
  }

  @Test
  void validValue_trimsBeforeMatching() {
    assertDoesNotThrow(() -> ValidationUtils.validatePattern("  hello  ", LETTERS_ONLY));
  }

  @Test
  void null_throws() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.validatePattern(null, LETTERS_ONLY));

    assertTrue(ex.getMessage().contains("null"));
  }

  @Test
  void empty_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ValidationUtils.validatePattern("", LETTERS_ONLY));
  }

  @Test
  void whitespaceOnly_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ValidationUtils.validatePattern("   ", LETTERS_ONLY));
  }

  @Test
  void invalidChars_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ValidationUtils.validatePattern("hello123", LETTERS_ONLY));
  }

  @Test
  void dotNotAllowedByLettersOnly_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ValidationUtils.validatePattern("a.b", LETTERS_ONLY));
  }

  @Test
  void messageIncludesPatternAndValue() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.validatePattern("bad!value", LETTERS_ONLY));

    assertTrue(ex.getMessage().contains(LETTERS_ONLY.toString()));
    assertTrue(ex.getMessage().contains("bad!value"));
  }
}

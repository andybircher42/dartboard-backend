package com.dartboardbackend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

class StringUtilsTest {

  // ── isBlank ──

  @Nested
  class IsBlankTests {

    @Test
    void null_isBlank() {
      assertTrue(StringUtils.isBlank(null));
    }

    @Test
    void empty_isBlank() {
      assertTrue(StringUtils.isBlank(""));
    }

    @Test
    void whitespaceOnly_isBlank() {
      assertTrue(StringUtils.isBlank("   "));
    }

    @Test
    void tab_isBlank() {
      assertTrue(StringUtils.isBlank("\t\n"));
    }

    @Test
    void nonBlank_isNotBlank() {
      assertFalse(StringUtils.isBlank("hello"));
    }

    @Test
    void whitespaceWithContent_isNotBlank() {
      assertFalse(StringUtils.isBlank("  x  "));
    }

    @Test
    void singleChar_isNotBlank() {
      assertFalse(StringUtils.isBlank("a"));
    }
  }

  // ── isNotBlank ──

  @Nested
  class IsNotBlankTests {

    @Test
    void null_isNotNotBlank() {
      assertFalse(StringUtils.isNotBlank(null));
    }

    @Test
    void empty_isNotNotBlank() {
      assertFalse(StringUtils.isNotBlank(""));
    }

    @Test
    void whitespaceOnly_isNotNotBlank() {
      assertFalse(StringUtils.isNotBlank("   "));
    }

    @Test
    void nonBlank_isNotBlank() {
      assertTrue(StringUtils.isNotBlank("hello"));
    }

    @Test
    void whitespaceWithContent_isNotBlank() {
      assertTrue(StringUtils.isNotBlank("  x  "));
    }
  }

  // ── getOrEmpty ──

  @Nested
  class GetOrEmptyTests {

    @Test
    void null_returnsEmpty() {
      assertEquals("", StringUtils.getOrEmpty(null));
    }

    @Test
    void empty_returnsEmpty() {
      assertEquals("", StringUtils.getOrEmpty(""));
    }

    @Test
    void whitespace_returnsEmpty() {
      assertEquals("", StringUtils.getOrEmpty("   "));
    }

    @Test
    void nonBlank_returnsOriginal() {
      assertEquals("hello", StringUtils.getOrEmpty("hello"));
    }

    @Test
    void whitespaceWithContent_returnsOriginal() {
      assertEquals("  x  ", StringUtils.getOrEmpty("  x  "));
    }
  }

  // ── defaultIfBlank ──

  @Nested
  class DefaultIfBlankTests {

    @Test
    void null_returnsDefault() {
      assertEquals("fallback", StringUtils.defaultIfBlank(null, "fallback"));
    }

    @Test
    void empty_returnsDefault() {
      assertEquals("fallback", StringUtils.defaultIfBlank("", "fallback"));
    }

    @Test
    void whitespace_returnsDefault() {
      assertEquals("fallback", StringUtils.defaultIfBlank("   ", "fallback"));
    }

    @Test
    void nonBlank_returnsOriginal() {
      assertEquals("hello", StringUtils.defaultIfBlank("hello", "fallback"));
    }

    @Test
    void defaultCanBeNull() {
      assertNull(StringUtils.defaultIfBlank(null, null));
    }

    @Test
    void nonBlank_defaultIgnored() {
      assertEquals("keep", StringUtils.defaultIfBlank("keep", null));
    }
  }

  // ── normalize ──

  @Nested
  class NormalizeTests {

    @Test
    void null_returnsEmpty() {
      assertEquals("", StringUtils.normalize(null));
    }

    @Test
    void empty_returnsEmpty() {
      assertEquals("", StringUtils.normalize(""));
    }

    @Test
    void whitespaceOnly_returnsEmpty() {
      assertEquals("", StringUtils.normalize("   "));
    }

    @Test
    void trims() {
      assertEquals("hello", StringUtils.normalize("  hello  "));
    }

    @Test
    void lowercases() {
      assertEquals("hello", StringUtils.normalize("HELLO"));
    }

    @Test
    void trimsAndLowercases() {
      assertEquals("hello world", StringUtils.normalize("  Hello World  "));
    }

    @Test
    void alreadyNormalized() {
      assertEquals("abc", StringUtils.normalize("abc"));
    }
  }

  // ── toLower ──

  @Nested
  class ToLowerTests {

    @Test
    void null_returnsEmpty() {
      assertEquals("", StringUtils.toLower(null));
    }

    @Test
    void empty_returnsEmpty() {
      assertEquals("", StringUtils.toLower(""));
    }

    @Test
    void whitespaceOnly_returnsEmpty() {
      assertEquals("", StringUtils.toLower("   "));
    }

    @Test
    void trims() {
      assertEquals("hello", StringUtils.toLower("  hello  "));
    }

    @Test
    void lowercases() {
      assertEquals("hello", StringUtils.toLower("HELLO"));
    }

    @Test
    void trimsAndLowercases() {
      assertEquals("hello world", StringUtils.toLower("  Hello World  "));
    }

    @Test
    void alreadyLowercase() {
      assertEquals("abc", StringUtils.toLower("abc"));
    }
  }

  // ── toUpper ──

  @Nested
  class ToUpperTests {

    @Test
    void null_returnsEmpty() {
      assertEquals("", StringUtils.toUpper(null));
    }

    @Test
    void empty_returnsEmpty() {
      assertEquals("", StringUtils.toUpper(""));
    }

    @Test
    void whitespaceOnly_returnsEmpty() {
      assertEquals("", StringUtils.toUpper("   "));
    }

    @Test
    void trims() {
      assertEquals("HELLO", StringUtils.toUpper("  HELLO  "));
    }

    @Test
    void uppercases() {
      assertEquals("HELLO", StringUtils.toUpper("hello"));
    }

    @Test
    void trimsAndUppercases() {
      assertEquals("HELLO WORLD", StringUtils.toUpper("  Hello World  "));
    }

    @Test
    void alreadyUppercase() {
      assertEquals("ABC", StringUtils.toUpper("ABC"));
    }
  }

  // ── split ──

  @Nested
  class SplitTests {

    @Test
    void null_returnsEmptyList() {
      assertEquals(List.of(), StringUtils.split(null, ","));
    }

    @Test
    void empty_returnsEmptyList() {
      assertEquals(List.of(), StringUtils.split("", ","));
    }

    @Test
    void whitespaceOnly_returnsEmptyList() {
      assertEquals(List.of(), StringUtils.split("   ", ","));
    }

    @Test
    void singleValue_noDelimiter() {
      assertEquals(List.of("hello"), StringUtils.split("hello", ","));
    }

    @Test
    void multipleValues_commaSeparated() {
      assertEquals(List.of("a", "b", "c"), StringUtils.split("a,b,c", ","));
    }

    @Test
    void regexDelimiter() {
      assertEquals(List.of("one", "two", "three"), StringUtils.split("one.two.three", "\\."));
    }

    @Test
    void trailingDelimiter_matchesStringSplitBehavior() {
      // String.split drops trailing empty strings by default
      assertEquals(List.of("a", "b"), StringUtils.split("a,b,", ","));
    }

    @Test
    void leadingDelimiter_matchesStringSplitBehavior() {
      assertEquals(List.of("", "a", "b"), StringUtils.split(",a,b", ","));
    }

    @Test
    void consecutiveDelimiters_matchesStringSplitBehavior() {
      assertEquals(List.of("a", "", "b"), StringUtils.split("a,,b", ","));
    }

    @Test
    void pipeDelimiter() {
      assertEquals(List.of("x", "y"), StringUtils.split("x|y", "\\|"));
    }
  }

  // ── toString (JsonNode) ──

  @Nested
  class ToStringTests {

    @Test
    void null_returnsEmpty() {
      assertEquals(Optional.empty(), StringUtils.toString(null));
    }

    @Test
    void missingNode_returnsEmpty() {
      assertEquals(Optional.empty(), StringUtils.toString(MissingNode.getInstance()));
    }

    @Test
    void nullNode_returnsEmpty() {
      assertEquals(Optional.empty(), StringUtils.toString(NullNode.getInstance()));
    }

    @Test
    void intNode_returnsEmpty() {
      assertEquals(Optional.empty(), StringUtils.toString(IntNode.valueOf(42)));
    }

    @Test
    void objectNode_returnsEmpty() {
      assertEquals(Optional.empty(), StringUtils.toString(new ObjectMapper().createObjectNode()));
    }

    @Test
    void emptyTextNode_returnsEmpty() {
      assertEquals(Optional.empty(), StringUtils.toString(TextNode.valueOf("")));
    }

    @Test
    void whitespaceOnlyTextNode_returnsEmpty() {
      assertEquals(Optional.empty(), StringUtils.toString(TextNode.valueOf("   ")));
    }

    @Test
    void textNode_returnsValue() {
      assertEquals(Optional.of("hello"), StringUtils.toString(TextNode.valueOf("hello")));
    }

    @Test
    void textNode_preservesWhitespaceContent() {
      assertEquals(Optional.of("  hello  "), StringUtils.toString(TextNode.valueOf("  hello  ")));
    }

    @Test
    void textNode_preservesCase() {
      assertEquals(Optional.of("HELLO"), StringUtils.toString(TextNode.valueOf("HELLO")));
    }
  }
}

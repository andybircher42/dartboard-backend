package com.dartboardbackend.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CategoryFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final Map<String, List<String>> TYPE_ALIASES =
      Map.of(
          "houses", List.of("SINGLE_FAMILY"),
          "condos", List.of("CONDO", "APARTMENT"),
          "townhomes", List.of("TOWNHOUSE"));

  // ==================== constructor (Set) ====================

  @Test
  void setConstructor_normalizesFieldPath() {
    CategoryFilter rule = new CategoryFilter("  Details.Category  ", Set.of("A"));

    assertEquals("details.category", rule.getFieldPath());
  }

  @Test
  void setConstructor_upperCasesAllowedValues() {
    CategoryFilter rule = new CategoryFilter("field", Set.of("single_family", "Condo"));

    assertEquals(Set.of("SINGLE_FAMILY", "CONDO"), rule.getAllowedValues());
  }

  @Test
  void setConstructor_emptyAllowedValues() {
    CategoryFilter rule = new CategoryFilter("field", Set.of());

    assertTrue(rule.getAllowedValues().isEmpty());
  }

  @Test
  void setConstructor_nullFieldPath_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new CategoryFilter(null, Set.of("A")));
  }

  @Test
  void setConstructor_emptyFieldPath_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new CategoryFilter("", Set.of("A")));
  }

  @Test
  void setConstructor_fieldPathWithNumbers_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new CategoryFilter("field1", Set.of("A")));
  }

  @Test
  void setConstructor_fieldPathWithSpecialChars_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CategoryFilter("field-path", Set.of("A")));
  }

  @Test
  void setConstructor_allowedValueWithNumbers_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CategoryFilter("field", Set.of("TYPE_1")));
  }

  @Test
  void setConstructor_allowedValueWithSpecialChars_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CategoryFilter("field", Set.of("SINGLE-FAMILY")));
  }

  @Test
  void setConstructor_nullAllowedValue_throws() {
    Set<String> values = new java.util.HashSet<>();
    values.add(null);
    assertThrows(IllegalArgumentException.class, () -> new CategoryFilter("field", values));
  }

  // ==================== constructor (alias map) ====================

  @Test
  void aliasConstructor_normalizesFieldPath() {
    CategoryFilter rule = new CategoryFilter("  Details.Category  ", "houses", TYPE_ALIASES);

    assertEquals("details.category", rule.getFieldPath());
  }

  @Test
  void aliasConstructor_expandsAndUpperCasesValues() {
    CategoryFilter rule = new CategoryFilter("field", "condos", TYPE_ALIASES);

    assertEquals(Set.of("CONDO", "APARTMENT"), rule.getAllowedValues());
  }

  @Test
  void aliasConstructor_multipleCategories() {
    CategoryFilter rule = new CategoryFilter("field", "houses,townhomes", TYPE_ALIASES);

    assertEquals(Set.of("SINGLE_FAMILY", "TOWNHOUSE"), rule.getAllowedValues());
  }

  @Test
  void aliasConstructor_nullCategories_emptyAllowedValues() {
    CategoryFilter rule = new CategoryFilter("field", null, TYPE_ALIASES);

    assertTrue(rule.getAllowedValues().isEmpty());
  }

  @Test
  void aliasConstructor_blankCategories_emptyAllowedValues() {
    CategoryFilter rule = new CategoryFilter("field", "  ", TYPE_ALIASES);

    assertTrue(rule.getAllowedValues().isEmpty());
  }

  @Test
  void aliasConstructor_unknownCategory_emptyAllowedValues() {
    CategoryFilter rule = new CategoryFilter("field", "unknown", TYPE_ALIASES);

    assertTrue(rule.getAllowedValues().isEmpty());
  }

  @Test
  void aliasConstructor_nullFieldPath_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CategoryFilter(null, "houses", TYPE_ALIASES));
  }

  @Test
  void aliasConstructor_emptyFieldPath_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CategoryFilter("", "houses", TYPE_ALIASES));
  }

  @Test
  void aliasConstructor_fieldPathWithNumbers_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CategoryFilter("field1", "houses", TYPE_ALIASES));
  }

  @Test
  void aliasConstructor_categoriesWithNumbers_treatedAsUnknown() {
    CategoryFilter rule = new CategoryFilter("field", "type1,type2", TYPE_ALIASES);

    assertTrue(rule.getAllowedValues().isEmpty());
  }

  @Test
  void aliasConstructor_categoriesWithSpecialChars_treatedAsUnknown() {
    CategoryFilter rule = new CategoryFilter("field", "houses;townhomes", TYPE_ALIASES);

    assertTrue(rule.getAllowedValues().isEmpty());
  }

  private ObjectNode item(String category) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("price", 0);
    if (category != null) {
      ObjectNode details = objectMapper.createObjectNode();
      details.put("category", category);
      node.set("details", details);
    }
    return node;
  }

  private ObjectNode itemTopLevelCategory(String category) {
    ObjectNode node = objectMapper.createObjectNode();
    if (category != null) {
      node.put("category", category);
    }
    return node;
  }

  // ==================== matches ====================

  @Test
  void explicitValues_matchesCaseInsensitive() {
    CategoryFilter rule = new CategoryFilter("details.category", Set.of("SINGLE_FAMILY"));

    assertTrue(rule.matches(item("SINGLE_FAMILY")));
    assertTrue(rule.matches(item("single_family")));
    assertFalse(rule.matches(item("CONDO")));
  }

  @Test
  void topLevelField_works() {
    CategoryFilter rule = new CategoryFilter("category", Set.of("ACTIVE"));

    assertTrue(rule.matches(itemTopLevelCategory("ACTIVE")));
    assertTrue(rule.matches(itemTopLevelCategory("active")));
    assertFalse(rule.matches(itemTopLevelCategory("SOLD")));
  }

  @Test
  void missingField_passes() {
    CategoryFilter rule = new CategoryFilter("details.category", Set.of("SINGLE_FAMILY"));

    assertTrue(rule.matches(item(null)));
  }

  @Test
  void emptyAllowedValues_matchesAll() {
    CategoryFilter rule = new CategoryFilter("details.category", Set.of());

    assertTrue(rule.matches(item("ANYTHING")));
  }

  @Test
  void aliasMap_expandsCategories() {
    CategoryFilter rule = new CategoryFilter("details.category", "houses", TYPE_ALIASES);

    assertTrue(rule.matches(item("SINGLE_FAMILY")));
    assertFalse(rule.matches(item("CONDO")));
    assertFalse(rule.matches(item("TOWNHOUSE")));
  }

  @Test
  void aliasMap_multipleCategories() {
    CategoryFilter rule = new CategoryFilter("details.category", "houses,townhomes", TYPE_ALIASES);

    assertTrue(rule.matches(item("SINGLE_FAMILY")));
    assertFalse(rule.matches(item("CONDO")));
    assertTrue(rule.matches(item("TOWNHOUSE")));
  }

  @Test
  void aliasMap_condosMatchesCondoAndApartment() {
    CategoryFilter rule = new CategoryFilter("details.category", "condos", TYPE_ALIASES);

    assertFalse(rule.matches(item("SINGLE_FAMILY")));
    assertTrue(rule.matches(item("CONDO")));
    assertTrue(rule.matches(item("APARTMENT")));
    assertFalse(rule.matches(item("TOWNHOUSE")));
  }

  @Test
  void aliasMap_allThreeTypes() {
    CategoryFilter rule =
        new CategoryFilter("details.category", "houses,condos,townhomes", TYPE_ALIASES);

    assertTrue(rule.matches(item("SINGLE_FAMILY")));
    assertTrue(rule.matches(item("CONDO")));
    assertTrue(rule.matches(item("APARTMENT")));
    assertTrue(rule.matches(item("TOWNHOUSE")));
  }

  @Test
  void aliasMap_unknownCategory_matchesAll() {
    CategoryFilter rule = new CategoryFilter("details.category", "unknown_type", TYPE_ALIASES);

    assertTrue(rule.matches(item("SINGLE_FAMILY")));
    assertTrue(rule.matches(item("CONDO")));
  }

  @Test
  void aliasMap_nullCategories_matchesAll() {
    CategoryFilter rule = new CategoryFilter("details.category", null, TYPE_ALIASES);

    assertTrue(rule.matches(item("ANYTHING")));
  }

  @Test
  void aliasMap_blankCategories_matchesAll() {
    CategoryFilter rule = new CategoryFilter("details.category", "  ", TYPE_ALIASES);

    assertTrue(rule.matches(item("ANYTHING")));
  }

  // ==================== validateField ====================

  @Nested
  class ValidateFieldTests {

    private static final Pattern LETTERS_ONLY = Pattern.compile("[a-zA-Z]+");
    private static final Pattern LETTERS_AND_DOTS = Pattern.compile("[a-zA-Z.]+");
    private static final Pattern LETTERS_AND_UNDERSCORES = Pattern.compile("[a-zA-Z_]+");

    @Test
    void validValue_doesNotThrow() {
      assertDoesNotThrow(() -> CategoryFilter.validateField("hello", LETTERS_ONLY));
    }

    @Test
    void validValue_withDots() {
      assertDoesNotThrow(() -> CategoryFilter.validateField("details.category", LETTERS_AND_DOTS));
    }

    @Test
    void validValue_withUnderscores() {
      assertDoesNotThrow(
          () -> CategoryFilter.validateField("SINGLE_FAMILY", LETTERS_AND_UNDERSCORES));
    }

    @Test
    void validValue_trimsBeforeMatching() {
      assertDoesNotThrow(() -> CategoryFilter.validateField("  hello  ", LETTERS_ONLY));
    }

    @Test
    void null_throws() {
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> CategoryFilter.validateField(null, LETTERS_ONLY));

      assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    void empty_throws() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CategoryFilter.validateField("", LETTERS_ONLY));
    }

    @Test
    void whitespaceOnly_throws() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CategoryFilter.validateField("   ", LETTERS_ONLY));
    }

    @Test
    void invalidChars_throws() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CategoryFilter.validateField("hello123", LETTERS_ONLY));
    }

    @Test
    void dotNotAllowedByLettersOnly_throws() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CategoryFilter.validateField("a.b", LETTERS_ONLY));
    }

    @Test
    void messageIncludesPatternAndValue() {
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> CategoryFilter.validateField("bad!value", LETTERS_ONLY));

      assertTrue(ex.getMessage().contains(LETTERS_ONLY.toString()));
      assertTrue(ex.getMessage().contains("bad!value"));
    }
  }

  // ==================== expandAliases ====================

  @Test
  void expandAliases_singleKey() {
    Set<String> result = CategoryFilter.expandAliases("houses", TYPE_ALIASES);

    assertEquals(Set.of("SINGLE_FAMILY"), result);
  }

  @Test
  void expandAliases_multipleKeys() {
    Set<String> result = CategoryFilter.expandAliases("houses,condos", TYPE_ALIASES);

    assertEquals(Set.of("SINGLE_FAMILY", "CONDO", "APARTMENT"), result);
  }

  @Test
  void expandAliases_unknownKey_returnsEmpty() {
    Set<String> result = CategoryFilter.expandAliases("unknown", TYPE_ALIASES);

    assertTrue(result.isEmpty());
  }

  @Test
  void expandAliases_null_returnsEmpty() {
    Set<String> result = CategoryFilter.expandAliases(null, TYPE_ALIASES);

    assertTrue(result.isEmpty());
  }

  @Test
  void expandAliases_blank_returnsEmpty() {
    Set<String> result = CategoryFilter.expandAliases("  ", TYPE_ALIASES);

    assertTrue(result.isEmpty());
  }

  @Test
  void expandAliases_trimming_works() {
    Set<String> result = CategoryFilter.expandAliases(" houses , townhomes ", TYPE_ALIASES);

    assertEquals(Set.of("SINGLE_FAMILY", "TOWNHOUSE"), result);
  }
}

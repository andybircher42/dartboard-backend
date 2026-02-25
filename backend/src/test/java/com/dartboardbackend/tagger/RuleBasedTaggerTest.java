package com.dartboardbackend.tagger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleBasedTaggerTest {

  private static final Tag LUXURY = Tag.of("luxury", "price", "Luxury");
  private static final Tag BUDGET = Tag.of("budget", "price", "Budget");
  private static final Tag LARGE = Tag.of("large", "size", "Large");
  private static final Tag POOL = Tag.of("pool", "amenity", "Has Pool");

  private RuleBasedTagger tagger;

  @BeforeEach
  void setUp() {
    tagger =
        new RuleBasedTagger(
            List.of(
                new TestTagRule(
                    LUXURY,
                    item -> {
                      Object price = item.get("price");
                      return price instanceof Number && ((Number) price).intValue() > 1_000_000;
                    },
                    "strict"),
                new TestTagRule(
                    BUDGET,
                    item -> {
                      Object price = item.get("price");
                      return price instanceof Number && ((Number) price).intValue() < 200_000;
                    },
                    "strict"),
                new TestTagRule(
                    LARGE,
                    item -> {
                      Object sqft = item.get("sqft");
                      return sqft instanceof Number && ((Number) sqft).intValue() > 3000;
                    },
                    "relaxed"),
                new TestTagRule(POOL, item -> Boolean.TRUE.equals(item.get("hasPool")), "strict")));
  }

  @Test
  void tag_singleRuleMatch() {
    Map<String, Object> item = Map.of("price", 2_000_000);
    TagResult result = tagger.tag(item);

    assertTrue(result.hasTag(LUXURY));
    assertFalse(result.hasTag(BUDGET));
    assertEquals(1, result.tags().size());
  }

  @Test
  void tag_multipleRulesMatch() {
    Map<String, Object> item = Map.of("price", 100_000, "hasPool", true);
    TagResult result = tagger.tag(item);

    assertTrue(result.hasTag(BUDGET));
    assertTrue(result.hasTag(POOL));
    assertEquals(2, result.tags().size());
  }

  @Test
  void tag_noMatch() {
    Map<String, Object> item = Map.of("price", 500_000);
    TagResult result = tagger.tag(item);

    assertTrue(result.tags().isEmpty());
  }

  @Test
  void tag_mixedConfidence() {
    Map<String, Object> item = Map.of("price", 2_000_000, "sqft", 4000);
    TagResult result = tagger.tag(item);

    assertEquals(2, result.tags().size());
    assertEquals(List.of(LUXURY), result.strict());
    assertEquals(List.of(LARGE), result.relaxed());
  }

  @Test
  void tag_emptyItem() {
    TagResult result = tagger.tag(Map.of());

    assertTrue(result.tags().isEmpty());
    assertTrue(result.confidences().isEmpty());
  }

  @Test
  void tagAll_batchProcessing() {
    List<Map<String, Object>> items =
        List.of(Map.of("price", 2_000_000), Map.of("price", 100_000), Map.of("price", 500_000));

    List<TagResult> results = tagger.tagAll(items);

    assertEquals(3, results.size());
    assertTrue(results.get(0).hasTag(LUXURY));
    assertTrue(results.get(1).hasTag(BUDGET));
    assertTrue(results.get(2).tags().isEmpty());
  }

  @Test
  void tagAll_emptyList() {
    List<TagResult> results = tagger.tagAll(List.of());

    assertTrue(results.isEmpty());
  }

  @Test
  void getRegisteredCategories() {
    Set<String> categories = tagger.getRegisteredCategories();

    assertEquals(3, categories.size());
    assertTrue(categories.contains("price"));
    assertTrue(categories.contains("size"));
    assertTrue(categories.contains("amenity"));
  }

  @Test
  void getTagsByCategory_found() {
    List<Tag> priceTags = tagger.getTagsByCategory("price");

    assertEquals(2, priceTags.size());
    assertTrue(priceTags.contains(LUXURY));
    assertTrue(priceTags.contains(BUDGET));
  }

  @Test
  void getTagsByCategory_notFound() {
    List<Tag> tags = tagger.getTagsByCategory("nonexistent");

    assertTrue(tags.isEmpty());
  }

  @Test
  void noRules_emptyTagger() {
    RuleBasedTagger emptyTagger = new RuleBasedTagger(List.of());
    TagResult result = emptyTagger.tag(Map.of("price", 999));

    assertTrue(result.tags().isEmpty());
    assertTrue(emptyTagger.getRegisteredCategories().isEmpty());
  }

  // --- Test helper ---

  private static class TestTagRule implements TagRule {
    private final Tag tag;
    private final java.util.function.Predicate<Map<String, Object>> predicate;
    private final String confidence;

    TestTagRule(
        Tag tag, java.util.function.Predicate<Map<String, Object>> predicate, String confidence) {
      this.tag = tag;
      this.predicate = predicate;
      this.confidence = confidence;
    }

    @Override
    public Tag getTag() {
      return tag;
    }

    @Override
    public boolean matches(Map<String, Object> item) {
      return predicate.test(item);
    }

    @Override
    public String getConfidence() {
      return confidence;
    }
  }
}

package com.bookishbroccoli.tagger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagBrowseServiceTest {

  private static final Tag LUXURY = Tag.of("luxury", "price", "Luxury");
  private static final Tag BUDGET = Tag.of("budget", "price", "Budget");
  private static final Tag LARGE = Tag.of("large", "size", "Large");

  private InMemoryTagIndex tagIndex;
  private TagBrowseService service;

  @BeforeEach
  void setUp() {
    tagIndex = new InMemoryTagIndex();
    service = new TagBrowseService(tagIndex);
  }

  @Test
  void browse_returnsAllCategories() {
    tagIndex.index(
        "item-1",
        new TagResult(List.of(LUXURY, LARGE), Map.of("luxury", "strict", "large", "relaxed")));
    tagIndex.index("item-2", new TagResult(List.of(BUDGET), Map.of("budget", "strict")));

    List<TagBrowseResult> results = service.browse();

    assertEquals(2, results.size());
  }

  @Test
  void browse_includesCountsAndSamples() {
    tagIndex.index("item-1", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
    tagIndex.index("item-2", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
    tagIndex.index("item-3", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));

    List<TagBrowseResult> results = service.browse();
    TagBrowseResult priceResult =
        results.stream().filter(r -> r.category().equals("price")).findFirst().orElseThrow();

    assertEquals(1, priceResult.tags().size());
    TagBrowseResult.TagSummary summary = priceResult.tags().get(0);
    assertEquals(LUXURY, summary.tag());
    assertEquals(3, summary.count());
    assertEquals(3, summary.sampleIds().size());
  }

  @Test
  void browse_emptyIndex() {
    List<TagBrowseResult> results = service.browse();

    assertTrue(results.isEmpty());
  }

  @Test
  void browseCategory_returnsTagsInCategory() {
    tagIndex.index("item-1", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
    tagIndex.index("item-2", new TagResult(List.of(BUDGET), Map.of("budget", "strict")));
    tagIndex.index("item-3", new TagResult(List.of(LARGE), Map.of("large", "relaxed")));

    TagBrowseResult result = service.browseCategory("price");

    assertEquals("price", result.category());
    assertEquals(2, result.tags().size());
  }

  @Test
  void browseCategory_unknownCategory_empty() {
    TagBrowseResult result = service.browseCategory("nonexistent");

    assertEquals("nonexistent", result.category());
    assertTrue(result.tags().isEmpty());
  }

  @Test
  void getItemsByTag_returnsMatchingItems() {
    tagIndex.index("item-1", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
    tagIndex.index("item-2", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));

    Set<String> items = service.getItemsByTag("luxury", 10);

    assertEquals(2, items.size());
    assertTrue(items.contains("item-1"));
    assertTrue(items.contains("item-2"));
  }

  @Test
  void getItemsByTag_unknownTag_empty() {
    Set<String> items = service.getItemsByTag("nonexistent", 10);

    assertTrue(items.isEmpty());
  }

  @Test
  void browse_maxSamplesCap() {
    for (int i = 0; i < 10; i++) {
      tagIndex.index("item-" + i, new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
    }

    List<TagBrowseResult> results = service.browse();
    TagBrowseResult.TagSummary summary = results.get(0).tags().get(0);

    assertEquals(10, summary.count());
    assertTrue(summary.sampleIds().size() <= 5, "samples should be capped at MAX_SAMPLES=5");
  }

  @Test
  void getItemsByTag_respectsLimit() {
    for (int i = 0; i < 10; i++) {
      tagIndex.index("item-" + i, new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
    }

    Set<String> items = service.getItemsByTag("luxury", 3);

    assertEquals(3, items.size());
  }
}

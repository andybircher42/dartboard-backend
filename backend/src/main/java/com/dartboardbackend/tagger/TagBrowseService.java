package com.dartboardbackend.tagger;

import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Service providing tag browsing capabilities. Assembles {@link TagBrowseResult}s by combining tag
 * index data with item counts and samples.
 */
@Service
public class TagBrowseService {

  private final TagIndex tagIndex;

  private static final int MAX_SAMPLES = 5;

  /**
   * Creates a new browse service backed by the given tag index.
   *
   * @param tagIndex the tag index to query
   */
  public TagBrowseService(TagIndex tagIndex) {
    this.tagIndex = tagIndex;
  }

  /**
   * Returns browse results for all categories.
   *
   * @return a list of {@link TagBrowseResult}s, one per category
   */
  public List<TagBrowseResult> browse() {
    Map<String, Set<Tag>> byCategory = tagIndex.getTagsByCategory();
    Map<Tag, Integer> counts = tagIndex.getTagCounts();

    List<TagBrowseResult> results = new ArrayList<>();
    for (Map.Entry<String, Set<Tag>> entry : byCategory.entrySet()) {
      results.add(buildBrowseResult(entry.getKey(), entry.getValue(), counts));
    }
    return results;
  }

  /**
   * Returns the browse result for a single category.
   *
   * @param category the category to browse
   * @return a {@link TagBrowseResult} for the specified category
   */
  public TagBrowseResult browseCategory(String category) {
    Map<String, Set<Tag>> byCategory = tagIndex.getTagsByCategory();
    Set<Tag> tags = byCategory.getOrDefault(category, Set.of());
    Map<Tag, Integer> counts = tagIndex.getTagCounts();
    return buildBrowseResult(category, tags, counts);
  }

  /**
   * Looks up item IDs by tag name, returning up to {@code limit} results.
   *
   * @param tagName the name of the tag to look up
   * @param limit the maximum number of item IDs to return
   * @return a set of item IDs, or an empty set if the tag is not found
   */
  public Set<String> getItemsByTag(String tagName, int limit) {
    Map<Tag, Integer> counts = tagIndex.getTagCounts();
    for (Tag tag : counts.keySet()) {
      if (tag.getName().equals(tagName)) {
        return tagIndex.getItemsByTag(tag, limit);
      }
    }
    return Set.of();
  }

  /**
   * Assembles a {@link TagBrowseResult} from a category, its tags, and a counts map. For each tag,
   * retrieves the item count and a sample of item IDs up to {@link #MAX_SAMPLES}.
   */
  private TagBrowseResult buildBrowseResult(
      String category, Set<Tag> tags, Map<Tag, Integer> counts) {
    List<TagBrowseResult.TagSummary> summaries = new ArrayList<>();
    for (Tag tag : tags) {
      int count = counts.getOrDefault(tag, 0);
      Set<String> samples = tagIndex.getItemsByTag(tag, MAX_SAMPLES);
      summaries.add(new TagBrowseResult.TagSummary(tag, count, List.copyOf(samples)));
    }
    return new TagBrowseResult(category, summaries);
  }
}

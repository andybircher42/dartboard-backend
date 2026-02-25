package com.bookishbroccoli.tagger;

import java.util.List;
import java.util.Map;

/**
 * Immutable record capturing the tags assigned to a single item, along with the confidence level of
 * each assignment.
 *
 * @param tags the list of tags assigned to the item
 * @param confidences a map from tag name to confidence level ({@code "strict"} or {@code
 *     "relaxed"})
 */
public record TagResult(List<Tag> tags, Map<String, String> confidences) {

  /**
   * Returns only tags assigned with {@code "strict"} confidence.
   *
   * @return an unmodifiable list of strict-confidence tags
   */
  public List<Tag> strict() {
    return tags.stream().filter(t -> "strict".equals(confidences.get(t.getName()))).toList();
  }

  /**
   * Returns only tags assigned with {@code "relaxed"} confidence.
   *
   * @return an unmodifiable list of relaxed-confidence tags
   */
  public List<Tag> relaxed() {
    return tags.stream().filter(t -> "relaxed".equals(confidences.get(t.getName()))).toList();
  }

  /**
   * Returns {@code true} if the given tag was assigned.
   *
   * @param tag the tag to check for
   * @return {@code true} if the tag is present in this result
   */
  public boolean hasTag(Tag tag) {
    return tags.contains(tag);
  }
}

package com.dartboardbackend.tagger;

import java.util.Map;

/**
 * Rule that determines whether an item should be assigned a particular tag.
 *
 * <p>Implementations define the matching logic and the confidence level of the assignment.
 */
public interface TagRule {

  /**
   * Returns the tag this rule assigns when matched.
   *
   * @return the tag associated with this rule
   */
  Tag getTag();

  /**
   * Returns {@code true} if the given item matches this rule's criteria.
   *
   * @param item the item to evaluate, represented as a map of property names to values
   * @return {@code true} if the item matches, {@code false} otherwise
   */
  boolean matches(Map<String, Object> item);

  /**
   * Returns the confidence level of the match. Defaults to {@code "strict"}.
   *
   * @return {@code "strict"} or {@code "relaxed"}
   */
  default String getConfidence() {
    return "strict";
  }
}

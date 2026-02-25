package com.bookishbroccoli.tagger;

import java.util.List;

/**
 * Immutable record representing a browseable view of tags within a single category, including
 * per-tag item counts and sample item IDs.
 *
 * @param category the tag category
 * @param tags the list of tag summaries in this category
 */
public record TagBrowseResult(String category, List<TagSummary> tags) {

  /**
   * Summary of a single tag including its count and sample item IDs.
   *
   * @param tag the tag
   * @param count the number of items associated with the tag
   * @param sampleIds a sample of item IDs associated with the tag
   */
  public record TagSummary(Tag tag, int count, List<String> sampleIds) {}
}

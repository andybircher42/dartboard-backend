package com.bookishbroccoli.tagger;

import java.util.Map;
import java.util.Set;

/**
 * Index mapping tags to item IDs and vice versa. Supports querying items by tag and aggregating
 * tags by category.
 */
public interface TagIndex {

  /**
   * Indexes an item's tags, replacing any previous entry for the same item.
   *
   * @param itemId the unique identifier of the item
   * @param result the tagging result to index
   */
  void index(String itemId, TagResult result);

  /**
   * Removes an item from the index, cleaning up all tag associations.
   *
   * @param itemId the unique identifier of the item to remove
   */
  void remove(String itemId);

  /**
   * Returns up to {@code limit} item IDs associated with the given tag.
   *
   * @param tag the tag to look up
   * @param limit the maximum number of item IDs to return
   * @return a set of item IDs, or an empty set if the tag is not indexed
   */
  Set<String> getItemsByTag(Tag tag, int limit);

  /**
   * Returns all indexed tags grouped by category.
   *
   * @return a map from category name to the set of tags in that category
   */
  Map<String, Set<Tag>> getTagsByCategory();

  /**
   * Returns the number of items associated with each tag.
   *
   * @return a map from tag to its item count
   */
  Map<Tag, Integer> getTagCounts();
}

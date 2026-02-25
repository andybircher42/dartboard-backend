package com.bookishbroccoli.tagger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link TagIndex} using {@link ConcurrentHashMap} for bidirectional
 * tag-to-item mapping. Suitable for development and testing.
 */
@Repository
public class InMemoryTagIndex implements TagIndex {

  private final ConcurrentHashMap<String, Set<String>> tagToItems = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Set<Tag>> itemToTags = new ConcurrentHashMap<>();

  /** {@inheritDoc} */
  @Override
  public synchronized void index(String itemId, TagResult result) {
    remove(itemId);
    Set<Tag> tags = new LinkedHashSet<>(result.tags());
    itemToTags.put(itemId, tags);
    for (Tag tag : tags) {
      tagToItems.computeIfAbsent(tag.getName(), k -> ConcurrentHashMap.newKeySet()).add(itemId);
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void remove(String itemId) {
    Optional.ofNullable(itemToTags.remove(itemId))
        .ifPresent(
            tags -> {
              for (Tag tag : tags) {
                Set<String> items = tagToItems.get(tag.getName());
                if (items != null) {
                  items.remove(itemId);
                  if (items.isEmpty()) {
                    tagToItems.remove(tag.getName());
                  }
                }
              }
            });
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getItemsByTag(Tag tag, int limit) {
    return Optional.ofNullable(tagToItems.get(tag.getName()))
        .<Set<String>>map(
            items ->
                items.stream().limit(limit).collect(Collectors.toCollection(LinkedHashSet::new)))
        .orElse(Set.of());
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Set<Tag>> getTagsByCategory() {
    Map<String, Set<Tag>> result = new LinkedHashMap<>();
    for (Set<Tag> tags : itemToTags.values()) {
      for (Tag tag : tags) {
        result.computeIfAbsent(tag.getCategory(), k -> new LinkedHashSet<>()).add(tag);
      }
    }
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public Map<Tag, Integer> getTagCounts() {
    Map<Tag, Integer> counts = new LinkedHashMap<>();
    for (Map.Entry<String, Set<Tag>> entry : itemToTags.entrySet()) {
      for (Tag tag : entry.getValue()) {
        counts.merge(tag, 1, Integer::sum);
      }
    }
    return counts;
  }

  /** Removes all entries from both the tag-to-item and item-to-tag maps. */
  public void clear() {
    tagToItems.clear();
    itemToTags.clear();
  }
}

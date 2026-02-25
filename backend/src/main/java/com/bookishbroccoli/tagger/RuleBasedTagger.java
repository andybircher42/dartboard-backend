package com.bookishbroccoli.tagger;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Service that applies a list of {@link TagRule}s to categorize items. Each rule is evaluated
 * independently; an item can receive multiple tags.
 */
@Service
public class RuleBasedTagger {

  private final List<TagRule> rules;

  /**
   * Creates a new tagger with the given rules.
   *
   * @param rules the list of rules to evaluate when tagging items
   */
  public RuleBasedTagger(List<TagRule> rules) {
    this.rules = rules;
  }

  /**
   * Applies all rules to a single item, returning the matched tags and their confidences.
   *
   * @param item the item to tag, represented as a map of property names to values
   * @return a {@link TagResult} containing the matched tags and their confidence levels
   */
  public TagResult tag(Map<String, Object> item) {
    List<Tag> matchedTags = new ArrayList<>();
    Map<String, String> confidences = new LinkedHashMap<>();

    for (TagRule rule : rules) {
      if (rule.matches(item)) {
        matchedTags.add(rule.getTag());
        confidences.put(rule.getTag().getName(), rule.getConfidence());
      }
    }

    return new TagResult(List.copyOf(matchedTags), Map.copyOf(confidences));
  }

  /**
   * Applies all rules to each item in the list.
   *
   * @param items the list of items to tag
   * @return a list of {@link TagResult}s, one per input item, in the same order
   */
  public List<TagResult> tagAll(List<Map<String, Object>> items) {
    return items.stream().map(this::tag).toList();
  }

  /**
   * Returns the distinct set of categories covered by the registered rules, in encounter order.
   *
   * @return an ordered set of category names
   */
  public Set<String> getRegisteredCategories() {
    return rules.stream()
        .map(r -> r.getTag().getCategory())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Returns the distinct tags in the given category.
   *
   * @param category the category to filter by
   * @return a list of distinct tags belonging to the specified category
   */
  public List<Tag> getTagsByCategory(String category) {
    return rules.stream()
        .map(TagRule::getTag)
        .filter(t -> t.getCategory().equals(category))
        .distinct()
        .toList();
  }
}

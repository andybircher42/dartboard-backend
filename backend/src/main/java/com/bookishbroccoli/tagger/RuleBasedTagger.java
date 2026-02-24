package com.bookishbroccoli.tagger;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RuleBasedTagger {

	private final List<TagRule> rules;

	public RuleBasedTagger(List<TagRule> rules) {
		this.rules = rules;
	}

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

	public List<TagResult> tagAll(List<Map<String, Object>> items) {
		return items.stream()
				.map(this::tag)
				.toList();
	}

	public Set<String> getRegisteredCategories() {
		return rules.stream()
				.map(r -> r.getTag().getCategory())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public List<Tag> getTagsByCategory(String category) {
		return rules.stream()
				.map(TagRule::getTag)
				.filter(t -> t.getCategory().equals(category))
				.distinct()
				.toList();
	}
}

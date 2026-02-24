package com.bookishbroccoli.tagger;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TagBrowseService {

	private final TagIndex tagIndex;

	private static final int MAX_SAMPLES = 5;

	public TagBrowseService(TagIndex tagIndex) {
		this.tagIndex = tagIndex;
	}

	public List<TagBrowseResult> browse() {
		Map<String, Set<Tag>> byCategory = tagIndex.getTagsByCategory();
		Map<Tag, Integer> counts = tagIndex.getTagCounts();

		List<TagBrowseResult> results = new ArrayList<>();
		for (Map.Entry<String, Set<Tag>> entry : byCategory.entrySet()) {
			results.add(buildBrowseResult(entry.getKey(), entry.getValue(), counts));
		}
		return results;
	}

	public TagBrowseResult browseCategory(String category) {
		Map<String, Set<Tag>> byCategory = tagIndex.getTagsByCategory();
		Set<Tag> tags = byCategory.getOrDefault(category, Set.of());
		Map<Tag, Integer> counts = tagIndex.getTagCounts();
		return buildBrowseResult(category, tags, counts);
	}

	public Set<String> getItemsByTag(String tagName, int limit) {
		Map<Tag, Integer> counts = tagIndex.getTagCounts();
		for (Tag tag : counts.keySet()) {
			if (tag.getName().equals(tagName)) {
				return tagIndex.getItemsByTag(tag, limit);
			}
		}
		return Set.of();
	}

	private TagBrowseResult buildBrowseResult(String category, Set<Tag> tags, Map<Tag, Integer> counts) {
		List<TagBrowseResult.TagSummary> summaries = new ArrayList<>();
		for (Tag tag : tags) {
			int count = counts.getOrDefault(tag, 0);
			Set<String> samples = tagIndex.getItemsByTag(tag, MAX_SAMPLES);
			summaries.add(new TagBrowseResult.TagSummary(tag, count, List.copyOf(samples)));
		}
		return new TagBrowseResult(category, summaries);
	}
}

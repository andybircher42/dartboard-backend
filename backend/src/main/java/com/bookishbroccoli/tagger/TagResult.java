package com.bookishbroccoli.tagger;

import java.util.List;
import java.util.Map;

public record TagResult(List<Tag> tags, Map<String, String> confidences) {

	public List<Tag> strict() {
		return tags.stream()
				.filter(t -> "strict".equals(confidences.get(t.getName())))
				.toList();
	}

	public List<Tag> relaxed() {
		return tags.stream()
				.filter(t -> "relaxed".equals(confidences.get(t.getName())))
				.toList();
	}

	public boolean hasTag(Tag tag) {
		return tags.contains(tag);
	}
}

package com.bookishbroccoli.tagger;

import java.util.List;

public record TagBrowseResult(String category, List<TagSummary> tags) {

	public record TagSummary(Tag tag, int count, List<String> sampleIds) {
	}
}

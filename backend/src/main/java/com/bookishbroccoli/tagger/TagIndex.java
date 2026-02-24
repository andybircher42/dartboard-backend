package com.bookishbroccoli.tagger;

import java.util.Map;
import java.util.Set;

public interface TagIndex {

	void index(String itemId, TagResult result);

	void remove(String itemId);

	Set<String> getItemsByTag(Tag tag, int limit);

	Map<String, Set<Tag>> getTagsByCategory();

	Map<Tag, Integer> getTagCounts();
}

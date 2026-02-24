package com.bookishbroccoli.tagger;

import java.util.Map;

public interface TagRule {

	Tag getTag();

	boolean matches(Map<String, Object> item);

	default String getConfidence() {
		return "strict";
	}
}

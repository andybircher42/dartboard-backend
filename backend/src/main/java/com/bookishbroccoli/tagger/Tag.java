package com.bookishbroccoli.tagger;

import java.util.Objects;

public class Tag {

	private final String name;
	private final String category;
	private final String displayName;

	private Tag(String name, String category, String displayName) {
		this.name = name;
		this.category = category;
		this.displayName = displayName;
	}

	public static Tag of(String name, String category, String displayName) {
		return new Tag(name, category, displayName);
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public String getDisplayName() {
		return displayName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tag tag = (Tag) o;
		return Objects.equals(name, tag.name) && Objects.equals(category, tag.category);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, category);
	}

	@Override
	public String toString() {
		return category + ":" + name;
	}
}

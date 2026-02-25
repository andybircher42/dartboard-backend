package com.dartboardbackend.tagger;

import java.util.Objects;

/**
 * Immutable value object representing a tag with a name, category, and display name.
 *
 * <p>Two tags are considered equal if they share the same name and category.
 */
public class Tag {

  private final String name;
  private final String category;
  private final String displayName;

  private Tag(String name, String category, String displayName) {
    this.name = name;
    this.category = category;
    this.displayName = displayName;
  }

  /**
   * Factory method creating a new {@code Tag}.
   *
   * @param name the unique name of the tag within its category
   * @param category the category this tag belongs to
   * @param displayName the human-readable display name
   * @return a new {@code Tag} instance
   */
  public static Tag of(String name, String category, String displayName) {
    return new Tag(name, category, displayName);
  }

  /**
   * Returns the tag's unique name within its category.
   *
   * @return the tag name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the category this tag belongs to.
   *
   * @return the tag category
   */
  public String getCategory() {
    return category;
  }

  /**
   * Returns the human-readable display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Tag)) return false;
    Tag tag = (Tag) o;
    return Objects.equals(name, tag.name) && Objects.equals(category, tag.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, category);
  }

  @Override
  public String toString() {
    return String.format("%s:%s", category, name);
  }
}

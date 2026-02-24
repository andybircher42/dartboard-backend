package com.bookishbroccoli.tagger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTagIndexTest {

	private static final Tag LUXURY = Tag.of("luxury", "price", "Luxury");
	private static final Tag BUDGET = Tag.of("budget", "price", "Budget");
	private static final Tag LARGE = Tag.of("large", "size", "Large");

	private InMemoryTagIndex index;

	@BeforeEach
	void setUp() {
		index = new InMemoryTagIndex();
	}

	@Test
	void index_andGetItemsByTag() {
		TagResult result = new TagResult(List.of(LUXURY), Map.of("luxury", "strict"));
		index.index("item-1", result);

		Set<String> items = index.getItemsByTag(LUXURY, 10);

		assertEquals(Set.of("item-1"), items);
	}

	@Test
	void index_multipleItemsSameTag() {
		index.index("item-1", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
		index.index("item-2", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));

		Set<String> items = index.getItemsByTag(LUXURY, 10);

		assertEquals(2, items.size());
		assertTrue(items.contains("item-1"));
		assertTrue(items.contains("item-2"));
	}

	@Test
	void getItemsByTag_unknownTag_empty() {
		Set<String> items = index.getItemsByTag(BUDGET, 10);

		assertTrue(items.isEmpty());
	}

	@Test
	void getItemsByTag_respectsLimit() {
		for (int i = 0; i < 10; i++) {
			index.index("item-" + i, new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
		}

		Set<String> items = index.getItemsByTag(LUXURY, 3);

		assertEquals(3, items.size());
	}

	@Test
	void remove_removesFromBothIndexes() {
		index.index("item-1", new TagResult(List.of(LUXURY, LARGE),
				Map.of("luxury", "strict", "large", "relaxed")));
		index.index("item-2", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));

		index.remove("item-1");

		assertEquals(Set.of("item-2"), index.getItemsByTag(LUXURY, 10));
		assertTrue(index.getItemsByTag(LARGE, 10).isEmpty());
	}

	@Test
	void remove_nonexistent_noError() {
		index.remove("nonexistent");
		// no exception
	}

	@Test
	void index_replacesExistingItem() {
		index.index("item-1", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
		index.index("item-1", new TagResult(List.of(BUDGET), Map.of("budget", "strict")));

		assertTrue(index.getItemsByTag(LUXURY, 10).isEmpty());
		assertEquals(Set.of("item-1"), index.getItemsByTag(BUDGET, 10));
	}

	@Test
	void getTagsByCategory() {
		index.index("item-1", new TagResult(List.of(LUXURY, LARGE),
				Map.of("luxury", "strict", "large", "relaxed")));
		index.index("item-2", new TagResult(List.of(BUDGET), Map.of("budget", "strict")));

		Map<String, Set<Tag>> byCategory = index.getTagsByCategory();

		assertEquals(2, byCategory.size());
		assertTrue(byCategory.get("price").contains(LUXURY));
		assertTrue(byCategory.get("price").contains(BUDGET));
		assertTrue(byCategory.get("size").contains(LARGE));
	}

	@Test
	void getTagCounts() {
		index.index("item-1", new TagResult(List.of(LUXURY, LARGE),
				Map.of("luxury", "strict", "large", "relaxed")));
		index.index("item-2", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));

		Map<Tag, Integer> counts = index.getTagCounts();

		assertEquals(2, counts.get(LUXURY));
		assertEquals(1, counts.get(LARGE));
	}

	@Test
	void getTagCounts_empty() {
		Map<Tag, Integer> counts = index.getTagCounts();

		assertTrue(counts.isEmpty());
	}

	@Test
	void clear_removesEverything() {
		index.index("item-1", new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
		index.index("item-2", new TagResult(List.of(BUDGET), Map.of("budget", "strict")));

		index.clear();

		assertTrue(index.getItemsByTag(LUXURY, 10).isEmpty());
		assertTrue(index.getItemsByTag(BUDGET, 10).isEmpty());
		assertTrue(index.getTagCounts().isEmpty());
		assertTrue(index.getTagsByCategory().isEmpty());
	}

	@Test
	void threadSafety_concurrentIndexing() throws InterruptedException {
		int threadCount = 10;
		Thread[] threads = new Thread[threadCount];
		for (int i = 0; i < threadCount; i++) {
			final int idx = i;
			threads[i] = new Thread(() -> {
				index.index("item-" + idx, new TagResult(List.of(LUXURY), Map.of("luxury", "strict")));
			});
		}
		for (Thread t : threads) t.start();
		for (Thread t : threads) t.join();

		assertEquals(threadCount, index.getItemsByTag(LUXURY, 100).size());
	}
}

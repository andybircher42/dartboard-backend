package com.bookishbroccoli.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class GetRunResultsListTest extends ApifyServiceTestBase {

	@Test
	void success_returnsAllItems() throws Exception {
		wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
				.withQueryParam("token", equalTo("test-token"))
				.willReturn(okJson("[{\"name\":\"a\"},{\"name\":\"b\"},{\"name\":\"c\"}]")));

		List<JsonNode> results = service.getRunResultsList("run123");

		assertEquals(3, results.size());
		assertEquals("a", results.get(0).path("name").asText());
		assertEquals("b", results.get(1).path("name").asText());
		assertEquals("c", results.get(2).path("name").asText());
	}

	@Test
	void filtersNoSearchResultsEntries() throws Exception {
		wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
				.willReturn(okJson("[{\"name\":\"a\"},{\"error\":\"No search results found\"},{\"name\":\"b\"}]")));

		List<JsonNode> results = service.getRunResultsList("run123");

		assertEquals(2, results.size());
		assertEquals("a", results.get(0).path("name").asText());
		assertEquals("b", results.get(1).path("name").asText());
	}

	@Test
	void allNoResults_returnsEmptyList() throws Exception {
		wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
				.willReturn(okJson("[{\"error\":\"No search results found\"}]")));

		List<JsonNode> results = service.getRunResultsList("run123");

		assertTrue(results.isEmpty());
	}

	@Test
	void emptyArray_returnsEmptyList() throws Exception {
		wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
				.willReturn(okJson("[]")));

		List<JsonNode> results = service.getRunResultsList("run123");

		assertTrue(results.isEmpty());
	}

	@Test
	void errorInItem_throwsNonRetryable() {
		wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
				.willReturn(okJson("[{\"error\":\"Actor crashed\"}]")));

		assertThrows(ApifyNonRetryableException.class,
				() -> service.getRunResultsList("run123"));
	}

	@Test
	void isValidFalseInItem_throwsNonRetryable() {
		wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
				.willReturn(okJson("[{\"isValid\":false,\"reason\":\"bad\"}]")));

		assertThrows(ApifyNonRetryableException.class,
				() -> service.getRunResultsList("run123"));
	}

	@Test
	void httpError_throwsAppropriateException() {
		wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
				.willReturn(aResponse().withStatus(429).withBody("rate limited")));

		assertThrows(ApifyRetryableException.class,
				() -> service.getRunResultsList("run123"));
	}
}

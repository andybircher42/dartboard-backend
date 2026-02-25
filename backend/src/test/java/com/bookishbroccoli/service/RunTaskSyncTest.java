package com.bookishbroccoli.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunTaskSyncTest extends ApifyServiceTestBase {

  @Test
  void success_returnsList() throws Exception {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
            .willReturn(okJson("[{\"name\":\"a\"},{\"name\":\"b\"}]")));

    List<JsonNode> results = service.runTaskSync("my~task", Map.of());

    assertEquals(2, results.size());
    assertEquals("a", results.get(0).path("name").asText());
    assertEquals("b", results.get(1).path("name").asText());
  }

  @Test
  void emptyArray_returnsEmptyList() throws Exception {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
            .willReturn(okJson("[]")));

    List<JsonNode> results = service.runTaskSync("my~task", Map.of());

    assertTrue(results.isEmpty());
  }

  @Test
  void filtersNoSearchResultsEntries() throws Exception {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
            .willReturn(
                okJson(
                    "[{\"name\":\"a\"},{\"error\":\"No search results found\"},{\"name\":\"b\"}]")));

    List<JsonNode> results = service.runTaskSync("my~task", Map.of());

    assertEquals(2, results.size());
    assertEquals("a", results.get(0).path("name").asText());
    assertEquals("b", results.get(1).path("name").asText());
  }

  @Test
  void errorInResponse_throwsNonRetryable() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
            .willReturn(okJson("{\"error\":\"Something went wrong\"}")));

    assertThrows(ApifyNonRetryableException.class, () -> service.runTaskSync("my~task", Map.of()));
  }

  @Test
  void isValidFalse_throwsNonRetryable() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
            .willReturn(okJson("{\"isValid\":false,\"message\":\"bad input\"}")));

    assertThrows(ApifyNonRetryableException.class, () -> service.runTaskSync("my~task", Map.of()));
  }

  @Test
  void nonArrayResponse_throwsRuntimeException() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
            .willReturn(okJson("{\"data\":\"not an array\"}")));

    assertThrows(RuntimeException.class, () -> service.runTaskSync("my~task", Map.of()));
  }

  @Test
  void errorInArrayItem_throwsNonRetryable() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
            .willReturn(okJson("[{\"name\":\"a\"},{\"error\":\"Actor failed\"}]")));

    assertThrows(ApifyNonRetryableException.class, () -> service.runTaskSync("my~task", Map.of()));
  }
}

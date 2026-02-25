package com.dartboardbackend.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunTaskTest extends ApifyServiceTestBase {

  @Test
  void success_returnsJsonResponse() throws Exception {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .withQueryParam("token", equalTo("test-token"))
            .willReturn(okJson("{\"data\":{\"id\":\"run123\"}}")));

    JsonNode result = service.runTask("my~task", "runs", Map.of("key", "value"));

    assertEquals("run123", result.path("data").path("id").asText());
  }

  @Test
  void sendsCorrectRequestBody() throws Exception {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .willReturn(okJson("{\"data\":{\"id\":\"run123\"}}")));

    service.runTask("my~task", "runs", Map.of("zipCodes", List.of("90210")));

    wireMock.verify(
        postRequestedFor(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{\"zipCodes\":[\"90210\"]}")));
  }

  @Test
  void http429_throwsRetryableException() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .willReturn(aResponse().withStatus(429).withBody("rate limited")));

    ApifyRetryableException ex =
        assertThrows(
            ApifyRetryableException.class, () -> service.runTask("my~task", "runs", Map.of()));

    assertEquals(429, ex.getStatusCode());
    assertEquals("rate limited", ex.getResponseBody().orElseThrow());
  }

  @Test
  void http500_throwsRetryableException() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .willReturn(aResponse().withStatus(500).withBody("server error")));

    ApifyRetryableException ex =
        assertThrows(
            ApifyRetryableException.class, () -> service.runTask("my~task", "runs", Map.of()));

    assertEquals(500, ex.getStatusCode());
  }

  @Test
  void http400_throwsNonRetryableException() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .willReturn(aResponse().withStatus(400).withBody("bad request")));

    ApifyNonRetryableException ex =
        assertThrows(
            ApifyNonRetryableException.class, () -> service.runTask("my~task", "runs", Map.of()));

    assertEquals(400, ex.getStatusCode());
  }

  @Test
  void http401_throwsNonRetryableException() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .willReturn(aResponse().withStatus(401).withBody("unauthorized")));

    ApifyNonRetryableException ex =
        assertThrows(
            ApifyNonRetryableException.class, () -> service.runTask("my~task", "runs", Map.of()));

    assertEquals(401, ex.getStatusCode());
  }

  @Test
  void http404_throwsNonRetryableException() {
    wireMock.stubFor(
        post(urlPathEqualTo("/actor-tasks/my~task/runs"))
            .willReturn(aResponse().withStatus(404).withBody("not found")));

    ApifyNonRetryableException ex =
        assertThrows(
            ApifyNonRetryableException.class, () -> service.runTask("my~task", "runs", Map.of()));

    assertEquals(404, ex.getStatusCode());
  }
}

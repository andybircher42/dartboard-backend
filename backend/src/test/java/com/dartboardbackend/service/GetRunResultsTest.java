package com.dartboardbackend.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class GetRunResultsTest extends ApifyServiceTestBase {

  @Test
  void arrayResponse_returnsFirstItem() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
            .withQueryParam("token", equalTo("test-token"))
            .willReturn(okJson("[{\"name\":\"first\"},{\"name\":\"second\"}]")));

    JsonNode result = service.getRunResults("run123");

    assertEquals("first", result.path("name").asText());
  }

  @Test
  void singleObjectResponse_returnsObject() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
            .willReturn(okJson("{\"name\":\"only\"}")));

    JsonNode result = service.getRunResults("run123");

    assertEquals("only", result.path("name").asText());
  }

  @Test
  void errorInResponse_throws() {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
            .willReturn(okJson("{\"error\":\"Something broke\"}")));

    assertThrows(ApifyNonRetryableException.class, () -> service.getRunResults("run123"));
  }
}

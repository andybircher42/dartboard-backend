package com.dartboardbackend.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GetRunStatusTest extends ApifyServiceTestBase {

  @Test
  void success_returnsStatusString() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .withQueryParam("token", equalTo("test-token"))
            .willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}")));

    String status = service.getRunStatus("run123");

    assertEquals("RUNNING", status);
  }

  @Test
  void succeededStatus() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

    assertEquals("SUCCEEDED", service.getRunStatus("run123"));
  }

  @Test
  void missingDataField_throwsNonRetryable() {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123")).willReturn(okJson("{\"something\":\"else\"}")));

    ApifyNonRetryableException ex =
        assertThrows(ApifyNonRetryableException.class, () -> service.getRunStatus("run123"));

    assertTrue(ex.getMessage().contains("missing 'data' field"));
  }

  @Test
  void nullDataField_throwsNonRetryable() {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123")).willReturn(okJson("{\"data\":null}")));

    ApifyNonRetryableException ex =
        assertThrows(ApifyNonRetryableException.class, () -> service.getRunStatus("run123"));

    assertTrue(ex.getMessage().contains("missing 'data' field"));
  }

  @Test
  void missingStatusField_throwsNonRetryable() {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"id\":\"run123\"}}")));

    ApifyNonRetryableException ex =
        assertThrows(ApifyNonRetryableException.class, () -> service.getRunStatus("run123"));

    assertTrue(ex.getMessage().contains("missing 'status' field"));
  }

  @Test
  void httpError_throwsAppropriateException() {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(aResponse().withStatus(500).withBody("error")));

    assertThrows(ApifyRetryableException.class, () -> service.getRunStatus("run123"));
  }
}

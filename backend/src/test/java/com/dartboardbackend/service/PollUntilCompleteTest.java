package com.dartboardbackend.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PollUntilCompleteTest extends ApifyServiceTestBase {

  @Test
  void immediateCompletion() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

    String status = service.pollUntilComplete("run123");

    assertEquals("SUCCEEDED", status);
  }

  @Test
  void completesAfterPolling() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .inScenario("poll")
            .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
            .willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}"))
            .willSetStateTo("poll2"));

    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .inScenario("poll")
            .whenScenarioStateIs("poll2")
            .willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

    String status = service.pollUntilComplete("run123");

    assertEquals("SUCCEEDED", status);
  }

  @Test
  void returnsFailedStatus() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"FAILED\"}}")));

    String status = service.pollUntilComplete("run123");

    assertEquals("FAILED", status);
  }

  @Test
  void timeout_throwsRuntimeException() throws Exception {
    setField(service, "pollTimeoutSeconds", 2);

    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}")));

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> service.pollUntilComplete("run123"));

    assertTrue(ex.getMessage().contains("Polling timeout"));
  }

  @Test
  void toleratesTransientErrors_thenSucceeds() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .inScenario("transient")
            .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
            .willReturn(aResponse().withStatus(500).withBody("error"))
            .willSetStateTo("recovered"));

    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .inScenario("transient")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

    String status = service.pollUntilComplete("run123");

    assertEquals("SUCCEEDED", status);
  }

  @Test
  void propagatesNonRetryableError() {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(aResponse().withStatus(401).withBody("unauthorized")));

    assertThrows(ApifyNonRetryableException.class, () -> service.pollUntilComplete("run123"));
  }

  @Test
  void givesUpAfterConsecutiveRetryableErrors() throws Exception {
    setField(service, "pollTimeoutSeconds", 30);

    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(aResponse().withStatus(500).withBody("error")));

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> service.pollUntilComplete("run123"));

    assertTrue(ex.getMessage().contains("consecutive errors"));
    assertInstanceOf(ApifyRetryableException.class, ex.getCause());
  }
}

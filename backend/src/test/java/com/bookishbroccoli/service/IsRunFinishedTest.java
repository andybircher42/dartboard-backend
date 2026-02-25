package com.bookishbroccoli.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IsRunFinishedTest extends ApifyServiceTestBase {

  @Test
  void running_returnsFalse() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}")));

    assertFalse(service.isRunFinished("run123"));
  }

  @Test
  void ready_returnsFalse() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"READY\"}}")));

    assertFalse(service.isRunFinished("run123"));
  }

  @Test
  void succeeded_returnsTrue() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

    assertTrue(service.isRunFinished("run123"));
  }

  @Test
  void failed_returnsTrue() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"FAILED\"}}")));

    assertTrue(service.isRunFinished("run123"));
  }

  @Test
  void aborted_returnsTrue() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"ABORTED\"}}")));

    assertTrue(service.isRunFinished("run123"));
  }

  @Test
  void timedOut_returnsTrue() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"TIMED-OUT\"}}")));

    assertTrue(service.isRunFinished("run123"));
  }

  @Test
  void unknownStatus_returnsFalse() throws Exception {
    wireMock.stubFor(
        get(urlPathEqualTo("/actor-runs/run123"))
            .willReturn(okJson("{\"data\":{\"status\":\"SOMETHING_NEW\"}}")));

    assertFalse(service.isRunFinished("run123"));
  }
}

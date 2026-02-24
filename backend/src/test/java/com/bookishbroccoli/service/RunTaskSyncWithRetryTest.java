package com.bookishbroccoli.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class RunTaskSyncWithRetryTest extends ApifyServiceTestBase {

	@Test
	void successOnFirstAttempt() throws Exception {
		wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
				.willReturn(okJson("[{\"name\":\"a\"}]")));

		List<JsonNode> results = service.runTaskSyncWithRetry("my~task", Map.of());

		assertEquals(1, results.size());
		wireMock.verify(1, postRequestedFor(
				urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items")));
	}

	@Test
	void retriesOnRetryableError_thenSucceeds() throws Exception {
		wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
				.inScenario("retry")
				.whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
				.willReturn(aResponse().withStatus(500).withBody("error"))
				.willSetStateTo("attempt2"));

		wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
				.inScenario("retry")
				.whenScenarioStateIs("attempt2")
				.willReturn(okJson("[{\"name\":\"recovered\"}]")));

		List<JsonNode> results = service.runTaskSyncWithRetry("my~task", Map.of());

		assertEquals(1, results.size());
		assertEquals("recovered", results.get(0).path("name").asText());
		wireMock.verify(2, postRequestedFor(
				urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items")));
	}

	@Test
	void nonRetryableError_failsImmediately() {
		wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
				.willReturn(aResponse().withStatus(400).withBody("bad request")));

		assertThrows(ApifyNonRetryableException.class,
				() -> service.runTaskSyncWithRetry("my~task", Map.of()));

		wireMock.verify(1, postRequestedFor(
				urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items")));
	}

	@Test
	void allRetriesExhausted_throwsRuntimeException() {
		wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
				.willReturn(aResponse().withStatus(500).withBody("persistent error")));

		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> service.runTaskSyncWithRetry("my~task", Map.of()));

		assertTrue(ex.getMessage().contains("attempts exhausted"));
		assertInstanceOf(ApifyRetryableException.class, ex.getCause());
		// 4 total attempts
		wireMock.verify(4, postRequestedFor(
				urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items")));
	}
}

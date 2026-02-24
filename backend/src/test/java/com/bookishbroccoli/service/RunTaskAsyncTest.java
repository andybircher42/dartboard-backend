package com.bookishbroccoli.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class RunTaskAsyncTest extends ApifyServiceTestBase {

	@Test
	void success_returnsRunId() throws Exception {
		wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
				.willReturn(okJson("{\"data\":{\"id\":\"run-abc-123\"}}")));

		String runId = service.runTaskAsync("my~task", Map.of());

		assertEquals("run-abc-123", runId);
	}

	@Test
	void arrayResponse_extractsIdFromFirst() throws Exception {
		wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
				.willReturn(okJson("[{\"data\":{\"id\":\"first-run\"}}]")));

		String runId = service.runTaskAsync("my~task", Map.of());

		assertEquals("first-run", runId);
	}
}

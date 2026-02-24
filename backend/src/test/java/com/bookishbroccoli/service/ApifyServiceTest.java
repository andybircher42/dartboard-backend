package com.bookishbroccoli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class ApifyServiceTest {

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private ApifyService service;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() throws Exception {
		service = new TestableApifyService(objectMapper);
		setField(service, "apifyToken", "test-token");
		setField(service, "apifyApiBase", wireMock.baseUrl());
		setField(service, "pollIntervalSeconds", 1);
		setField(service, "pollTimeoutSeconds", 5);
	}

	/** Subclass that eliminates real sleeps for retry/poll tests. */
	static class TestableApifyService extends ApifyService {
		TestableApifyService(ObjectMapper objectMapper) {
			super(objectMapper);
		}

		@Override
		long getRetryBackoffMs(int attempt) {
			return 10;
		}
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Class<?> clazz = target.getClass();
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(target, value);
				return;
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}

	// ==================== runTask ====================

	@Nested
	class RunTask {

		@Test
		void success_returnsJsonResponse() throws Exception {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.withQueryParam("token", equalTo("test-token"))
					.willReturn(okJson("{\"data\":{\"id\":\"run123\"}}")));

			JsonNode result = service.runTask("my~task", "runs", Map.of("key", "value"));

			assertEquals("run123", result.path("data").path("id").asText());
		}

		@Test
		void sendsCorrectRequestBody() throws Exception {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.willReturn(okJson("{\"data\":{\"id\":\"run123\"}}")));

			service.runTask("my~task", "runs", Map.of("zipCodes", List.of("90210")));

			wireMock.verify(postRequestedFor(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.withHeader("Content-Type", equalTo("application/json"))
					.withRequestBody(equalToJson("{\"zipCodes\":[\"90210\"]}")));
		}

		@Test
		void http429_throwsRetryableException() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.willReturn(aResponse().withStatus(429).withBody("rate limited")));

			ApifyRetryableException ex = assertThrows(ApifyRetryableException.class,
					() -> service.runTask("my~task", "runs", Map.of()));

			assertEquals(429, ex.getStatusCode());
			assertEquals("rate limited", ex.getResponseBody());
		}

		@Test
		void http500_throwsRetryableException() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.willReturn(aResponse().withStatus(500).withBody("server error")));

			ApifyRetryableException ex = assertThrows(ApifyRetryableException.class,
					() -> service.runTask("my~task", "runs", Map.of()));

			assertEquals(500, ex.getStatusCode());
		}

		@Test
		void http400_throwsNonRetryableException() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.willReturn(aResponse().withStatus(400).withBody("bad request")));

			ApifyNonRetryableException ex = assertThrows(ApifyNonRetryableException.class,
					() -> service.runTask("my~task", "runs", Map.of()));

			assertEquals(400, ex.getStatusCode());
		}

		@Test
		void http401_throwsNonRetryableException() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.willReturn(aResponse().withStatus(401).withBody("unauthorized")));

			ApifyNonRetryableException ex = assertThrows(ApifyNonRetryableException.class,
					() -> service.runTask("my~task", "runs", Map.of()));

			assertEquals(401, ex.getStatusCode());
		}

		@Test
		void http404_throwsNonRetryableException() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/runs"))
					.willReturn(aResponse().withStatus(404).withBody("not found")));

			ApifyNonRetryableException ex = assertThrows(ApifyNonRetryableException.class,
					() -> service.runTask("my~task", "runs", Map.of()));

			assertEquals(404, ex.getStatusCode());
		}
	}

	// ==================== runTaskSync ====================

	@Nested
	class RunTaskSync {

		@Test
		void success_returnsList() throws Exception {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
					.willReturn(okJson("[{\"name\":\"a\"},{\"name\":\"b\"}]")));

			List<JsonNode> results = service.runTaskSync("my~task", Map.of());

			assertEquals(2, results.size());
			assertEquals("a", results.get(0).path("name").asText());
			assertEquals("b", results.get(1).path("name").asText());
		}

		@Test
		void emptyArray_returnsEmptyList() throws Exception {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
					.willReturn(okJson("[]")));

			List<JsonNode> results = service.runTaskSync("my~task", Map.of());

			assertTrue(results.isEmpty());
		}

		@Test
		void filtersNoSearchResultsEntries() throws Exception {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
					.willReturn(okJson("[{\"name\":\"a\"},{\"error\":\"No search results found\"},{\"name\":\"b\"}]")));

			List<JsonNode> results = service.runTaskSync("my~task", Map.of());

			assertEquals(2, results.size());
			assertEquals("a", results.get(0).path("name").asText());
			assertEquals("b", results.get(1).path("name").asText());
		}

		@Test
		void errorInResponse_throwsNonRetryable() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
					.willReturn(okJson("{\"error\":\"Something went wrong\"}")));

			assertThrows(ApifyNonRetryableException.class,
					() -> service.runTaskSync("my~task", Map.of()));
		}

		@Test
		void isValidFalse_throwsNonRetryable() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
					.willReturn(okJson("{\"isValid\":false,\"message\":\"bad input\"}")));

			assertThrows(ApifyNonRetryableException.class,
					() -> service.runTaskSync("my~task", Map.of()));
		}

		@Test
		void nonArrayResponse_throwsRuntimeException() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
					.willReturn(okJson("{\"data\":\"not an array\"}")));

			assertThrows(RuntimeException.class,
					() -> service.runTaskSync("my~task", Map.of()));
		}

		@Test
		void errorInArrayItem_throwsNonRetryable() {
			wireMock.stubFor(post(urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items"))
					.willReturn(okJson("[{\"name\":\"a\"},{\"error\":\"Actor failed\"}]")));

			assertThrows(ApifyNonRetryableException.class,
					() -> service.runTaskSync("my~task", Map.of()));
		}
	}

	// ==================== runTaskSyncWithRetry ====================

	@Nested
	class RunTaskSyncWithRetry {

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
			// MAX_RETRIES=3, so 4 total attempts (0,1,2,3)
			wireMock.verify(4, postRequestedFor(
					urlPathEqualTo("/actor-tasks/my~task/run-sync-get-dataset-items")));
		}
	}

	// ==================== runTaskAsync ====================

	@Nested
	class RunTaskAsync {

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

	// ==================== getRunStatus ====================

	@Nested
	class GetRunStatus {

		@Test
		void success_returnsStatusString() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.withQueryParam("token", equalTo("test-token"))
					.willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}")));

			String status = service.getRunStatus("run123");

			assertEquals("RUNNING", status);
		}

		@Test
		void succeededStatus() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

			assertEquals("SUCCEEDED", service.getRunStatus("run123"));
		}

		@Test
		void missingDataField_throwsNonRetryable() {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"something\":\"else\"}")));

			ApifyNonRetryableException ex = assertThrows(ApifyNonRetryableException.class,
					() -> service.getRunStatus("run123"));

			assertTrue(ex.getMessage().contains("missing 'data' field"));
		}

		@Test
		void nullDataField_throwsNonRetryable() {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":null}")));

			ApifyNonRetryableException ex = assertThrows(ApifyNonRetryableException.class,
					() -> service.getRunStatus("run123"));

			assertTrue(ex.getMessage().contains("missing 'data' field"));
		}

		@Test
		void missingStatusField_throwsNonRetryable() {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"id\":\"run123\"}}")));

			ApifyNonRetryableException ex = assertThrows(ApifyNonRetryableException.class,
					() -> service.getRunStatus("run123"));

			assertTrue(ex.getMessage().contains("missing 'status' field"));
		}

		@Test
		void httpError_throwsAppropriateException() {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(aResponse().withStatus(500).withBody("error")));

			assertThrows(ApifyRetryableException.class,
					() -> service.getRunStatus("run123"));
		}
	}

	// ==================== isRunFinished ====================

	@Nested
	class IsRunFinished {

		@Test
		void running_returnsFalse() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}")));

			assertFalse(service.isRunFinished("run123"));
		}

		@Test
		void ready_returnsFalse() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"READY\"}}")));

			assertFalse(service.isRunFinished("run123"));
		}

		@Test
		void succeeded_returnsTrue() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

			assertTrue(service.isRunFinished("run123"));
		}

		@Test
		void failed_returnsTrue() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"FAILED\"}}")));

			assertTrue(service.isRunFinished("run123"));
		}

		@Test
		void aborted_returnsTrue() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"ABORTED\"}}")));

			assertTrue(service.isRunFinished("run123"));
		}

		@Test
		void timedOut_returnsTrue() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"TIMED-OUT\"}}")));

			assertTrue(service.isRunFinished("run123"));
		}

		@Test
		void unknownStatus_returnsFalse() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"SOMETHING_NEW\"}}")));

			assertFalse(service.isRunFinished("run123"));
		}
	}

	// ==================== pollUntilComplete ====================

	@Nested
	class PollUntilComplete {

		@Test
		void immediateCompletion() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

			String status = service.pollUntilComplete("run123");

			assertEquals("SUCCEEDED", status);
		}

		@Test
		void completesAfterPolling() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.inScenario("poll")
					.whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
					.willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}"))
					.willSetStateTo("poll2"));

			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.inScenario("poll")
					.whenScenarioStateIs("poll2")
					.willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

			String status = service.pollUntilComplete("run123");

			assertEquals("SUCCEEDED", status);
		}

		@Test
		void returnsFailedStatus() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"FAILED\"}}")));

			String status = service.pollUntilComplete("run123");

			assertEquals("FAILED", status);
		}

		@Test
		void timeout_throwsRuntimeException() throws Exception {
			// pollTimeoutSeconds=5, pollIntervalSeconds=1, so it will poll ~5 times then timeout
			setField(service, "pollTimeoutSeconds", 2);

			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(okJson("{\"data\":{\"status\":\"RUNNING\"}}")));

			RuntimeException ex = assertThrows(RuntimeException.class,
					() -> service.pollUntilComplete("run123"));

			assertTrue(ex.getMessage().contains("Polling timeout"));
		}

		@Test
		void toleratesTransientErrors_thenSucceeds() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.inScenario("transient")
					.whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
					.willReturn(aResponse().withStatus(500).withBody("error"))
					.willSetStateTo("recovered"));

			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.inScenario("transient")
					.whenScenarioStateIs("recovered")
					.willReturn(okJson("{\"data\":{\"status\":\"SUCCEEDED\"}}")));

			String status = service.pollUntilComplete("run123");

			assertEquals("SUCCEEDED", status);
		}

		@Test
		void propagatesNonRetryableError() {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(aResponse().withStatus(401).withBody("unauthorized")));

			assertThrows(ApifyNonRetryableException.class,
					() -> service.pollUntilComplete("run123"));
		}

		@Test
		void givesUpAfterConsecutiveRetryableErrors() throws Exception {
			// MAX_CONSECUTIVE_POLL_ERRORS = 5, all polls return 500
			setField(service, "pollTimeoutSeconds", 30);

			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123"))
					.willReturn(aResponse().withStatus(500).withBody("error")));

			RuntimeException ex = assertThrows(RuntimeException.class,
					() -> service.pollUntilComplete("run123"));

			assertTrue(ex.getMessage().contains("consecutive errors"));
			assertInstanceOf(ApifyRetryableException.class, ex.getCause());
		}
	}

	// ==================== getRunResults ====================

	@Nested
	class GetRunResults {

		@Test
		void arrayResponse_returnsFirstItem() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
					.withQueryParam("token", equalTo("test-token"))
					.willReturn(okJson("[{\"name\":\"first\"},{\"name\":\"second\"}]")));

			JsonNode result = service.getRunResults("run123");

			assertEquals("first", result.path("name").asText());
		}

		@Test
		void singleObjectResponse_returnsObject() throws Exception {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
					.willReturn(okJson("{\"name\":\"only\"}")));

			JsonNode result = service.getRunResults("run123");

			assertEquals("only", result.path("name").asText());
		}

		@Test
		void errorInResponse_throws() {
			wireMock.stubFor(get(urlPathEqualTo("/actor-runs/run123/dataset/items"))
					.willReturn(okJson("{\"error\":\"Something broke\"}")));

			assertThrows(ApifyNonRetryableException.class,
					() -> service.getRunResults("run123"));
		}
	}

	// ==================== getRunResultsList ====================

	@Nested
	class GetRunResultsList {

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

	// ==================== getRetryBackoffMs ====================

	@Nested
	class RetryBackoff {

		@Test
		void exponentialBackoff() {
			// Use a fresh non-testable instance to test real backoff logic
			ApifyService realService = new ApifyService(objectMapper);

			assertEquals(10_000, realService.getRetryBackoffMs(1));  // 10000 * 2^0
			assertEquals(20_000, realService.getRetryBackoffMs(2));  // 10000 * 2^1
			assertEquals(40_000, realService.getRetryBackoffMs(3));  // 10000 * 2^2
		}

		@Test
		void cappedAtMaxBackoff() {
			ApifyService realService = new ApifyService(objectMapper);

			assertEquals(60_000, realService.getRetryBackoffMs(4));  // 10000 * 2^3 = 80000 capped to 60000
			assertEquals(60_000, realService.getRetryBackoffMs(10)); // way over cap
		}
	}

	// ==================== ApifyRunStatus ====================

	@Nested
	class RunStatusEnum {

		@Test
		void fromString_knownStatuses() {
			assertEquals(ApifyRunStatus.RUNNING, ApifyRunStatus.fromString("RUNNING"));
			assertEquals(ApifyRunStatus.SUCCEEDED, ApifyRunStatus.fromString("SUCCEEDED"));
			assertEquals(ApifyRunStatus.FAILED, ApifyRunStatus.fromString("FAILED"));
			assertEquals(ApifyRunStatus.ABORTED, ApifyRunStatus.fromString("ABORTED"));
			assertEquals(ApifyRunStatus.TIMED_OUT, ApifyRunStatus.fromString("TIMED-OUT"));
			assertEquals(ApifyRunStatus.READY, ApifyRunStatus.fromString("READY"));
		}

		@Test
		void fromString_unknownReturnsUnknown() {
			assertEquals(ApifyRunStatus.UNKNOWN, ApifyRunStatus.fromString("FOOBAR"));
			assertEquals(ApifyRunStatus.UNKNOWN, ApifyRunStatus.fromString(""));
		}

		@Test
		void terminalStatuses() {
			assertTrue(ApifyRunStatus.SUCCEEDED.isTerminal());
			assertTrue(ApifyRunStatus.FAILED.isTerminal());
			assertTrue(ApifyRunStatus.ABORTED.isTerminal());
			assertTrue(ApifyRunStatus.TIMED_OUT.isTerminal());
		}

		@Test
		void nonTerminalStatuses() {
			assertFalse(ApifyRunStatus.RUNNING.isTerminal());
			assertFalse(ApifyRunStatus.READY.isTerminal());
			assertFalse(ApifyRunStatus.UNKNOWN.isTerminal());
		}

		@Test
		void valueReturnsString() {
			assertEquals("SUCCEEDED", ApifyRunStatus.SUCCEEDED.value());
			assertEquals("TIMED-OUT", ApifyRunStatus.TIMED_OUT.value());
		}

		@Test
		void toStringReturnsValue() {
			assertEquals("RUNNING", ApifyRunStatus.RUNNING.toString());
		}
	}
}

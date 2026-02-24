package com.bookishbroccoli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
public class ApifyService {

	private static final Logger logger = LoggerFactory.getLogger(ApifyService.class);

	@Value("${apify.api.token}")
	private String apifyToken;

	private static final String APIFY_API_BASE = "https://api.apify.com/v2";
	private static final int MAX_RETRIES = 3;
	private static final long INITIAL_BACKOFF_MS = 10_000;
	private static final long MAX_BACKOFF_MS = 60_000;
	private static final int MAX_CONSECUTIVE_POLL_ERRORS = 5;

	private final ObjectMapper objectMapper;

	public ApifyService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Runs the given Apify task synchronously — blocks until completion and returns dataset items.
	 */
	public List<JsonNode> runTaskSync(String taskId, Map<String, Object> input) throws IOException, ParseException {
		return toList(runTask(taskId, "run-sync-get-dataset-items", input));
	}

	/**
	 * Runs the given Apify task synchronously with retry logic.
	 * Retries transient failures with exponential backoff.
	 */
	public List<JsonNode> runTaskSyncWithRetry(String taskId, Map<String, Object> input) throws Exception {
		ApifyRetryableException lastRetryable = null;

		for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
			if (attempt > 0) {
				long backoffMs = getRetryBackoffMs(attempt);
				logger.warn("Retry attempt {}/{} for task {} after {}ms backoff", attempt, MAX_RETRIES, taskId, backoffMs);
				Thread.sleep(backoffMs);
			}

			try {
				return runTaskSync(taskId, input);
			} catch (ApifyNonRetryableException e) {
				throw e;
			} catch (ApifyRetryableException e) {
				lastRetryable = e;
				logger.warn("Retryable error on attempt {}/{} for task {}: {}",
						attempt + 1, MAX_RETRIES + 1, taskId, e.getMessage());
			}
		}

		throw new RuntimeException("All " + (MAX_RETRIES + 1) + " attempts exhausted for task " + taskId, lastRetryable);
	}

	/**
	 * Starts an Apify task asynchronously — returns the run ID immediately.
	 * Use {@link #pollUntilComplete(String)} to wait for it, then {@link #getRunResultsList(String)} to fetch results.
	 */
	public String runTaskAsync(String taskId, Map<String, Object> input) throws IOException, ParseException {
		JsonNode node = runTask(taskId, "runs", input);
		return (node.isArray() ? node.get(0) : node).get("data").get("id").asText();
	}

	/**
	 * Low-level method: POSTs to the Apify task endpoint and returns the raw JSON response.
	 */
	public JsonNode runTask(String taskId, String runType, Map<String, Object> input)
			throws IOException, ParseException {
		String url = String.format("%s/actor-tasks/%s/%s?token=%s", APIFY_API_BASE, taskId, runType, apifyToken);
		logger.info("Running actor task: {}/{}", taskId, runType);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost request = new HttpPost(url);
			request.setHeader("Content-Type", "application/json");

			String jsonInput = objectMapper.writeValueAsString(input);
			request.setEntity(new StringEntity(jsonInput));

			try (CloseableHttpResponse response = httpClient.execute(request)) {
				int statusCode = response.getCode();
				String responseBody = EntityUtils.toString(response.getEntity());
				checkHttpStatus(statusCode, responseBody);
				JsonNode root = objectMapper.readTree(responseBody);
				logger.info("Run for task {} returned response", taskId);
				return root;
			}
		} catch (ApifyApiException e) {
			throw e;
		} catch (IOException e) {
			throw new ApifyRetryableException("Network error during runTask: " + e.getMessage(), e);
		}
	}

	/**
	 * Fetches the current status string (e.g. "RUNNING", "SUCCEEDED") for the given run.
	 */
	public String getRunStatus(String runId) throws IOException, ParseException {
		String url = String.format("%s/actor-runs/%s?token=%s", APIFY_API_BASE, runId, apifyToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);

			try (CloseableHttpResponse response = httpClient.execute(request)) {
				int statusCode = response.getCode();
				String responseBody = EntityUtils.toString(response.getEntity());
				checkHttpStatus(statusCode, responseBody);
				JsonNode jsonResponse = objectMapper.readTree(responseBody);
				JsonNode data = jsonResponse.get("data");
				if (data == null || data.isNull()) {
					throw new ApifyNonRetryableException(
							"Unexpected response structure: missing 'data' field in getRunStatus for run " + runId,
							statusCode, responseBody);
				}
				JsonNode status = data.get("status");
				if (status == null || status.isNull()) {
					throw new ApifyNonRetryableException(
							"Unexpected response structure: missing 'status' field in getRunStatus for run " + runId,
							statusCode, responseBody);
				}
				return status.asText();
			}
		}
	}

	/**
	 * Returns true if the run has reached a terminal state (SUCCEEDED, FAILED, ABORTED, TIMED_OUT).
	 */
	public boolean isRunFinished(String runId) throws IOException, ParseException {
		String status = getRunStatus(runId);
		return ApifyRunStatus.fromString(status).isTerminal();
	}

	/**
	 * Polls Apify until the run is complete. Returns the final status string.
	 * Polls every 20 seconds, times out at 300 seconds.
	 * Tolerates up to 5 consecutive transient errors during polling.
	 */
	public String pollUntilComplete(String runId) throws Exception {
		int elapsed = 0;
		int pollInterval = 20;
		int pollTimeout = 300;
		int consecutiveErrors = 0;

		while (elapsed < pollTimeout) {
			try {
				String status = getRunStatus(runId);
				consecutiveErrors = 0;
				logger.info("Run {} status: {}", runId, status);

				if (ApifyRunStatus.fromString(status).isTerminal()) {
					return status;
				}
			} catch (ApifyNonRetryableException e) {
				throw e;
			} catch (ApifyRetryableException e) {
				consecutiveErrors++;
				logger.warn("Retryable error polling run {} (consecutive: {}/{}): {}",
						runId, consecutiveErrors, MAX_CONSECUTIVE_POLL_ERRORS, e.getMessage());
				if (consecutiveErrors >= MAX_CONSECUTIVE_POLL_ERRORS) {
					throw new RuntimeException("Polling gave up after " + MAX_CONSECUTIVE_POLL_ERRORS
							+ " consecutive errors for run " + runId, e);
				}
			}

			Thread.sleep(pollInterval * 1000L);
			elapsed += pollInterval;
		}

		logger.error("Polling timeout after {} seconds for run {}", pollTimeout, runId);
		throw new RuntimeException("Polling timeout after " + pollTimeout + " seconds for run " + runId);
	}

	/**
	 * Fetches the dataset items for the given run, returning the first item if the result is an array.
	 */
	public JsonNode getRunResults(String runId) throws IOException, ParseException {
		String url = String.format("%s/actor-runs/%s/dataset/items?token=%s", APIFY_API_BASE, runId, apifyToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);

			try (CloseableHttpResponse response = httpClient.execute(request)) {
				int statusCode = response.getCode();
				String responseBody = EntityUtils.toString(response.getEntity());
				checkHttpStatus(statusCode, responseBody);
				JsonNode root = objectMapper.readTree(responseBody);
				checkForErrors(root);
				if (!root.isArray()) {
					return root;
				}
				return StreamSupport.stream(root.spliterator(), false).toList().get(0);
			}
		}
	}

	/**
	 * Fetches the dataset items for the given run as a list, filtering out "no results" entries.
	 */
	public List<JsonNode> getRunResultsList(String runId) throws IOException, ParseException {
		String url = String.format("%s/actor-runs/%s/dataset/items?token=%s", APIFY_API_BASE, runId, apifyToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);

			try (CloseableHttpResponse response = httpClient.execute(request)) {
				int statusCode = response.getCode();
				String responseBody = EntityUtils.toString(response.getEntity());
				checkHttpStatus(statusCode, responseBody);
				JsonNode root = objectMapper.readTree(responseBody);
				return toList(root);
			}
		}
	}

	/** Package-private so tests can override to avoid sleeps. */
	long getRetryBackoffMs(int attempt) {
		return Math.min(INITIAL_BACKOFF_MS * (1L << (attempt - 1)), MAX_BACKOFF_MS);
	}

	private void checkHttpStatus(int statusCode, String responseBody) throws ApifyApiException {
		if (statusCode >= 200 && statusCode <= 299) {
			return;
		}
		if (statusCode == 429 || statusCode >= 500) {
			throw new ApifyRetryableException(
					"Apify returned retryable HTTP " + statusCode, statusCode, responseBody);
		}
		throw new ApifyNonRetryableException(
				"Apify returned non-retryable HTTP " + statusCode, statusCode, responseBody);
	}

	private List<JsonNode> toList(JsonNode results) {
		checkForErrors(results);
		if (!results.isArray()) {
			throw new RuntimeException("JsonNode run info from Apify is not an array" + results);
		}
		return StreamSupport.stream(results.spliterator(), false)
				.peek(this::checkForErrors)
				.filter(node -> !isNoResultsError(node))
				.toList();
	}

	private boolean isNoResultsError(JsonNode node) {
		return node.has("error") && "No search results found".equals(node.get("error").asText());
	}

	private JsonNode checkForErrors(JsonNode results) {
		if (results.has("error")) {
			if (isNoResultsError(results)) {
				logger.info("Apify returned no search results — treating as empty");
				return results;
			}
			logger.error("Apify returned error in response: {}", results);
			throw new ApifyNonRetryableException("caught error in jsonNode: " + results);
		}
		if (results.has("isValid") && !results.get("isValid").asBoolean()) {
			logger.error("Apify request was invalid: {}", results);
			throw new ApifyNonRetryableException(
					"The request to Apify was not valid. Cannot retry. " + results.toPrettyString());
		}
		return results;
	}
}

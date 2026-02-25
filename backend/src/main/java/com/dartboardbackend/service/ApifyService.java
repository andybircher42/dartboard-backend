package com.dartboardbackend.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dartboardbackend.retry.NonRetryableException;
import com.dartboardbackend.retry.RetryExecutor;
import com.dartboardbackend.retry.RetryPolicy;
import com.dartboardbackend.retry.RetryResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Central service for communicating with the Apify API.
 *
 * <p>Handles synchronous and asynchronous task execution, polling for run completion, result
 * fetching, and error classification into retryable vs. non-retryable categories.
 */
@Service
public class ApifyService {

  private static final Logger logger = LoggerFactory.getLogger(ApifyService.class);

  @Value("${apify.api.token}")
  private String apifyToken;

  @Value("${apify.api.base-url:https://api.apify.com/v2}")
  private String apifyApiBase;

  @Value("${apify.poll.interval-seconds:20}")
  private int pollIntervalSeconds;

  @Value("${apify.poll.timeout-seconds:300}")
  private int pollTimeoutSeconds;

  private static final int MAX_CONSECUTIVE_POLL_ERRORS = 5;
  private static final RetryPolicy RETRY_POLICY = RetryPolicy.of("apify-sync", 4);

  private final ObjectMapper objectMapper;
  private final RetryExecutor retryExecutor;

  /**
   * Constructs the service with required dependencies.
   *
   * @param objectMapper Jackson mapper used for JSON serialization and deserialization
   * @param retryExecutor executor used to run operations with configurable retry policies
   */
  public ApifyService(ObjectMapper objectMapper, RetryExecutor retryExecutor) {
    this.objectMapper = objectMapper;
    this.retryExecutor = retryExecutor;
  }

  /**
   * Runs the given Apify task synchronously — blocks until completion and returns dataset items.
   */
  public List<JsonNode> runTaskSync(String taskId, Map<String, Object> input)
      throws IOException, ParseException {
    return toList(runTask(taskId, "run-sync-get-dataset-items", input));
  }

  /**
   * Runs the given Apify task synchronously with retry logic. Retries transient failures with
   * exponential backoff.
   */
  public List<JsonNode> runTaskSyncWithRetry(String taskId, Map<String, Object> input)
      throws Exception {
    RetryResult<List<JsonNode>> result =
        retryExecutor.execute(RETRY_POLICY, () -> runTaskSync(taskId, input));
    if (result.succeeded()) {
      return result.value();
    }
    if (result.lastException() instanceof NonRetryableException) {
      throw result.lastException();
    }
    throw new RuntimeException(
        "All " + result.attempts() + " attempts exhausted for task " + taskId,
        result.lastException());
  }

  /**
   * Starts an Apify task asynchronously — returns the run ID immediately. Use {@link
   * #pollUntilComplete(String)} to wait for it, then {@link #getRunResultsList(String)} to fetch
   * results.
   */
  public String runTaskAsync(String taskId, Map<String, Object> input)
      throws IOException, ParseException {
    JsonNode node = runTask(taskId, "runs", input);
    return (node.isArray() ? node.get(0) : node).get("data").get("id").asText();
  }

  /** Low-level method: POSTs to the Apify task endpoint and returns the raw JSON response. */
  public JsonNode runTask(String taskId, String runType, Map<String, Object> input)
      throws IOException, ParseException {
    String url =
        String.format("%s/actor-tasks/%s/%s?token=%s", apifyApiBase, taskId, runType, apifyToken);
    logger.info("Running actor task: {}/{}", taskId, runType);

    try {
      HttpPost request = new HttpPost(url);
      request.setHeader("Content-Type", "application/json");

      String jsonInput = objectMapper.writeValueAsString(input);
      request.setEntity(new StringEntity(jsonInput));

      return executeRequest(request, (statusCode, responseBody) -> {
        JsonNode root = objectMapper.readTree(responseBody);
        logger.info("Run for task {} returned response", taskId);
        return root;
      });
    } catch (ApifyRetryableException | ApifyNonRetryableException e) {
      throw e;
    } catch (IOException e) {
      throw new ApifyRetryableException("Network error during runTask: " + e.getMessage(), e);
    }
  }

  /** Fetches the current status string (e.g. "RUNNING", "SUCCEEDED") for the given run. */
  public String getRunStatus(String runId) throws IOException, ParseException {
    String url = String.format("%s/actor-runs/%s?token=%s", apifyApiBase, runId, apifyToken);

    HttpGet request = new HttpGet(url);

    return executeRequest(request, (statusCode, responseBody) -> {
      JsonNode jsonResponse = objectMapper.readTree(responseBody);
      JsonNode data = jsonResponse.get("data");
      if (data == null || data.isNull()) {
        throw new ApifyNonRetryableException(
            "Unexpected response structure: missing 'data' field in getRunStatus for run "
                + runId,
            statusCode,
            responseBody);
      }
      JsonNode status = data.get("status");
      if (status == null || status.isNull()) {
        throw new ApifyNonRetryableException(
            "Unexpected response structure: missing 'status' field in getRunStatus for run "
                + runId,
            statusCode,
            responseBody);
      }
      return status.asText();
    });
  }

  /**
   * Returns true if the run has reached a terminal state (SUCCEEDED, FAILED, ABORTED, TIMED_OUT).
   */
  public boolean isRunFinished(String runId) throws IOException, ParseException {
    String status = getRunStatus(runId);
    return ApifyRunStatus.fromString(status).isTerminal();
  }

  /**
   * Polls Apify until the run is complete. Returns the final status string. Polls every 20 seconds,
   * times out at 300 seconds. Tolerates up to 5 consecutive transient errors during polling.
   */
  public String pollUntilComplete(String runId) throws Exception {
    int elapsed = 0;
    int consecutiveErrors = 0;

    while (elapsed < pollTimeoutSeconds) {
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
        logger.warn(
            "Retryable error polling run {} (consecutive: {}/{}): {}",
            runId,
            consecutiveErrors,
            MAX_CONSECUTIVE_POLL_ERRORS,
            e.getMessage());
        if (consecutiveErrors >= MAX_CONSECUTIVE_POLL_ERRORS) {
          throw new RuntimeException(
              "Polling gave up after "
                  + MAX_CONSECUTIVE_POLL_ERRORS
                  + " consecutive errors for run "
                  + runId,
              e);
        }
      }

      Thread.sleep(pollIntervalSeconds * 1000L);
      elapsed += pollIntervalSeconds;
    }

    logger.error("Polling timeout after {} seconds for run {}", pollTimeoutSeconds, runId);
    throw new RuntimeException(
        "Polling timeout after " + pollTimeoutSeconds + " seconds for run " + runId);
  }

  /**
   * Fetches the dataset items for the given run, returning the first item if the result is an
   * array.
   */
  public JsonNode getRunResults(String runId) throws IOException, ParseException {
    String url =
        String.format("%s/actor-runs/%s/dataset/items?token=%s", apifyApiBase, runId, apifyToken);

    HttpGet request = new HttpGet(url);

    return executeRequest(request, (statusCode, responseBody) -> {
      JsonNode root = objectMapper.readTree(responseBody);
      checkForErrors(root);
      if (!root.isArray()) {
        return root;
      }
      return StreamSupport.stream(root.spliterator(), false).toList().get(0);
    });
  }

  /** Fetches the dataset items for the given run as a list, filtering out "no results" entries. */
  public List<JsonNode> getRunResultsList(String runId) throws IOException, ParseException {
    String url =
        String.format("%s/actor-runs/%s/dataset/items?token=%s", apifyApiBase, runId, apifyToken);

    HttpGet request = new HttpGet(url);

    return executeRequest(request, (statusCode, responseBody) -> {
      JsonNode root = objectMapper.readTree(responseBody);
      return toList(root);
    });
  }

  @FunctionalInterface
  interface ResponseParser<T> {
    T parse(int statusCode, String body) throws IOException, ParseException;
  }

  private <T> T executeRequest(ClassicHttpRequest request, ResponseParser<T> parser)
      throws IOException, ParseException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      return httpClient.execute(request, response -> {
        int statusCode = response.getCode();
        String body = EntityUtils.toString(response.getEntity());
        checkHttpStatus(statusCode, body);
        return parser.parse(statusCode, body);
      });
    }
  }

  /**
   * Classifies an HTTP status code and throws the appropriate exception for error responses.
   *
   * <p>2xx codes are treated as success. HTTP 429 (rate limit) and 5xx (server error) codes are
   * classified as retryable. All other error codes (4xx) are classified as non-retryable.
   *
   * @param statusCode the HTTP response status code
   * @param responseBody the raw response body, included in the exception for diagnostics
   * @throws ApifyRetryableException if the status indicates a transient, retryable error
   * @throws ApifyNonRetryableException if the status indicates a permanent error
   */
  private void checkHttpStatus(int statusCode, String responseBody) {
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

  /**
   * Converts a JSON array node into a list of result items, filtering out "no results" entries.
   *
   * <p>Each element is checked for body-level errors via {@link #checkForErrors(JsonNode)}. Items
   * matching the "No search results found" pattern are silently excluded from the returned list.
   *
   * @param results the JSON array node returned by an Apify run
   * @return a filtered list of result items
   * @throws RuntimeException if the node is not a JSON array
   * @throws ApifyNonRetryableException if any element contains a non-ignorable error
   */
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

  /**
   * Inspects a JSON response node for error or validation-failure fields.
   *
   * <p>If an {@code "error"} field is present and is not the benign "No search results found"
   * message, an {@link ApifyNonRetryableException} is thrown. If an {@code "isValid"} field is
   * present and {@code false}, the request is treated as invalid and a non-retryable exception is
   * thrown.
   *
   * @param results the JSON node to inspect
   * @return the same node, unchanged, if no actionable errors are found
   * @throws ApifyNonRetryableException if the node contains an error or a validation failure
   */
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

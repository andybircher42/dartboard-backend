package com.dartboardbackend.controller;

import com.dartboardbackend.service.ApifyService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing Apify operations.
 *
 * <p>All endpoints are served under the base path {@code /api/apify}. Provides synchronous and
 * asynchronous task execution, run status polling, and result retrieval.
 */
@RestController
@RequestMapping("/api/apify")
public class ApifyController {

  private final ApifyService apifyService;

  /**
   * Constructs the controller with the required Apify service dependency.
   *
   * @param apifyService the service used to interact with the Apify API
   */
  public ApifyController(ApifyService apifyService) {
    this.apifyService = apifyService;
  }

  /**
   * Runs an Apify task synchronously with automatic retry on transient failures.
   *
   * @param taskId the Apify task identifier
   * @param input the input payload to pass to the task
   * @return the dataset items produced by the task
   * @throws Exception if all retry attempts are exhausted or a non-retryable error occurs
   */
  @PostMapping("/run-sync")
  public ResponseEntity<List<JsonNode>> runSync(
      @RequestParam String taskId, @RequestBody Map<String, Object> input) throws Exception {
    List<JsonNode> results = apifyService.runTaskSyncWithRetry(taskId, input);
    return ResponseEntity.ok(results);
  }

  /**
   * Starts an Apify task asynchronously and returns the run ID immediately.
   *
   * @param taskId the Apify task identifier
   * @param input the input payload to pass to the task
   * @return a map containing the {@code "runId"} of the newly started run
   * @throws Exception if the task cannot be started
   */
  @PostMapping("/run-async")
  public ResponseEntity<Map<String, String>> runAsync(
      @RequestParam String taskId, @RequestBody Map<String, Object> input) throws Exception {
    String runId = apifyService.runTaskAsync(taskId, input);
    return ResponseEntity.ok(Map.of("runId", runId));
  }

  /**
   * Fetches the current status of an Apify run.
   *
   * @param runId the identifier of the Apify run
   * @return a map containing the {@code "status"} string (e.g. "RUNNING", "SUCCEEDED")
   * @throws Exception if the status cannot be retrieved
   */
  @GetMapping("/runs/{runId}/status")
  public ResponseEntity<Map<String, String>> getRunStatus(@PathVariable String runId)
      throws Exception {
    String status = apifyService.getRunStatus(runId);
    return ResponseEntity.ok(Map.of("status", status));
  }

  /**
   * Fetches the dataset items for a completed Apify run.
   *
   * @param runId the identifier of the Apify run
   * @return the list of dataset items produced by the run
   * @throws Exception if the results cannot be retrieved
   */
  @GetMapping("/runs/{runId}/results")
  public ResponseEntity<List<JsonNode>> getRunResults(@PathVariable String runId) throws Exception {
    List<JsonNode> results = apifyService.getRunResultsList(runId);
    return ResponseEntity.ok(results);
  }
}

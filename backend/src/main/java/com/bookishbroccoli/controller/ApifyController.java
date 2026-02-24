package com.bookishbroccoli.controller;

import com.bookishbroccoli.service.ApifyService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apify")
public class ApifyController {

	private final ApifyService apifyService;

	public ApifyController(ApifyService apifyService) {
		this.apifyService = apifyService;
	}

	@PostMapping("/run-sync")
	public ResponseEntity<List<JsonNode>> runSync(
			@RequestParam String taskId,
			@RequestBody Map<String, Object> input) throws Exception {
		List<JsonNode> results = apifyService.runTaskSyncWithRetry(taskId, input);
		return ResponseEntity.ok(results);
	}

	@PostMapping("/run-async")
	public ResponseEntity<Map<String, String>> runAsync(
			@RequestParam String taskId,
			@RequestBody Map<String, Object> input) throws Exception {
		String runId = apifyService.runTaskAsync(taskId, input);
		return ResponseEntity.ok(Map.of("runId", runId));
	}

	@GetMapping("/runs/{runId}/status")
	public ResponseEntity<Map<String, String>> getRunStatus(@PathVariable String runId) throws Exception {
		String status = apifyService.getRunStatus(runId);
		return ResponseEntity.ok(Map.of("status", status));
	}

	@GetMapping("/runs/{runId}/results")
	public ResponseEntity<List<JsonNode>> getRunResults(@PathVariable String runId) throws Exception {
		List<JsonNode> results = apifyService.getRunResultsList(runId);
		return ResponseEntity.ok(results);
	}
}

package com.bookishbroccoli.task;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskRepository repository;
	private final TaskProcessor processor;

	public TaskController(TaskRepository repository, TaskProcessor processor) {
		this.repository = repository;
		this.processor = processor;
	}

	@PostMapping("/enqueue")
	public ResponseEntity<Map<String, Object>> enqueue(@RequestBody Map<String, Object> body) {
		String taskType = (String) body.get("taskType");
		if (taskType == null || taskType.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "taskType is required"));
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> payload = (Map<String, Object>) body.getOrDefault("payload", Map.of());
		int maxAttempts = body.containsKey("maxAttempts")
				? ((Number) body.get("maxAttempts")).intValue()
				: 3;

		TaskRecord task = TaskRecord.create(taskType, payload, maxAttempts);
		repository.save(task);

		return ResponseEntity.ok(task.toMap());
	}

	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> getTasks(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String taskType,
			@RequestParam(defaultValue = "50") int limit) {

		TaskStatus taskStatus = status != null ? TaskStatus.fromString(status) : null;
		List<TaskRecord> tasks = repository.findByFilters(taskStatus, taskType, limit);

		List<Map<String, Object>> result = tasks.stream()
				.map(TaskRecord::toMap)
				.toList();
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{taskId}")
	public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
		return repository.findById(taskId)
				.map(task -> ResponseEntity.ok(task.toMap()))
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getStatus() {
		return ResponseEntity.ok(processor.getStatus());
	}
}

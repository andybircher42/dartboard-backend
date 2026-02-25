package com.bookishbroccoli.task;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for task queue management. Base path: {@code /api/tasks}. */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskRepository repository;
  private final TaskProcessor processor;

  /**
   * Constructs a new {@code TaskController}.
   *
   * @param repository the task persistence layer
   * @param processor the background task processor
   */
  public TaskController(TaskRepository repository, TaskProcessor processor) {
    this.repository = repository;
    this.processor = processor;
  }

  /**
   * Enqueues a new task. Expects a JSON body with {@code taskType} (required), {@code payload}, and
   * optional {@code maxAttempts}.
   *
   * @param body the request body containing task parameters
   * @return the created task record, or a 400 response if {@code taskType} is missing
   */
  @PostMapping("/enqueue")
  public ResponseEntity<Map<String, Object>> enqueue(@RequestBody Map<String, Object> body) {
    String taskType = (String) body.get("taskType");
    if (taskType == null || taskType.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "taskType is required"));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) body.getOrDefault("payload", Map.of());
    int maxAttempts =
        body.containsKey("maxAttempts") ? ((Number) body.get("maxAttempts")).intValue() : 3;

    TaskRecord task = TaskRecord.create(taskType, payload, maxAttempts);
    repository.save(task);

    return ResponseEntity.ok(task.toMap());
  }

  /**
   * Lists tasks with optional {@code status} and {@code taskType} filters.
   *
   * @param status optional status filter (case-insensitive)
   * @param taskType optional task type filter
   * @param limit the maximum number of tasks to return (default 50)
   * @return a list of matching task records
   */
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> getTasks(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String taskType,
      @RequestParam(defaultValue = "50") int limit) {

    TaskStatus taskStatus = status != null ? TaskStatus.fromString(status) : null;
    List<TaskRecord> tasks = repository.findByFilters(taskStatus, taskType, limit);

    List<Map<String, Object>> result = tasks.stream().map(TaskRecord::toMap).toList();
    return ResponseEntity.ok(result);
  }

  /**
   * Retrieves a single task by ID.
   *
   * @param taskId the task identifier
   * @return the task record, or 404 if not found
   */
  @GetMapping("/{taskId}")
  public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
    return repository
        .findById(taskId)
        .map(task -> ResponseEntity.ok(task.toMap()))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Returns the current processor status and queue counts.
   *
   * @return a map of processor status information
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    return ResponseEntity.ok(processor.getStatus());
  }
}

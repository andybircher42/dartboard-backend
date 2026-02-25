package com.dartboardbackend.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
class TaskControllerTest {

  private InMemoryTaskRepository repo;
  private TaskProcessor processor;
  private TaskController controller;

  @BeforeEach
  void setUp() {
    repo = new InMemoryTaskRepository();
    TaskProcessorProperties props = new TaskProcessorProperties();
    props.setNumWorkers(1);
    props.setPollIntervalSeconds(60);
    processor = new TaskProcessor(repo, props, List.of());
    controller = new TaskController(repo, processor);
  }

  @Test
  void enqueue_validRequest() {
    var response =
        controller.enqueue(
            Map.of("taskType", "my_task", "payload", Map.of("key", "val"), "maxAttempts", 5));

    assertEquals(200, response.getStatusCode().value());
    assertEquals("my_task", response.getBody().get("taskType"));
    assertEquals("pending", response.getBody().get("status"));
    assertEquals(5, response.getBody().get("maxAttempts"));
    assertNotNull(response.getBody().get("id"));
  }

  @Test
  void enqueue_defaultMaxAttempts() {
    var response = controller.enqueue(Map.of("taskType", "my_task"));

    assertEquals(200, response.getStatusCode().value());
    assertEquals(3, response.getBody().get("maxAttempts"));
  }

  @Test
  void enqueue_missingTaskType_returnsBadRequest() {
    var response = controller.enqueue(Map.of("payload", Map.of()));

    assertEquals(400, response.getStatusCode().value());
    assertTrue(response.getBody().get("error").toString().contains("taskType"));
  }

  @Test
  void enqueue_blankTaskType_returnsBadRequest() {
    var response = controller.enqueue(Map.of("taskType", "  "));

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void getTasks_noFilters() {
    controller.enqueue(Map.of("taskType", "a"));
    controller.enqueue(Map.of("taskType", "b"));

    var response = controller.getTasks(null, null, 50);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(2, response.getBody().size());
  }

  @Test
  void getTasks_filterByTaskType() {
    controller.enqueue(Map.of("taskType", "a"));
    controller.enqueue(Map.of("taskType", "b"));

    var response = controller.getTasks(null, "a", 50);

    assertEquals(1, response.getBody().size());
    assertEquals("a", response.getBody().get(0).get("taskType"));
  }

  @Test
  void getTasks_filterByStatus() {
    controller.enqueue(Map.of("taskType", "a"));

    var response = controller.getTasks("pending", null, 50);

    assertEquals(1, response.getBody().size());
  }

  @Test
  void getTask_found() {
    var enqueued = controller.enqueue(Map.of("taskType", "test"));
    String taskId = (String) enqueued.getBody().get("id");

    var response = controller.getTask(taskId);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(taskId, response.getBody().get("id"));
  }

  @Test
  void getTask_notFound() {
    var response = controller.getTask("nonexistent");

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void getStatus_returnsProcessorStatus() {
    var response = controller.getStatus();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody().get("workers"));
    assertNotNull(response.getBody().get("queueCounts"));
  }
}

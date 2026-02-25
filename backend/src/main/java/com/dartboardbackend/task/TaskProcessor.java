package com.dartboardbackend.task;

import com.dartboardbackend.retry.NonRetryableException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Background service that polls for pending tasks, dispatches them to registered {@link TaskHandler
 * TaskHandlers} via a worker pool, and manages task lifecycle (completion, failure, recovery).
 */
@Service
@SuppressWarnings("unused") // var unused captures Future returns required by Error Prone
public class TaskProcessor {

  private static final Logger log = LoggerFactory.getLogger(TaskProcessor.class);
  private static final int TASK_BATCH_LIMIT = 10;

  private final TaskRepository repository;
  private final TaskProcessorProperties properties;
  private final Map<String, TaskHandler> handlerMap;

  private ExecutorService workerPool;
  private ScheduledExecutorService scheduler;
  private final Set<String> activeWorkers = ConcurrentHashMap.newKeySet();
  private final AtomicInteger workerIdCounter = new AtomicInteger(0);

  /**
   * Constructs a new {@code TaskProcessor}.
   *
   * @param repository the task persistence layer
   * @param properties configuration properties (worker count, poll interval)
   * @param handlers the list of available task handlers, keyed by their task type
   */
  public TaskProcessor(
      TaskRepository repository, TaskProcessorProperties properties, List<TaskHandler> handlers) {
    this.repository = repository;
    this.properties = properties;
    this.handlerMap = new LinkedHashMap<>();
    for (TaskHandler handler : handlers) {
      this.handlerMap.put(handler.getTaskType(), handler);
    }
  }

  /**
   * Initializes the worker pool and starts polling for pending tasks. Called automatically after
   * construction by Spring.
   */
  @PostConstruct
  public void init() {
    int numWorkers = properties.getNumWorkers();
    workerPool = Executors.newFixedThreadPool(numWorkers);
    scheduler = Executors.newSingleThreadScheduledExecutor();

    int recovered = recoverStaleTasks();
    if (recovered > 0) {
      log.info("Recovered {} stale processing tasks", recovered);
    }

    startPolling();
    log.info("TaskProcessor initialized with {} workers", numWorkers);
  }

  /**
   * Resets any tasks left in PROCESSING state back to PENDING (recovery after unclean shutdown).
   */
  int recoverStaleTasks() {
    return repository.resetProcessingTasks();
  }

  /** Schedules the periodic poll that checks for pending tasks across all registered types. */
  void startPolling() {
    var unused =
        scheduler.scheduleAtFixedRate(
            this::processPendingTasks, 0, properties.getPollIntervalSeconds(), TimeUnit.SECONDS);
  }

  /** Iterates over all registered task types and attempts to claim and dispatch pending tasks. */
  void processPendingTasks() {
    for (String taskType : handlerMap.keySet()) {
      claimAndDispatch(taskType, TASK_BATCH_LIMIT);
    }
  }

  /**
   * Claims up to {@code limit} pending tasks of the given type and submits them to the worker pool.
   */
  void claimAndDispatch(String taskType, int limit) {
    for (int i = 0; i < limit; i++) {
      String workerId = String.format("worker-%d", workerIdCounter.incrementAndGet());
      Optional<TaskRecord> claimed = repository.claimNextPending(taskType, workerId);
      if (claimed.isEmpty()) {
        break;
      }
      TaskRecord task = claimed.get();
      activeWorkers.add(workerId);
      var unused =
          workerPool.submit(
              () -> {
                try {
                  processTask(task);
                } finally {
                  activeWorkers.remove(task.getWorkerId().orElse(workerId));
                }
              });
    }
  }

  /**
   * Looks up the handler for the task type and executes it, recording the outcome in the
   * repository.
   */
  void processTask(TaskRecord task) {
    String taskType = task.getTaskType();
    TaskHandler handler = handlerMap.get(taskType);

    if (handler == null) {
      String error = String.format("No handler registered for task type: %s", taskType);
      log.error(error);
      repository.failPermanently(task.getId(), error);
      return;
    }

    try {
      TaskResult result = handler.handle(task);
      if (result.succeeded()) {
        repository.complete(task.getId(), result.resultCount());
        log.info("Task {} completed with {} results", task.getId(), result.resultCount());
      } else {
        repository.fail(task.getId(), result.error().orElse(null), true);
        log.warn("Task {} failed: {}", task.getId(), result.error().orElse("unknown"));
      }
    } catch (NonRetryableException e) {
      repository.failPermanently(task.getId(), e.getMessage());
      log.error("Task {} failed permanently: {}", task.getId(), e.getMessage());
    } catch (Exception e) {
      repository.fail(task.getId(), e.getMessage(), true);
      log.warn("Task {} failed with exception: {}", task.getId(), e.getMessage());
    }
  }

  /**
   * Returns a snapshot of the processor state including worker count, active workers, registered
   * task types, and queue counts.
   *
   * @return an ordered map of processor status information
   */
  public Map<String, Object> getStatus() {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("workers", properties.getNumWorkers());
    status.put("activeWorkers", activeWorkers.size());
    status.put("registeredTypes", new ArrayList<>(handlerMap.keySet()));
    status.put("queueCounts", repository.getQueueCounts());
    return status;
  }

  /**
   * Gracefully shuts down the worker pool and scheduler, resetting any in-flight tasks back to
   * {@link TaskStatus#PENDING}.
   */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down TaskProcessor...");

    if (scheduler != null) {
      scheduler.shutdown();
    }

    if (workerPool != null) {
      workerPool.shutdown();
      try {
        if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
          workerPool.shutdownNow();
        }
      } catch (InterruptedException e) {
        workerPool.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    int reset = repository.resetProcessingTasks();
    if (reset > 0) {
      log.info("Reset {} processing tasks on shutdown", reset);
    }

    activeWorkers.clear();
    log.info("TaskProcessor shut down");
  }

  /** Returns an unmodifiable view of the registered handler map. Visible for testing. */
  Map<String, TaskHandler> getHandlerMap() {
    return Collections.unmodifiableMap(handlerMap);
  }

  /** Returns an unmodifiable view of the currently active worker IDs. Visible for testing. */
  Set<String> getActiveWorkers() {
    return Collections.unmodifiableSet(activeWorkers);
  }
}

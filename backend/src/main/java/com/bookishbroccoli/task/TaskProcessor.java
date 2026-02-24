package com.bookishbroccoli.task;

import com.bookishbroccoli.service.ApifyNonRetryableException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
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

	public TaskProcessor(TaskRepository repository,
						 TaskProcessorProperties properties,
						 List<TaskHandler> handlers) {
		this.repository = repository;
		this.properties = properties;
		this.handlerMap = new LinkedHashMap<>();
		for (TaskHandler handler : handlers) {
			this.handlerMap.put(handler.getTaskType(), handler);
		}
	}

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

	int recoverStaleTasks() {
		return repository.resetProcessingTasks();
	}

	void startPolling() {
		scheduler.scheduleAtFixedRate(
				this::processPendingTasks,
				0,
				properties.getPollIntervalSeconds(),
				TimeUnit.SECONDS
		);
	}

	void processPendingTasks() {
		for (String taskType : handlerMap.keySet()) {
			claimAndDispatch(taskType, TASK_BATCH_LIMIT);
		}
	}

	void claimAndDispatch(String taskType, int limit) {
		for (int i = 0; i < limit; i++) {
			String workerId = "worker-" + workerIdCounter.incrementAndGet();
			Optional<TaskRecord> claimed = repository.claimNextPending(taskType, workerId);
			if (claimed.isEmpty()) {
				break;
			}
			TaskRecord task = claimed.get();
			activeWorkers.add(workerId);
			workerPool.submit(() -> {
				try {
					processTask(task);
				} finally {
					activeWorkers.remove(task.getWorkerId() != null ? task.getWorkerId() : workerId);
				}
			});
		}
	}

	void processTask(TaskRecord task) {
		String taskType = task.getTaskType();
		TaskHandler handler = handlerMap.get(taskType);

		if (handler == null) {
			String error = "No handler registered for task type: " + taskType;
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
				repository.fail(task.getId(), result.error(), true);
				log.warn("Task {} failed: {}", task.getId(), result.error());
			}
		} catch (ApifyNonRetryableException e) {
			repository.failPermanently(task.getId(), e.getMessage());
			log.error("Task {} failed permanently: {}", task.getId(), e.getMessage());
		} catch (Exception e) {
			repository.fail(task.getId(), e.getMessage(), true);
			log.warn("Task {} failed with exception: {}", task.getId(), e.getMessage());
		}
	}

	public Map<String, Object> getStatus() {
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("workers", properties.getNumWorkers());
		status.put("activeWorkers", activeWorkers.size());
		status.put("registeredTypes", new ArrayList<>(handlerMap.keySet()));
		status.put("queueCounts", repository.getQueueCounts());
		return status;
	}

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

	// Visible for testing
	Map<String, TaskHandler> getHandlerMap() {
		return Collections.unmodifiableMap(handlerMap);
	}

	Set<String> getActiveWorkers() {
		return Collections.unmodifiableSet(activeWorkers);
	}
}

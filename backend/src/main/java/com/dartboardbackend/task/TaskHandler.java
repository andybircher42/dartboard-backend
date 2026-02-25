package com.dartboardbackend.task;

/**
 * Strategy interface for processing a specific type of task. Implementations are registered by task
 * type and invoked by the {@link TaskProcessor}.
 */
public interface TaskHandler {

  /**
   * Returns the task type string this handler processes.
   *
   * @return the task type identifier
   */
  String getTaskType();

  /**
   * Executes the task and returns a result indicating success or failure.
   *
   * @param task the task record to process
   * @return a {@link TaskResult} indicating the outcome
   */
  TaskResult handle(TaskRecord task);
}

package com.dartboardbackend.task;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring configuration properties for the {@link TaskProcessor}. Bound to the {@code
 * task.processor} prefix.
 */
@Component
@ConfigurationProperties(prefix = "task.processor")
public class TaskProcessorProperties {

  private int numWorkers = 5;
  private int pollIntervalSeconds = 5;

  /** Returns the number of worker threads in the processing pool. */
  public int getNumWorkers() {
    return numWorkers;
  }

  /** Sets the number of worker threads in the processing pool. */
  public void setNumWorkers(int numWorkers) {
    this.numWorkers = numWorkers;
  }

  /** Returns the interval in seconds between polling cycles. */
  public int getPollIntervalSeconds() {
    return pollIntervalSeconds;
  }

  /** Sets the interval in seconds between polling cycles. */
  public void setPollIntervalSeconds(int pollIntervalSeconds) {
    this.pollIntervalSeconds = pollIntervalSeconds;
  }
}

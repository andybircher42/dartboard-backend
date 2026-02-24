package com.bookishbroccoli.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.function.LongConsumer;

@Service
public class RetryExecutor {

	private static final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);

	private final LongConsumer sleeper;

	public RetryExecutor() {
		this(ms -> {
			try {
				Thread.sleep(ms);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		});
	}

	RetryExecutor(LongConsumer sleeper) {
		this.sleeper = sleeper;
	}

	public <T> RetryResult<T> execute(RetryPolicy policy, Callable<T> operation) {
		Exception lastRetryable = null;

		for (int attempt = 1; attempt <= policy.getMaxAttempts(); attempt++) {
			if (attempt > 1) {
				long backoffMs = policy.getBackoffMs(attempt - 1);
				logger.warn("Retry attempt {}/{} for policy '{}' after {}ms backoff",
						attempt, policy.getMaxAttempts(), policy.getName(), backoffMs);
				try {
					sleeper.accept(backoffMs);
				} catch (RuntimeException e) {
					if (e.getCause() instanceof InterruptedException) {
						Thread.currentThread().interrupt();
						return RetryResult.failure(attempt - 1, lastRetryable != null ? lastRetryable : e);
					}
					throw e;
				}
			}

			try {
				T result = operation.call();
				return RetryResult.success(result, attempt);
			} catch (NonRetryableException e) {
				return RetryResult.failure(attempt, e);
			} catch (RetryableException e) {
				lastRetryable = e;
				logger.warn("Retryable error on attempt {}/{} for policy '{}': {}",
						attempt, policy.getMaxAttempts(), policy.getName(), e.getMessage());
			} catch (Exception e) {
				return RetryResult.failure(attempt, e);
			}
		}

		return RetryResult.failure(policy.getMaxAttempts(), lastRetryable);
	}
}

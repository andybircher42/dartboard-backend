package com.bookishbroccoli.task;

import java.util.Optional;

public record TaskResult(boolean succeeded, int resultCount, Optional<String> error) {

	public static TaskResult success(int resultCount) {
		return new TaskResult(true, resultCount, Optional.empty());
	}

	public static TaskResult success() {
		return new TaskResult(true, 0, Optional.empty());
	}

	public static TaskResult failure(String error) {
		return new TaskResult(false, 0, Optional.of(error));
	}
}

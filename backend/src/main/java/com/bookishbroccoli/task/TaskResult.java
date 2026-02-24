package com.bookishbroccoli.task;

public record TaskResult(boolean succeeded, int resultCount, String error) {

	public static TaskResult success(int resultCount) {
		return new TaskResult(true, resultCount, null);
	}

	public static TaskResult success() {
		return new TaskResult(true, 0, null);
	}

	public static TaskResult failure(String error) {
		return new TaskResult(false, 0, error);
	}
}

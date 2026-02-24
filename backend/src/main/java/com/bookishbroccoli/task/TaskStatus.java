package com.bookishbroccoli.task;

public enum TaskStatus {
	PENDING("pending"),
	PROCESSING("processing"),
	COMPLETED("completed"),
	FAILED("failed");

	private final String value;

	TaskStatus(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public boolean isDone() {
		return this == COMPLETED || this == FAILED;
	}

	public boolean isRunning() {
		return this == PROCESSING;
	}

	public static TaskStatus resolve(String status) {
		return fromString(status);
	}

	public static TaskStatus fromString(String status) {
		if (status == null) {
			return PENDING;
		}
		for (TaskStatus s : values()) {
			if (s.value.equalsIgnoreCase(status) || s.name().equalsIgnoreCase(status)) {
				return s;
			}
		}
		return PENDING;
	}

	@Override
	public String toString() {
		return value;
	}
}

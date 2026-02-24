package com.bookishbroccoli.job;

public enum JobStatus {
	PENDING("pending"),
	RUNNING("running"),
	SUCCEEDED("succeeded"),
	FAILED("failed");

	private final String value;

	JobStatus(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public boolean isTerminal() {
		return this == SUCCEEDED || this == FAILED;
	}

	public boolean isRunning() {
		return this == RUNNING;
	}

	public static JobStatus fromString(String status) {
		if (status == null) {
			return PENDING;
		}
		for (JobStatus s : values()) {
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

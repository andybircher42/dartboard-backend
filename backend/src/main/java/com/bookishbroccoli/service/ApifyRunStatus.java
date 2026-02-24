package com.bookishbroccoli.service;

public enum ApifyRunStatus {
	UNKNOWN("UNKNOWN"),
	RUNNING("RUNNING"),
	SUCCEEDED("SUCCEEDED"),
	FAILED("FAILED"),
	ABORTED("ABORTED"),
	TIMED_OUT("TIMED-OUT"),
	READY("READY");

	private final String value;

	ApifyRunStatus(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public boolean isTerminal() {
		return this == SUCCEEDED || this == FAILED || this == ABORTED || this == TIMED_OUT;
	}

	public static ApifyRunStatus fromString(String status) {
		for (ApifyRunStatus s : values()) {
			if (s.value.equals(status)) {
				return s;
			}
		}
		return UNKNOWN;
	}

	@Override
	public String toString() {
		return value;
	}
}

package com.bookishbroccoli.task;

public interface TaskHandler {

	String getTaskType();

	TaskResult handle(TaskRecord task);
}

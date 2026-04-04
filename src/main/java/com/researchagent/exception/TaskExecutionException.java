package com.researchagent.exception;

/**
 * Specific exception for task execution failures.
 *
 * BENEFIT: Allows controllers and services to handle execution errors differently
 */
public class TaskExecutionException extends AgentException {

    private final String taskId;

    public TaskExecutionException(String message, String taskId) {
        super(message, AgentErrorCode.EXECUTION_FAILED);
        this.taskId = taskId;
    }

    public TaskExecutionException(String message, String taskId, Throwable cause) {
        super(message, AgentErrorCode.EXECUTION_FAILED, cause);
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}


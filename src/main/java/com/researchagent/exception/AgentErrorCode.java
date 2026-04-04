package com.researchagent.exception;

/**
 * Error codes for agent operations.
 *
 * BENEFIT: Standardized error classification for logging, metrics, and API responses
 */
public enum AgentErrorCode {
    UNKNOWN_ERROR("UNKNOWN", "An unknown error occurred"),
    INVALID_GOAL("INVALID_GOAL", "Invalid agent goal provided"),
    EXECUTION_FAILED("EXEC_FAILED", "Task execution failed"),
    TOOL_EXECUTION_FAILED("TOOL_FAILED", "Tool execution failed"),
    INVALID_DECISION("INVALID_DECISION", "Agent returned invalid decision"),
    UNKNOWN_TOOL("UNKNOWN_TOOL", "Unknown tool requested"),
    REPEATED_TOOL_CALL("REPEATED_CALL", "Repeated tool call detected"),
    MAX_STEPS_EXCEEDED("MAX_STEPS", "Maximum execution steps exceeded"),
    INVALID_JSON("INVALID_JSON", "Agent returned invalid JSON"),
    TASK_NOT_FOUND("NOT_FOUND", "Task not found"),
    DATABASE_ERROR("DB_ERROR", "Database operation failed");

    private final String code;
    private final String message;

    AgentErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}


package com.researchagent.exception;

/**
 * Specific exception for tool execution failures.
 *
 * BENEFIT: Allows differentiation between agent logic errors and tool errors
 */
public class ToolExecutionException extends AgentException {

    private final String toolName;
    private final String toolInput;

    public ToolExecutionException(String message, String toolName, String toolInput) {
        super(message, AgentErrorCode.TOOL_EXECUTION_FAILED);
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    public ToolExecutionException(String message, String toolName, String toolInput, Throwable cause) {
        super(message, AgentErrorCode.TOOL_EXECUTION_FAILED, cause);
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolInput() {
        return toolInput;
    }
}


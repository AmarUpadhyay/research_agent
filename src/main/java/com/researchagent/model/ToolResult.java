package com.researchagent.model;

public class ToolResult {

    private final String toolName;
    private final boolean success;
    private final String output;

    public ToolResult(String toolName, boolean success, String output) {
        this.toolName = toolName;
        this.success = success;
        this.output = output;
    }

    public String getToolName() {
        return toolName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }
}

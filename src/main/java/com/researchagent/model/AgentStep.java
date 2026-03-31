package com.researchagent.model;

public class AgentStep {

    private final int stepNumber;
    private final AgentStepType type;
    private final String content;
    private final ToolResult toolResult;

    public AgentStep(int stepNumber, AgentStepType type, String content, ToolResult toolResult) {
        this.stepNumber = stepNumber;
        this.type = type;
        this.content = content;
        this.toolResult = toolResult;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public AgentStepType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public ToolResult getToolResult() {
        return toolResult;
    }
}

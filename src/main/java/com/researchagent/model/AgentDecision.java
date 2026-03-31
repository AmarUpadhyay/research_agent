package com.researchagent.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentDecision {

    private String decisionType;
    private String summary;
    private String toolName;
    private Map<String, Object> toolInput = new LinkedHashMap<>();
    private String finalResponse;

    public String getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(String decisionType) {
        this.decisionType = decisionType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getToolInput() {
        return toolInput;
    }

    public void setToolInput(Map<String, Object> toolInput) {
        this.toolInput = toolInput == null ? new LinkedHashMap<>() : new LinkedHashMap<>(toolInput);
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
    }
}

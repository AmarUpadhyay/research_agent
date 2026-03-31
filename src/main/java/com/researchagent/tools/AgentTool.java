package com.researchagent.tools;

import com.researchagent.model.ToolResult;

import java.util.Map;

public interface AgentTool {

    String getName();

    String getDescription();

    ToolResult execute(Map<String, Object> input);

    default String getPromptContext() {
        return "";
    }
}

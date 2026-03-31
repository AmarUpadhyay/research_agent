package com.researchagent.tools;

import com.researchagent.model.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoggingTool implements AgentTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingTool.class);

    @Override
    public String getName() {
        return "logging";
    }

    @Override
    public String getDescription() {
        return "Write an audit log message.";
    }

    @Override
    public ToolResult execute(Map<String, Object> input) {
        String message = stringValue(input, "message", "No message provided");
        LOGGER.info(message);
        return new ToolResult(getName(), true, "Logged message: " + message);
    }

    private String stringValue(Map<String, Object> input, String key, String fallback) {
        Object value = input.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}

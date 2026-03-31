package com.researchagent.tools;

import com.researchagent.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailTool implements AgentTool {

    @Override
    public String getName() {
        return "email";
    }

    @Override
    public String getDescription() {
        return "Send a notification email with recipient, subject, and body.";
    }

    @Override
    public ToolResult execute(Map<String, Object> input) {
        String recipient = stringValue(input, "recipient", "ops@research-agent.local");
        String subject = stringValue(input, "subject", "Agent notification");
        String body = stringValue(input, "body", "");
        String output = "Email queued for " + recipient + " with subject '" + subject + "' and body '" + body + "'";
        return new ToolResult(getName(), true, output);
    }

    private String stringValue(Map<String, Object> input, String key, String fallback) {
        Object value = input.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}

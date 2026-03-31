package com.researchagent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchagent.memory.InMemoryStore;
import com.researchagent.model.AgentDecision;
import com.researchagent.model.AgentStep;
import com.researchagent.model.AgentStepType;
import com.researchagent.model.AgentTask;
import com.researchagent.model.AgentTaskStatus;
import com.researchagent.model.ToolResult;
import com.researchagent.tools.AgentTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AgentExecutor {

    private static final int DEFAULT_MAX_STEPS = 5;

    private final TaskAgent taskAgent;
    private final InMemoryStore inMemoryStore;
    private final ObjectMapper objectMapper;
    private final Map<String, AgentTool> toolsByName;

    public AgentExecutor(
            TaskAgent taskAgent,
            InMemoryStore inMemoryStore,
            ObjectMapper objectMapper,
            List<AgentTool> tools) {
        this.taskAgent = taskAgent;
        this.inMemoryStore = inMemoryStore;
        this.objectMapper = objectMapper;
        this.toolsByName = tools.stream()
                .collect(Collectors.toUnmodifiableMap(AgentTool::getName, tool -> tool));
    }

    public AgentTask run(String goal) {
        AgentTask task = inMemoryStore.createTask(goal, DEFAULT_MAX_STEPS);
        task.setStatus(AgentTaskStatus.RUNNING);
        inMemoryStore.saveTask(task);

        for (int stepNumber = 1; stepNumber <= task.getMaxSteps(); stepNumber++) {
            String prompt = buildPrompt(task, stepNumber);
            String rawDecision = taskAgent.decideNextAction(prompt);
            String normalizedDecision = normalizeDecision(rawDecision);

            AgentDecision decision;
            try {
                decision = objectMapper.readValue(normalizedDecision, AgentDecision.class);
            } catch (JsonProcessingException ex) {
                task.addStep(new AgentStep(stepNumber, AgentStepType.ERROR,
                        "Agent returned invalid JSON: " + rawDecision, null));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution failed because the agent returned an invalid structured decision.");
                inMemoryStore.saveTask(task);
                return task;
            }

            task.addStep(new AgentStep(stepNumber, AgentStepType.PLAN,
                    defaultText(decision.getSummary(), "No plan summary provided."), null));

            if ("FINAL".equalsIgnoreCase(decision.getDecisionType())) {
                String finalResponse = defaultText(decision.getFinalResponse(), decision.getSummary());
                task.addStep(new AgentStep(stepNumber, AgentStepType.FINAL, finalResponse, null));
                task.setStatus(AgentTaskStatus.COMPLETED);
                task.setFinalResponse(finalResponse);
                inMemoryStore.saveTask(task);
                return task;
            }

            if (!"TOOL".equalsIgnoreCase(decision.getDecisionType())) {
                task.addStep(new AgentStep(stepNumber, AgentStepType.ERROR,
                        "Unsupported decision type: " + decision.getDecisionType(), null));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution failed because the agent produced an unsupported decision type.");
                inMemoryStore.saveTask(task);
                return task;
            }

            AgentTool tool = toolsByName.get(decision.getToolName());
            if (tool == null) {
                task.addStep(new AgentStep(stepNumber, AgentStepType.ERROR,
                        "Unknown tool requested: " + decision.getToolName(), null));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution failed because the agent requested an unknown tool.");
                inMemoryStore.saveTask(task);
                return task;
            }

            if (isRepeatedToolCall(task, decision)) {
                task.addStep(new AgentStep(stepNumber, AgentStepType.ERROR,
                        "Repeated tool call blocked for tool '" + decision.getToolName() + "'.", null));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution stopped because the agent repeated the same tool call without making progress.");
                inMemoryStore.saveTask(task);
                return task;
            }

            task.addStep(new AgentStep(stepNumber, AgentStepType.ACTION,
                    "Calling tool '" + tool.getName() + "' with input " + decision.getToolInput(), null));

            ToolResult result = tool.execute(decision.getToolInput());
            task.addStep(new AgentStep(stepNumber, AgentStepType.OBSERVATION, result.getOutput(), result));
            inMemoryStore.saveTask(task);
        }

        task.setStatus(AgentTaskStatus.FAILED);
        task.setFinalResponse("Execution stopped after reaching the maximum number of steps.");
        task.addStep(new AgentStep(task.getMaxSteps(), AgentStepType.ERROR, task.getFinalResponse(), null));
        inMemoryStore.saveTask(task);
        return task;
    }

    private String buildPrompt(AgentTask task, int nextStep) {
        StringBuilder builder = new StringBuilder();
        builder.append("Goal: ").append(task.getGoal()).append("\n");
        builder.append("Next step: ").append(nextStep).append(" of ").append(task.getMaxSteps()).append("\n");
        builder.append("If the goal can be answered directly, return FINAL without using any tool.\n");
        builder.append("Do not use logging for ordinary conversational replies.\n");
        builder.append("Do not repeat the same successful tool call unless a retry is explicitly required.\n");
        builder.append("Available tools:\n");
        for (AgentTool tool : toolsByName.values()) {
            builder.append("- ")
                    .append(tool.getName())
                    .append(": ")
                    .append(tool.getDescription())
                    .append("\n");
        }
        builder.append("Tool context:\n");
        for (AgentTool tool : toolsByName.values()) {
            String context = tool.getPromptContext();
            if (context != null && !context.isBlank()) {
                builder.append("- ")
                        .append(tool.getName())
                        .append(": ")
                        .append(context)
                        .append("\n");
            }
        }
        builder.append("Previous steps:\n");
        if (task.getSteps().isEmpty()) {
            builder.append("- none\n");
        } else {
            for (AgentStep step : task.getSteps()) {
                builder.append("- ")
                        .append(step.getType())
                        .append(": ")
                        .append(step.getContent())
                        .append("\n");
            }
        }
        builder.append("Return the next decision only.");
        return builder.toString();
    }

    private String defaultText(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "";
    }

    private boolean isRepeatedToolCall(AgentTask task, AgentDecision currentDecision) {
        List<AgentStep> steps = task.getSteps();
        AgentStep previousAction = null;
        for (int i = steps.size() - 1; i >= 0; i--) {
            AgentStep step = steps.get(i);
            if (step.getType() == AgentStepType.ACTION) {
                previousAction = step;
                break;
            }
        }

        if (previousAction == null) {
            return false;
        }

        String expectedContent = "Calling tool '" + currentDecision.getToolName() + "' with input " + currentDecision.getToolInput();
        return Objects.equals(previousAction.getContent(), expectedContent);
    }

    private String normalizeDecision(String rawDecision) {
        if (rawDecision == null) {
            return "";
        }

        String normalized = rawDecision.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }

        return normalized.trim();
    }
}

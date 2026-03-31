package com.researchagent.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AgentTask {

    private final String taskId;
    private final String goal;
    private final int maxSteps;
    private final Instant createdAt;
    private Instant updatedAt;
    private AgentTaskStatus status;
    private String finalResponse;
    private final List<AgentStep> steps = new ArrayList<>();

    public AgentTask(String taskId, String goal, int maxSteps) {
        this.taskId = taskId;
        this.goal = goal;
        this.maxSteps = maxSteps;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.status = AgentTaskStatus.PENDING;
    }

    public void addStep(AgentStep step) {
        steps.add(step);
        updatedAt = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getGoal() {
        return goal;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public AgentTaskStatus getStatus() {
        return status;
    }

    public void setStatus(AgentTaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
        this.updatedAt = Instant.now();
    }

    public List<AgentStep> getSteps() {
        return List.copyOf(steps);
    }
}

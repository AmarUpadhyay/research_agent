package com.researchagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.researchagent.model.AgentTask;
import com.researchagent.model.AgentTaskStatus;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for task execution.
 *
 * BENEFITS:
 * ✅ Hides internal implementation details
 * ✅ Provides clean API contract
 * ✅ Easy to evolve API without breaking clients
 */
public class ExecuteTaskResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("goal")
    private String goal;

    @JsonProperty("status")
    private String status;

    @JsonProperty("final_response")
    private String finalResponse;

    @JsonProperty("steps_count")
    private int stepsCount;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    // Static factory method for better testability
    public static ExecuteTaskResponse from(AgentTask task) {
        ExecuteTaskResponse response = new ExecuteTaskResponse();
        response.id = task.getTaskId();
        response.goal = task.getGoal();
        response.status = task.getStatus().name();
        response.finalResponse = task.getFinalResponse();
        response.stepsCount = task.getSteps().size();
        response.createdAt = task.getCreatedAt();
        response.updatedAt = task.getUpdatedAt();
        return response;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
    }

    public int getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(int stepsCount) {
        this.stepsCount = stepsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}


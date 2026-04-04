package com.researchagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Request DTO for executing a task.
 *
 * BENEFITS:
 * ✅ Decouples API contract from domain models
 * ✅ Enables request validation
 * ✅ Allows API versioning
 * ✅ Better documentation
 */
public class ExecuteTaskRequest {

    @NotBlank(message = "Task goal cannot be blank")
    @Size(min = 3, max = 1000, message = "Task goal must be between 3 and 1000 characters")
    @JsonProperty("task")
    private String task;

    @Min(value = 1, message = "Max steps must be at least 1")
    @Max(value = 50, message = "Max steps cannot exceed 50")
    @JsonProperty("max_steps")
    private Integer maxSteps = 5;

    // Constructors
    public ExecuteTaskRequest() {
    }

    public ExecuteTaskRequest(String task) {
        this.task = task;
    }

    public ExecuteTaskRequest(String task, Integer maxSteps) {
        this.task = task;
        this.maxSteps = maxSteps;
    }

    // Getters and Setters
    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Integer getMaxSteps() {
        return maxSteps != null ? maxSteps : 5;
    }

    public void setMaxSteps(Integer maxSteps) {
        this.maxSteps = maxSteps;
    }
}


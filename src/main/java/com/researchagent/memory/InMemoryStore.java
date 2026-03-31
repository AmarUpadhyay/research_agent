package com.researchagent.memory;

import com.researchagent.model.AgentTask;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryStore {

    private final Map<String, AgentTask> tasks = new ConcurrentHashMap<>();

    public AgentTask createTask(String goal, int maxSteps) {
        AgentTask task = new AgentTask(UUID.randomUUID().toString(), goal, maxSteps);
        tasks.put(task.getTaskId(), task);
        return task;
    }

    public void saveTask(AgentTask task) {
        tasks.put(task.getTaskId(), task);
    }

    public Optional<AgentTask> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}

package com.researchagent.service;

import com.researchagent.agent.AgentExecutor;
import com.researchagent.memory.InMemoryStore;
import com.researchagent.model.AgentStep;
import com.researchagent.model.AgentTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private final AgentExecutor agentExecutor;
    private final InMemoryStore inMemoryStore;

    public AgentService(AgentExecutor agentExecutor, InMemoryStore inMemoryStore) {
        this.agentExecutor = agentExecutor;
        this.inMemoryStore = inMemoryStore;
    }

    public AgentTask execute(String goal) {
        return agentExecutor.run(goal);
    }

    public AgentTask getTask(String taskId) {
        return inMemoryStore.getTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    public List<AgentStep> getTaskSteps(String taskId) {
        return getTask(taskId).getSteps();
    }
}

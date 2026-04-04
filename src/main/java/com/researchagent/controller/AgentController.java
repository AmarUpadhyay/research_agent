package com.researchagent.controller;

import com.researchagent.model.AgentStep;
import com.researchagent.model.AgentTask;
import com.researchagent.model.TaskRequest;
import com.researchagent.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Agent Controller - REST API endpoints for autonomous AI agent
 * Handles task execution, status retrieval, and step history
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "up"));
    }

    @PostMapping("/tasks")
    public ResponseEntity<AgentTask> executeTask(@RequestBody TaskRequest request) {
        return ResponseEntity.ok(agentService.execute(request.getTask()));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<AgentTask> getTask(@PathVariable String taskId) {
        return ResponseEntity.ok(agentService.getTask(taskId));
    }

    @GetMapping("/tasks/{taskId}/steps")
    public ResponseEntity<List<AgentStep>> getTaskSteps(@PathVariable String taskId) {
        return ResponseEntity.ok(agentService.getTaskSteps(taskId));
    }
}

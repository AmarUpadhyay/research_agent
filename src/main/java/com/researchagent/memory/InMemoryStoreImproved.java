package com.researchagent.memory;

import com.researchagent.model.AgentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Improved in-memory store with task TTL and automatic cleanup.
 *
 * IMPROVEMENTS:
 * ✅ Task TTL to prevent memory leaks
 * ✅ Scheduled cleanup job
 * ✅ Logging for observability
 * ✅ Configurable TTL
 * ✅ Metrics on cleanup
 *
 * NOTE: For production with distributed systems, replace with:
 * - Spring Data Redis
 * - Spring Data JPA (PostgreSQL)
 * - Hazelcast
 */
@Component
@EnableScheduling
public class InMemoryStoreImproved {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryStoreImproved.class);

    private final Map<String, CachedTask> tasks = new ConcurrentHashMap<>();

    @Value("${agent.task-ttl-minutes:30}")
    private int taskTtlMinutes;

    /**
     * Create a new task.
     */
    public AgentTask createTask(String goal, int maxSteps) {
        AgentTask task = new AgentTask(UUID.randomUUID().toString(), goal, maxSteps);
        CachedTask cachedTask = new CachedTask(task, Instant.now());
        tasks.put(task.getTaskId(), cachedTask);

        LOGGER.debug("Created new task: {} (TTL: {} minutes)", task.getTaskId(), taskTtlMinutes);
        return task;
    }

    /**
     * Save or update a task.
     */
    public void saveTask(AgentTask task) {
        CachedTask cachedTask = tasks.get(task.getTaskId());
        if (cachedTask != null) {
            cachedTask.updateAccessTime();
            tasks.put(task.getTaskId(), cachedTask);
            LOGGER.debug("Updated task: {}", task.getTaskId());
        } else {
            tasks.put(task.getTaskId(), new CachedTask(task, Instant.now()));
            LOGGER.debug("Saved new task: {}", task.getTaskId());
        }
    }

    /**
     * Retrieve a task by ID.
     */
    public Optional<AgentTask> getTask(String taskId) {
        CachedTask cached = tasks.get(taskId);
        if (cached != null) {
            cached.updateAccessTime();
            return Optional.of(cached.task);
        }
        return Optional.empty();
    }

    /**
     * Scheduled cleanup job - removes expired tasks every 5 minutes.
     *
     * BENEFIT: Prevents unbounded memory growth
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void cleanupExpiredTasks() {
        Duration ttl = Duration.ofMinutes(taskTtlMinutes);
        Instant cutoff = Instant.now().minus(ttl);

        int initialSize = tasks.size();

        tasks.entrySet().removeIf(entry ->
            entry.getValue().isExpired(cutoff)
        );

        int removed = initialSize - tasks.size();
        if (removed > 0) {
            LOGGER.info("Cleaned up {} expired tasks (TTL: {} minutes)", removed, taskTtlMinutes);
        }
    }

    /**
     * Get current store size (for monitoring).
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * Internal class to track task and its metadata.
     */
    private static class CachedTask {
        private final AgentTask task;
        private final Instant createdAt;
        private volatile Instant lastAccessedAt;

        CachedTask(AgentTask task, Instant createdAt) {
            this.task = task;
            this.createdAt = createdAt;
            this.lastAccessedAt = createdAt;
        }

        void updateAccessTime() {
            this.lastAccessedAt = Instant.now();
        }

        boolean isExpired(Instant cutoff) {
            return lastAccessedAt.isBefore(cutoff);
        }
    }
}


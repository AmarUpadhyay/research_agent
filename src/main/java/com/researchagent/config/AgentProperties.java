package com.researchagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * Centralized configuration properties for the Agent application.
 *
 * BENEFITS over @Value:
 * ✅ Type-safe configuration
 * ✅ Validation at startup
 * ✅ IDE autocomplete support
 * ✅ Easy to test
 * ✅ Documentation auto-generation
 */
@Configuration
@ConfigurationProperties(prefix = "agent")
@Validated
public class AgentProperties {

    /**
     * Maximum execution steps per task (prevents infinite loops)
     */
    @Min(value = 1, message = "maxSteps must be at least 1")
    @Max(value = 100, message = "maxSteps cannot exceed 100")
    private int maxSteps = 5;

    /**
     * Execution timeout to prevent hanging tasks
     */
    private Duration executionTimeout = Duration.ofSeconds(30);

    /**
     * Cache TTL for database schema context
     */
    private Duration schemaContextCacheTtl = Duration.ofSeconds(60);

    /**
     * Enable request tracing with MDC
     */
    private boolean enableRequestTracing = true;

    /**
     * Enable metrics collection
     */
    private boolean enableMetrics = true;

    /**
     * Enable async tool execution
     */
    private boolean enableAsyncTools = false;

    // Getters and Setters with documentation

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public Duration getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(Duration executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public Duration getSchemaContextCacheTtl() {
        return schemaContextCacheTtl;
    }

    public void setSchemaContextCacheTtl(Duration schemaContextCacheTtl) {
        this.schemaContextCacheTtl = schemaContextCacheTtl;
    }

    public boolean isEnableRequestTracing() {
        return enableRequestTracing;
    }

    public void setEnableRequestTracing(boolean enableRequestTracing) {
        this.enableRequestTracing = enableRequestTracing;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public boolean isEnableAsyncTools() {
        return enableAsyncTools;
    }

    public void setEnableAsyncTools(boolean enableAsyncTools) {
        this.enableAsyncTools = enableAsyncTools;
    }
}


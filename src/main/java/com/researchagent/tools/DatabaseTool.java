package com.researchagent.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchagent.model.ToolResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseTool implements AgentTool {

    private final McpDatabaseClient mcpDatabaseClient;
    private final ObjectMapper objectMapper;
    private final int defaultLimit;
    private final String schemaName;
    private final int schemaContextTtlSeconds;
    private volatile String cachedSchemaContext;
    private volatile long cachedSchemaContextAtMillis;

    public DatabaseTool(
            McpDatabaseClient mcpDatabaseClient,
            ObjectMapper objectMapper,
            @Value("${agent.database.default-limit:100}") int defaultLimit,
            @Value("${agent.database.schema-name:public}") String schemaName,
            @Value("${agent.database.schema-context-ttl-seconds:60}") int schemaContextTtlSeconds) {
        this.mcpDatabaseClient = mcpDatabaseClient;
        this.objectMapper = objectMapper;
        this.defaultLimit = defaultLimit;
        this.schemaName = schemaName;
        this.schemaContextTtlSeconds = schemaContextTtlSeconds;
    }

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public String getDescription() {
        return "Execute exact read-only PostgreSQL queries through the MCP server. Input must include sql and may include limit.";
    }

    @Override
    public ToolResult execute(Map<String, Object> input) {
        try {
            return runSqlQuery(input);
        }
        catch (IllegalArgumentException ex) {
            return new ToolResult(getName(), false, ex.getMessage());
        }
        catch (IllegalStateException ex) {
            return new ToolResult(getName(), false, "Database MCP error: " + ex.getMessage());
        }
    }

    @Override
    public String getPromptContext() {
        long now = System.currentTimeMillis();
        long ttlMillis = Math.max(0, schemaContextTtlSeconds) * 1000L;
        if (cachedSchemaContext != null && (now - cachedSchemaContextAtMillis) <= ttlMillis) {
            return cachedSchemaContext;
        }

        try {
            Map<String, Object> health = mcpDatabaseClient.healthCheck();
            List<Map<String, Object>> tables = mapList(mcpDatabaseClient.listTables(schemaName).get("tables"));
            if (tables.isEmpty()) {
                cachedSchemaContext = "No table metadata found in schema '" + schemaName + "' from the MCP database server.";
                cachedSchemaContextAtMillis = now;
                return cachedSchemaContext;
            }

            List<String> tableSummaries = new ArrayList<>();
            for (Map<String, Object> table : tables.stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.get("name"))))
                    .toList()) {
                String tableName = String.valueOf(table.get("name"));
                Map<String, Object> details = mcpDatabaseClient.describeTable(schemaName, tableName);
                List<Map<String, Object>> columns = mapList(details.get("columns"));
                List<String> columnSummaries = columns.stream()
                        .map(column -> column.get("name") + " (" + column.get("typeName") + ")")
                        .toList();
                tableSummaries.add(tableName + ": " + String.join(", ", columnSummaries));
            }

            cachedSchemaContext = "Database " + health.getOrDefault("database", "reno_build")
                    + ", allowed schema '" + schemaName + "' -> "
                    + String.join(" | ", tableSummaries)
                    + ". For user lookups, the auth table is public.\"User\" with columns id, name, email, password. Do not select password unless the user explicitly asks for it.";
            cachedSchemaContextAtMillis = now;
            return cachedSchemaContext;
        }
        catch (RuntimeException ex) {
            return "Schema context unavailable from MCP database server: " + ex.getMessage();
        }
    }

    private ToolResult runSqlQuery(Map<String, Object> input) {
        String sql = stringValue(input, "sql", "");
        if (sql.isBlank()) {
            throw new IllegalArgumentException("Database tool input must include a non-blank 'sql' field.");
        }

        int limit = intValue(input, "limit", defaultLimit);
        if (limit <= 0) {
            throw new IllegalArgumentException("Input 'limit' must be greater than 0.");
        }

        Map<String, Object> response = mcpDatabaseClient.queryReadonly(sql, limit);
        return new ToolResult(getName(), true, formatJson(response));
    }

    private String formatJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String stringValue(Map<String, Object> input, String key, String fallback) {
        if (input == null) {
            return fallback;
        }
        Object value = input.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Map<String, Object> input, String key, int fallback) {
        if (input == null) {
            return fallback;
        }
        Object value = input.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> cast = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    cast.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(cast);
            }
        }
        return result;
    }
}

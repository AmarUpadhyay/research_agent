package com.researchagent.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchagent.model.ToolResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class DatabaseTool implements AgentTool {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String defaultTable;
    private final String defaultSearchColumn;
    private final int defaultLimit;
    private final String schemaName;
    private final int schemaContextTtlSeconds;
    private volatile String cachedSchemaContext;
    private volatile long cachedSchemaContextAtMillis;

    public DatabaseTool(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${agent.database.default-table:task_context}") String defaultTable,
            @Value("${agent.database.default-search-column:content}") String defaultSearchColumn,
            @Value("${agent.database.default-limit:5}") int defaultLimit,
            @Value("${agent.database.schema-name:public}") String schemaName,
            @Value("${agent.database.schema-context-ttl-seconds:60}") int schemaContextTtlSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.defaultTable = defaultTable;
        this.defaultSearchColumn = defaultSearchColumn;
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
        return "Execute read-only SQL queries. Inputs: sql + params (preferred) or table + criteria.";
    }

    @Override
    public ToolResult execute(Map<String, Object> input) {
        try {
            if (input != null && input.get("sql") != null) {
                return runSqlQuery(input);
            }
            return runLookupQuery(input);
        } catch (DataAccessException ex) {
            return new ToolResult(getName(), false, "Database error: " + errorMessage(ex));
        } catch (IllegalArgumentException ex) {
            return new ToolResult(getName(), false, ex.getMessage());
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
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                            SELECT table_name, column_name, data_type, ordinal_position
                            FROM information_schema.columns
                            WHERE table_schema = ?
                            ORDER BY table_name, ordinal_position
                            """,
                    schemaName
            );

            if (rows.isEmpty()) {
                cachedSchemaContext = "No table metadata found in schema '" + schemaName + "'.";
                cachedSchemaContextAtMillis = now;
                return cachedSchemaContext;
            }

            Map<String, List<String>> byTable = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String tableName = String.valueOf(row.get("table_name"));
                String columnName = String.valueOf(row.get("column_name"));
                String dataType = String.valueOf(row.get("data_type"));
                byTable.computeIfAbsent(tableName, key -> new ArrayList<>())
                        .add(columnName + " (" + dataType + ")");
            }

            List<String> tableSummaries = byTable.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                    .toList();

            cachedSchemaContext = "Schema '" + schemaName + "' tables -> " + String.join(" | ", tableSummaries);
            cachedSchemaContextAtMillis = now;
            return cachedSchemaContext;
        } catch (DataAccessException ex) {
            return "Schema context unavailable: " + errorMessage(ex);
        }
    }

    private ToolResult runSqlQuery(Map<String, Object> input) {
        String sql = stringValue(input, "sql", "");
        if (sql.isBlank()) {
            throw new IllegalArgumentException("Input 'sql' cannot be blank.");
        }

        String trimmedSql = sql.trim();
        if (!trimmedSql.toLowerCase().startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }

        List<Object> params = objectList(input.get("params"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(trimmedSql, params.toArray());
        return new ToolResult(getName(), true, formatRows(rows));
    }

    private ToolResult runLookupQuery(Map<String, Object> input) {
        String table = stringValue(input, "table", defaultTable);
        String criteria = stringValue(input, "criteria", "");
        String searchColumn = stringValue(input, "column", defaultSearchColumn);
        int limit = intValue(input, "limit", defaultLimit);

        validateIdentifier(table, "table");
        validateIdentifier(searchColumn, "column");
        if (limit <= 0) {
            throw new IllegalArgumentException("Input 'limit' must be greater than 0.");
        }

        String sql = "SELECT * FROM " + table + " WHERE " + searchColumn + " LIKE ? LIMIT ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, "%" + criteria + "%", limit);
        return new ToolResult(getName(), true, formatRows(rows));
    }

    private void validateIdentifier(String value, String fieldName) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + fieldName + " value: " + value);
        }
    }

    private String formatRows(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No rows found.";
        }
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException ex) {
            return rows.toString();
        }
    }

    private String errorMessage(DataAccessException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return ex.getMessage() == null ? "Unknown database error." : ex.getMessage();
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

    private List<Object> objectList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        throw new IllegalArgumentException("Input 'params' must be a list.");
    }
}

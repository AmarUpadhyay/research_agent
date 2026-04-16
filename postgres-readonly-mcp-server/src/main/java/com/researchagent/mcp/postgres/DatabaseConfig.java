package com.researchagent.mcp.postgres;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record DatabaseConfig(
        String jdbcUrl,
        String username,
        String password,
        int maxRows,
        int statementTimeoutSeconds,
        Set<String> allowedSchemas
) {

    private static final int DEFAULT_MAX_ROWS = 100;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final Set<String> DEFAULT_SCHEMAS = Set.of("public");
    private static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:5432/reno_build";
    private static final String DEFAULT_USERNAME = "postgres";
    private static final String DEFAULT_PASSWORD = "admin";

    public static DatabaseConfig fromEnvironment() {
        String jdbcUrl = optionalEnv("PG_MCP_DB_URL", "DB_URL");
        String username = optionalEnv("PG_MCP_DB_USERNAME", "DB_USERNAME");
        String password = optionalEnv("PG_MCP_DB_PASSWORD", "DB_PASSWORD");
        int maxRows = parseInt(optionalEnv("PG_MCP_MAX_ROWS"), DEFAULT_MAX_ROWS);
        int timeoutSeconds = parseInt(optionalEnv("PG_MCP_STATEMENT_TIMEOUT_SECONDS"), DEFAULT_TIMEOUT_SECONDS);
        Set<String> schemas = parseSchemas(optionalEnv("PG_MCP_ALLOWED_SCHEMAS"));
        return new DatabaseConfig(
                jdbcUrl == null || jdbcUrl.isBlank() ? DEFAULT_JDBC_URL : jdbcUrl,
                username == null || username.isBlank() ? DEFAULT_USERNAME : username,
                password == null ? DEFAULT_PASSWORD : password,
                maxRows,
                timeoutSeconds,
                schemas
        );
    }

    public DatabaseConfig {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl is required");
        Objects.requireNonNull(username, "username is required");
        password = password == null ? "" : password;
        maxRows = Math.max(1, maxRows);
        statementTimeoutSeconds = Math.max(1, statementTimeoutSeconds);
        allowedSchemas = allowedSchemas == null || allowedSchemas.isEmpty()
                ? DEFAULT_SCHEMAS
                : allowedSchemas.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String optionalEnv(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Set<String> parseSchemas(String rawSchemas) {
        if (rawSchemas == null || rawSchemas.isBlank()) {
            return DEFAULT_SCHEMAS;
        }
        return Arrays.stream(rawSchemas.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

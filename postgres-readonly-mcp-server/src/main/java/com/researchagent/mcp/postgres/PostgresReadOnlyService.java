package com.researchagent.mcp.postgres;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public final class PostgresReadOnlyService {

    private final DatabaseConfig config;
    private final ReadOnlySqlGuard sqlGuard;

    public PostgresReadOnlyService(DatabaseConfig config, ReadOnlySqlGuard sqlGuard) {
        this.config = Objects.requireNonNull(config, "config is required");
        this.sqlGuard = Objects.requireNonNull(sqlGuard, "sqlGuard is required");
    }

    public Map<String, Object> health() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select current_database() as database_name, current_user as db_user, version() as server_version")) {
            resultSet.next();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "ok");
            response.put("database", resultSet.getString("database_name"));
            response.put("user", resultSet.getString("db_user"));
            response.put("serverVersion", resultSet.getString("server_version"));
            response.put("allowedSchemas", config.allowedSchemas());
            response.put("checkedAt", OffsetDateTime.now().toString());
            return response;
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Database health check failed: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> listTables(String schema) {
        String schemaName = normalizeSchema(schema);
        try (Connection connection = openConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> tables = new ArrayList<>();
            try (ResultSet resultSet = metaData.getTables(null, schemaName, "%", new String[]{"TABLE", "VIEW", "MATERIALIZED VIEW", "FOREIGN TABLE", "PARTITIONED TABLE"})) {
                while (resultSet.next()) {
                    Map<String, Object> table = new LinkedHashMap<>();
                    table.put("schema", resultSet.getString("TABLE_SCHEM"));
                    table.put("name", resultSet.getString("TABLE_NAME"));
                    table.put("type", resultSet.getString("TABLE_TYPE"));
                    table.put("remarks", resultSet.getString("REMARKS"));
                    tables.add(table);
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("schema", schemaName);
            response.put("count", tables.size());
            response.put("tables", tables);
            return response;
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to list tables: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> describeTable(String schema, String table) {
        String schemaName = normalizeSchema(schema);
        String tableName = requireIdentifier(table, "table");

        try (Connection connection = openConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> columns = new ArrayList<>();
            try (ResultSet resultSet = metaData.getColumns(null, schemaName, tableName, "%")) {
                while (resultSet.next()) {
                    Map<String, Object> column = new LinkedHashMap<>();
                    column.put("name", resultSet.getString("COLUMN_NAME"));
                    column.put("jdbcType", resultSet.getInt("DATA_TYPE"));
                    column.put("typeName", resultSet.getString("TYPE_NAME"));
                    column.put("columnSize", resultSet.getInt("COLUMN_SIZE"));
                    column.put("nullable", resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    column.put("defaultValue", resultSet.getString("COLUMN_DEF"));
                    column.put("remarks", resultSet.getString("REMARKS"));
                    columns.add(column);
                }
            }

            if (columns.isEmpty()) {
                throw new IllegalArgumentException("Table not found in allowed schema: " + schemaName + "." + tableName);
            }

            Set<String> primaryKeys = new LinkedHashSet<>();
            try (ResultSet resultSet = metaData.getPrimaryKeys(null, schemaName, tableName)) {
                while (resultSet.next()) {
                    primaryKeys.add(resultSet.getString("COLUMN_NAME"));
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("schema", schemaName);
            response.put("table", tableName);
            response.put("primaryKeys", primaryKeys);
            response.put("columns", columns);
            return response;
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to describe table: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> executeQuery(String sql, Integer requestedLimit) {
        sqlGuard.validate(sql);
        int effectiveLimit = requestedLimit == null ? config.maxRows() : Math.max(1, Math.min(requestedLimit, config.maxRows()));

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setMaxRows(effectiveLimit);
            statement.setFetchSize(Math.min(effectiveLimit, config.maxRows()));
            statement.setQueryTimeout(config.statementTimeoutSeconds());

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<String> columns = new ArrayList<>();
                for (int index = 1; index <= metaData.getColumnCount(); index++) {
                    columns.add(metaData.getColumnLabel(index));
                }

                List<Map<String, Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= metaData.getColumnCount(); index++) {
                        row.put(metaData.getColumnLabel(index), readValue(resultSet, index, metaData.getColumnType(index)));
                    }
                    rows.add(row);
                }

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("rowCount", rows.size());
                response.put("maxRowsApplied", effectiveLimit);
                response.put("columns", columns);
                response.put("rows", rows);
                return response;
            }
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Read-only query failed: " + ex.getMessage(), ex);
        }
    }

    private Connection openConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", config.username());
        properties.setProperty("password", config.password());
        properties.setProperty("readOnly", "true");
        properties.setProperty("ApplicationName", "postgres-readonly-mcp-server");

        Connection connection = DriverManager.getConnection(config.jdbcUrl(), properties);
        connection.setReadOnly(true);
        connection.setAutoCommit(false);

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(config.statementTimeoutSeconds());
            statement.execute("SET TRANSACTION READ ONLY");
        }
        return connection;
    }

    private String normalizeSchema(String schema) {
        String normalized = requireIdentifier(schema == null || schema.isBlank() ? "public" : schema, "schema").toLowerCase(Locale.ROOT);
        if (!config.allowedSchemas().contains(normalized)) {
            throw new IllegalArgumentException("Schema is not allowed: " + normalized);
        }
        return normalized;
    }

    private String requireIdentifier(String rawValue, String label) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String value = rawValue.trim();
        if (!value.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(label + " must contain only letters, numbers, or underscores.");
        }
        return value;
    }

    private Object readValue(ResultSet resultSet, int index, int jdbcType) throws SQLException {
        Object value = switch (jdbcType) {
            case Types.ARRAY -> {
                Object array = resultSet.getObject(index);
                yield array == null ? null : array.toString();
            }
            case Types.TIMESTAMP_WITH_TIMEZONE -> {
                OffsetDateTime dateTime = resultSet.getObject(index, OffsetDateTime.class);
                yield dateTime == null ? null : dateTime.toString();
            }
            default -> resultSet.getObject(index);
        };
        return value;
    }
}

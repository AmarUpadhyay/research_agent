package com.researchagent.mcp.postgres;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public final class PostgresReadonlyMcpServer {

    private PostgresReadonlyMcpServer() {
    }

    public static void main(String[] args) throws InterruptedException {
        DatabaseConfig config = DatabaseConfig.fromEnvironment();
        PostgresReadOnlyService service = new PostgresReadOnlyService(config, new ReadOnlySqlGuard());

        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(JsonSupport.mcpJsonMapper());

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("postgres-readonly-mcp-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(
                        createHealthTool(service),
                        createListTablesTool(service),
                        createDescribeTableTool(service),
                        createQueryTool(service, config)
                )
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.err.println("postgres-readonly-mcp-server is ready on STDIO");
        Thread.currentThread().join();
    }

    private static McpServerFeatures.SyncToolSpecification createHealthTool(PostgresReadOnlyService service) {
        String schema = """
                {
                  "type": "object",
                  "properties": {},
                  "additionalProperties": false
                }
                """;
        return new McpServerFeatures.SyncToolSpecification(
                McpSchema.Tool.builder()
                        .name("postgres_health_check")
                        .description("Verify connectivity to PostgreSQL and return database metadata for this read-only MCP server.")
                        .inputSchema(JsonSupport.mcpJsonMapper(), schema)
                        .build(),
                (exchange, request) -> success(service.health())
        );
    }

    private static McpServerFeatures.SyncToolSpecification createListTablesTool(PostgresReadOnlyService service) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "schema": {
                      "type": "string",
                      "description": "Schema to inspect. Defaults to public."
                    }
                  },
                  "additionalProperties": false
                }
                """;
        return new McpServerFeatures.SyncToolSpecification(
                McpSchema.Tool.builder()
                        .name("list_tables")
                        .description("List tables and views from an allowed PostgreSQL schema.")
                        .inputSchema(JsonSupport.mcpJsonMapper(), schema)
                        .build(),
                (exchange, request) -> {
                    try {
                        return success(service.listTables(stringArg(request.arguments(), "schema")));
                    }
                    catch (RuntimeException ex) {
                        return error(ex.getMessage());
                    }
                }
        );
    }

    private static McpServerFeatures.SyncToolSpecification createDescribeTableTool(PostgresReadOnlyService service) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "schema": {
                      "type": "string",
                      "description": "Schema that contains the table. Defaults to public."
                    },
                    "table": {
                      "type": "string",
                      "description": "Table name to describe."
                    }
                  },
                  "required": ["table"],
                  "additionalProperties": false
                }
                """;
        return new McpServerFeatures.SyncToolSpecification(
                McpSchema.Tool.builder()
                        .name("describe_table")
                        .description("Return column metadata and primary keys for a PostgreSQL table in an allowed schema.")
                        .inputSchema(JsonSupport.mcpJsonMapper(), schema)
                        .build(),
                (exchange, request) -> {
                    try {
                        return success(service.describeTable(
                                stringArg(request.arguments(), "schema"),
                                requiredStringArg(request.arguments(), "table")
                        ));
                    }
                    catch (RuntimeException ex) {
                        return error(ex.getMessage());
                    }
                }
        );
    }

    private static McpServerFeatures.SyncToolSpecification createQueryTool(PostgresReadOnlyService service, DatabaseConfig config) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "sql": {
                      "type": "string",
                      "description": "A single read-only SQL statement. Only SELECT, WITH, VALUES, and SHOW are allowed."
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Optional row limit capped by the server configuration."
                    }
                  },
                  "required": ["sql"],
                  "additionalProperties": false
                }
                """;
        return new McpServerFeatures.SyncToolSpecification(
                McpSchema.Tool.builder()
                        .name("query_readonly")
                        .description("Execute a guarded read-only PostgreSQL query. The server rejects write statements, multi-statement SQL, session-changing commands, and privileged file-access helpers. Results are capped at " + config.maxRows() + " rows.")
                        .inputSchema(JsonSupport.mcpJsonMapper(), schema)
                        .build(),
                (exchange, request) -> {
                    try {
                        return success(service.executeQuery(
                                requiredStringArg(request.arguments(), "sql"),
                                integerArg(request.arguments(), "limit")
                        ));
                    }
                    catch (RuntimeException ex) {
                        return error(ex.getMessage());
                    }
                }
        );
    }

    private static String stringArg(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        return value == null ? null : value.toString();
    }

    private static String requiredStringArg(Map<String, Object> arguments, String key) {
        String value = stringArg(arguments, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required.");
        }
        return value;
    }

    private static Integer integerArg(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be an integer.");
        }
    }

    private static McpSchema.CallToolResult success(Object payload) {
        return McpSchema.CallToolResult.builder()
                .addContent(new McpSchema.TextContent(JsonSupport.toJson(payload)))
                .isError(false)
                .build();
    }

    private static McpSchema.CallToolResult error(String message) {
        return McpSchema.CallToolResult.builder()
                .addContent(new McpSchema.TextContent(message))
                .isError(true)
                .build();
    }
}

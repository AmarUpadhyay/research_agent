package com.researchagent.tools;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class McpDatabaseClient {

    private static final Logger log = LoggerFactory.getLogger(McpDatabaseClient.class);

    private final String serverCommand;
    private final List<String> serverArgs;
    private final Duration requestTimeout;
    private final Duration initializationTimeout;
    private final Map<String, String> serverEnv;
    private final JacksonMcpJsonMapper jsonMapper;

    private volatile McpSyncClient client;
    private volatile StdioClientTransport transport;

    public McpDatabaseClient(
            @Value("${agent.database.mcp.command:java}") String serverCommand,
            @Value("${agent.database.mcp.jar-path:postgres-readonly-mcp-server/target/postgres-readonly-mcp-server-1.0.0.jar}") String jarPath,
            @Value("${agent.database.mcp.request-timeout-seconds:20}") long requestTimeoutSeconds,
            @Value("${agent.database.mcp.initialization-timeout-seconds:20}") long initializationTimeoutSeconds,
            @Value("${agent.database.mcp.db-url:jdbc:postgresql://localhost:5432/reno_build}") String dbUrl,
            @Value("${agent.database.mcp.db-username:postgres}") String dbUsername,
            @Value("${agent.database.mcp.db-password:admin}") String dbPassword,
            @Value("${agent.database.mcp.allowed-schemas:public}") String allowedSchemas,
            @Value("${agent.database.mcp.max-rows:100}") int maxRows,
            @Value("${agent.database.mcp.statement-timeout-seconds:30}") int statementTimeoutSeconds) {
        this.serverCommand = serverCommand;
        this.serverArgs = List.of("-jar", resolveJarPath(jarPath).toString());
        this.requestTimeout = Duration.ofSeconds(Math.max(1, requestTimeoutSeconds));
        this.initializationTimeout = Duration.ofSeconds(Math.max(1, initializationTimeoutSeconds));
        this.serverEnv = new LinkedHashMap<>();
        this.serverEnv.put("PG_MCP_DB_URL", dbUrl);
        this.serverEnv.put("PG_MCP_DB_USERNAME", dbUsername);
        this.serverEnv.put("PG_MCP_DB_PASSWORD", dbPassword);
        this.serverEnv.put("PG_MCP_ALLOWED_SCHEMAS", allowedSchemas);
        this.serverEnv.put("PG_MCP_MAX_ROWS", String.valueOf(Math.max(1, maxRows)));
        this.serverEnv.put("PG_MCP_STATEMENT_TIMEOUT_SECONDS", String.valueOf(Math.max(1, statementTimeoutSeconds)));
        this.jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
    }

    public synchronized Map<String, Object> healthCheck() {
        return callTool("postgres_health_check", Map.of());
    }

    public synchronized Map<String, Object> listTables(String schema) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        if (schema != null && !schema.isBlank()) {
            arguments.put("schema", schema);
        }
        return callTool("list_tables", arguments);
    }

    public synchronized Map<String, Object> describeTable(String schema, String table) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        if (schema != null && !schema.isBlank()) {
            arguments.put("schema", schema);
        }
        arguments.put("table", table);
        return callTool("describe_table", arguments);
    }

    public synchronized Map<String, Object> queryReadonly(String sql, Integer limit) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("sql", sql);
        if (limit != null) {
            arguments.put("limit", limit);
        }
        return callTool("query_readonly", arguments);
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (client != null) {
            try {
                client.closeGracefully();
            }
            catch (Exception ex) {
                log.debug("Error while closing MCP client gracefully", ex);
            }
            client.close();
            client = null;
        }
        if (transport != null) {
            try {
                transport.closeGracefully().block(Duration.ofSeconds(2));
            }
            catch (Exception ex) {
                log.debug("Error while closing MCP transport", ex);
            }
            transport = null;
        }
    }

    private Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = getClient().callTool(new McpSchema.CallToolRequest(toolName, arguments));
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException(extractText(result));
        }

        Object structuredContent = result.structuredContent();
        if (structuredContent instanceof Map<?, ?> map) {
            return castMap(map);
        }

        String text = extractText(result);
        if (text == null || text.isBlank()) {
            return Map.of();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = jsonMapper.readValue(text, Map.class);
            return parsed;
        }
        catch (IOException ex) {
            return Map.of("text", text);
        }
    }

    private McpSyncClient getClient() {
        McpSyncClient existing = client;
        if (existing != null && existing.isInitialized()) {
            return existing;
        }

        synchronized (this) {
            existing = client;
            if (existing != null && existing.isInitialized()) {
                return existing;
            }

            shutdown();

            ServerParameters serverParameters = ServerParameters.builder(serverCommand)
                    .args(serverArgs)
                    .env(serverEnv)
                    .build();

            StdioClientTransport newTransport = new StdioClientTransport(serverParameters, jsonMapper);
            newTransport.setStdErrorHandler(line -> log.debug("postgres-readonly-mcp-server stderr: {}", line));

            McpSyncClient newClient = McpClient.sync(newTransport)
                    .clientInfo(new McpSchema.Implementation("research-agent", "Research Agent", "1.0.0"))
                    .requestTimeout(requestTimeout)
                    .initializationTimeout(initializationTimeout)
                    .build();

            newClient.initialize();
            this.transport = newTransport;
            this.client = newClient;
            return newClient;
        }
    }

    private String extractText(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .filter(Objects::nonNull)
                .reduce((first, second) -> first + "\n" + second)
                .orElse("");
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return target;
    }

    private Path resolveJarPath(String jarPath) {
        Path path = Path.of(jarPath);
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path).normalize();
        }
        if (!Files.exists(path)) {
            throw new IllegalStateException("PostgreSQL MCP server jar not found at " + path + ". Build it with `mvn package` in postgres-readonly-mcp-server first.");
        }
        return path;
    }
}

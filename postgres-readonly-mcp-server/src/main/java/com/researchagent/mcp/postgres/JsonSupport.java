package com.researchagent.mcp.postgres;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class JsonSupport {

    private static final JsonMapper PROTOCOL_JSON_MAPPER = JsonMapper.builder()
            .build();

    private static final JsonMapper PAYLOAD_JSON_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private static final McpJsonMapper MCP_JSON_MAPPER = new JacksonMcpJsonMapper(PROTOCOL_JSON_MAPPER);

    private JsonSupport() {
    }

    public static McpJsonMapper mcpJsonMapper() {
        return MCP_JSON_MAPPER;
    }

    public static String toJson(Object value) {
        return PAYLOAD_JSON_MAPPER.writeValueAsString(value);
    }
}

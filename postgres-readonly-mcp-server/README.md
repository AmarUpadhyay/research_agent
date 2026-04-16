# PostgreSQL Read-Only MCP Server

Standalone Java MCP server for read-only PostgreSQL access over STDIO.

## What It Exposes

- `postgres_health_check`
  Checks database connectivity and returns basic database metadata.
- `list_tables`
  Lists tables and views in an allowed schema.
- `describe_table`
  Returns column metadata and primary keys for a table.
- `query_readonly`
  Executes a single guarded read-only SQL statement with a server-side row cap.

## Read-Only Protections

This server does more than set JDBC read-only mode:

- Opens the connection with JDBC `readOnly=true`
- Calls `connection.setReadOnly(true)`
- Starts a transaction and runs `SET TRANSACTION READ ONLY`
- Rejects semicolons to block multi-statement execution
- Allows only statements starting with `SELECT`, `WITH`, `VALUES`, or `SHOW`
- Rejects write/session-changing keywords such as `INSERT`, `UPDATE`, `DELETE`, `ALTER`, `DROP`, `COPY`, `SET`, and others
- Rejects a small blocklist of privileged helper functions such as `pg_read_file`
- Caps result size with `setMaxRows`

## Environment Variables

Required:

- None. By default the server targets `jdbc:postgresql://localhost:5432/reno_build`
  with username `postgres`, password `admin`, and schema `public`.

Optional:

- `PG_MCP_DB_PASSWORD`
  Fallback: `DB_PASSWORD`
- `PG_MCP_MAX_ROWS`
  Default: `100`
- `PG_MCP_STATEMENT_TIMEOUT_SECONDS`
  Default: `30`
- `PG_MCP_ALLOWED_SCHEMAS`
  Comma-separated list, default: `public`

## Build

```bash
cd /Users/amarupadhyaya/code/projects/research_agent/postgres-readonly-mcp-server
mvn package
```

The shaded jar is created in `target/postgres-readonly-mcp-server-1.0.0.jar`.

## Run

```bash
export PG_MCP_DB_URL="jdbc:postgresql://localhost:5432/reno_build"
export PG_MCP_DB_USERNAME="postgres"
export PG_MCP_DB_PASSWORD="admin"
export PG_MCP_ALLOWED_SCHEMAS="public"

java -jar target/postgres-readonly-mcp-server-1.0.0.jar
```

## Example MCP Client Config

For Codex or other STDIO-based MCP clients:

```json
{
  "mcpServers": {
    "postgres-readonly": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/amarupadhyaya/code/projects/research_agent/postgres-readonly-mcp-server/target/postgres-readonly-mcp-server-1.0.0.jar"
      ],
      "env": {
        "PG_MCP_DB_URL": "jdbc:postgresql://localhost:5432/reno_build",
        "PG_MCP_DB_USERNAME": "postgres",
        "PG_MCP_DB_PASSWORD": "admin",
        "PG_MCP_ALLOWED_SCHEMAS": "public",
        "PG_MCP_MAX_ROWS": "100",
        "PG_MCP_STATEMENT_TIMEOUT_SECONDS": "30"
      }
    }
  }
}
```

## Notes

- Keep the database user itself read-only as a second line of defense.
- `query_readonly` currently accepts a single SQL string rather than parameter arrays.
- If you need HTTP transport later, this project can be extended using the MCP Java SDK server transports.

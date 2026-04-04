# AI Agent Application - Quick Reference Guide

**Last Updated**: April 3, 2026

---

## 🚀 Quick Start Commands

### Health Check
```bash
curl http://localhost:8080/api/agent/health
```

### Simple Task
```bash
curl -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{"task":"What is 2+2?"}'
```

### Complex Task (Multi-Step)
```bash
curl -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "task":"Find all pending tasks in the database and log the count"
  }'
```

### Retrieve Task Result
```bash
curl http://localhost:8080/api/agent/tasks/task-{TIMESTAMP}
```

### Get Execution Steps
```bash
curl http://localhost:8080/api/agent/tasks/task-{TIMESTAMP}/steps
```

---

## 📋 All REST Endpoints

### 1. Health Status
```
GET /api/agent/health
```
**Returns**: `{"status": "up"}`  
**Use**: Service availability check

---

### 2. Execute Task
```
POST /api/agent/tasks
```
**Body**:
```json
{
  "task": "Your goal here"
}
```
**Returns**: Full `AgentTask` with execution history

**Status Codes**:
- `200 OK` - Execution completed (check task.status)
- `400 Bad Request` - Invalid request
- `500 Server Error` - Unexpected error

---

### 3. Get Task Details
```
GET /api/agent/tasks/{taskId}
```
**Parameters**: 
- `taskId` - From execute response (e.g., "task-1234567890")

**Returns**: 
```json
{
  "id": "task-1234567890",
  "goal": "...",
  "status": "RUNNING|COMPLETED|FAILED",
  "maxSteps": 5,
  "steps": [...],
  "finalResponse": "...",
  "createdAt": "2026-04-03T10:30:00Z"
}
```

---

### 4. Get Task Steps
```
GET /api/agent/tasks/{taskId}/steps
```
**Returns**: Array of `AgentStep` objects

**Step Object**:
```json
{
  "stepNumber": 1,
  "type": "PLAN|ACTION|OBSERVATION|FINAL|ERROR",
  "content": "Step details",
  "toolResult": {
    "toolName": "database",
    "success": true,
    "output": "..."
  }
}
```

---

## 🛠️ Available Tools Reference

### Tool 1: Database

**Name**: `database`

**Simple Lookup**:
```json
{
  "table": "task_context",
  "column": "content",
  "criteria": "important",
  "limit": 5
}
```

**SQL Query**:
```json
{
  "sql": "SELECT * FROM users WHERE status = ?",
  "params": ["active"]
}
```

**What It Does**: Reads data from PostgreSQL  
**Supported Operations**: SELECT only (read-only)

---

### Tool 2: Email

**Name**: `email`

**Usage**:
```json
{
  "recipient": "admin@example.com",
  "subject": "Task Complete",
  "body": "Notification message"
}
```

**What It Does**: Queues email notification  
**Status**: Currently mock (doesn't send)

---

### Tool 3: Logging

**Name**: `logging`

**Usage**:
```json
{
  "message": "Event to log"
}
```

**What It Does**: Writes audit log  
**Best For**: Compliance, debugging, tracking

---

## 📊 Task Status Values

| Status | Meaning | Next Action |
|--------|---------|------------|
| `RUNNING` | Currently executing | Wait for completion or retry |
| `COMPLETED` | Goal achieved | Check `finalResponse` for result |
| `FAILED` | Stopped with error | Check steps for `ERROR` type to see why |

---

## 📈 Step Type Progression

```
Every task follows this pattern:

PLAN → ACTION → OBSERVATION → PLAN → ACTION → OBSERVATION → ... → FINAL

Or stops with:

PLAN → ACTION → OBSERVATION → ERROR (something went wrong)
```

### Step Types Explained

| Type | Meaning | Contains |
|------|---------|----------|
| `PLAN` | Agent's reasoning | Summary of next action |
| `ACTION` | Tool invocation | Tool name + input params |
| `OBSERVATION` | Tool result | Output + success flag |
| `FINAL` | Task complete | Final answer |
| `ERROR` | Something failed | Error message |

---

## 💡 Example Workflows

### Workflow 1: Simple Query
```
Goal: "Show me all active users"

Step 1 (PLAN): "I need to query the users table for active status"
Step 1 (ACTION): Tool=database, Input={table:users, criteria:active}
Step 1 (OBSERVATION): Result=[{id:1,name:John}, {id:2,name:Jane}]
Step 2 (FINAL): "I found 2 active users: John and Jane"
```

### Workflow 2: Query + Notification
```
Goal: "Find pending orders and notify fulfillment team"

Step 1 (PLAN): "First, I'll find pending orders"
Step 1 (ACTION): Tool=database, Input={table:orders, criteria:pending}
Step 1 (OBSERVATION): Result=[{id:101, amount:$500}, {id:102, amount:$300}]

Step 2 (PLAN): "Now I'll send email notification"
Step 2 (ACTION): Tool=email, Input={recipient:fulfill@co.com, body:"2 pending orders found"}
Step 2 (OBSERVATION): Result="Email queued"

Step 3 (FINAL): "Found 2 pending orders and notified fulfillment team"
```

### Workflow 3: Query + Audit Log
```
Goal: "Find high-value customers and log the search"

Step 1 (PLAN): "I'll query for customers with high spend"
Step 1 (ACTION): Tool=database, Input={table:customers, criteria:premium}
Step 1 (OBSERVATION): Result=[{id:1, spend:$10000}, ...]

Step 2 (PLAN): "I'll log this search for audit"
Step 2 (ACTION): Tool=logging, Input={message:"Premium customer search executed"}
Step 2 (OBSERVATION): Result="Logged"

Step 3 (FINAL): "Found 5 premium customers and logged the search"
```

---

## 🔍 How the Agent Makes Decisions

The LLM receives this prompt (simplified):
```
Goal: {your goal}
Next step: 1 of 5
Available tools:
- database: Query PostgreSQL
- email: Send notification
- logging: Write audit log

Tool context:
- database: Schema public has tables: users, orders, customers
- email: (no context)
- logging: (no context)

Previous steps:
- none

Return the next decision only.
```

The agent responds with JSON:
```json
{
  "decisionType": "TOOL",
  "summary": "I need to find pending records",
  "toolName": "database",
  "toolInput": {
    "table": "orders",
    "criteria": "pending"
  }
}
```

Or when done:
```json
{
  "decisionType": "FINAL",
  "summary": "I have the answer",
  "finalResponse": "I found 3 pending orders"
}
```

---

## ⚠️ Error Cases

### Invalid JSON from Agent
```json
{
  "status": "FAILED",
  "steps": [
    {
      "type": "ERROR",
      "content": "Agent returned invalid JSON: {bad json here}"
    }
  ]
}
```

### Unknown Tool Requested
```json
{
  "status": "FAILED",
  "steps": [
    {
      "type": "ERROR",
      "content": "Unknown tool requested: unknown-tool"
    }
  ]
}
```

### Repeated Tool Call (Stuck Loop)
```json
{
  "status": "FAILED",
  "steps": [
    {
      "type": "ERROR",
      "content": "Repeated tool call blocked for tool 'database'"
    }
  ]
}
```

### Max Steps Exceeded
```json
{
  "status": "FAILED",
  "steps": [
    {
      "type": "ERROR",
      "content": "Execution stopped after reaching the maximum number of steps"
    }
  ]
}
```

---

## 🔧 Configuration Quick Reference

### Database Settings
```yaml
agent.database.default-table: task_context
agent.database.default-search-column: content
agent.database.default-limit: 5
agent.database.schema-name: public
agent.database.schema-context-ttl-seconds: 60
```

### LLM Settings
```yaml
ai.ollama.base-url: http://localhost:11434
ai.ollama.model: gemma:2b
```

### Agent Settings
```yaml
agent.executor.max-steps: 5  # Add this if you want to increase
```

---

## 📱 Example cURL Commands

### Submit Task and Capture ID
```bash
RESPONSE=$(curl -s -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{"task":"Find active users"}')

TASK_ID=$(echo $RESPONSE | jq -r '.id')
echo "Task ID: $TASK_ID"
```

### Poll Until Complete
```bash
TASK_ID="task-1234567890"
while true; do
  STATUS=$(curl -s http://localhost:8080/api/agent/tasks/$TASK_ID | jq -r '.status')
  if [ "$STATUS" != "RUNNING" ]; then
    echo "Task completed with status: $STATUS"
    break
  fi
  echo "Still running..."
  sleep 2
done
```

### Pretty Print Results
```bash
curl -s http://localhost:8080/api/agent/tasks/task-1234567890 | jq '.finalResponse'
curl -s http://localhost:8080/api/agent/tasks/task-1234567890/steps | jq '.[] | {type, content}'
```

---

## 🐛 Debugging Tips

### 1. Check Application Logs
```bash
tail -f application.log | grep -i error
```

### 2. View Full Task Details
```bash
curl http://localhost:8080/api/agent/tasks/{taskId} | jq .
```

### 3. Examine Each Step
```bash
curl http://localhost:8080/api/agent/tasks/{taskId}/steps | jq '.[]'
```

### 4. Enable Debug Logging
Edit `application.yml`:
```yaml
logging:
  level:
    com.researchagent: DEBUG
    dev.langchain4j: DEBUG
```

### 5. Check Ollama Connection
```bash
curl http://localhost:11434/api/tags
```

### 6. Test Database Connection
```bash
psql -h localhost -U postgres -d reno_build -c "SELECT 1;"
```

---

## 🎯 Common Task Examples

### Query Examples
```json
{"task": "Show me all records"}
{"task": "Find items containing 'important'"}
{"task": "How many tasks are in the database?"}
{"task": "What are the most recent 3 records?"}
```

### Multi-Step Examples
```json
{"task": "Find pending items and send a notification"}
{"task": "Query the database and log the results"}
{"task": "Get user count, email the team, and log completion"}
```

### Conversational Examples
```json
{"task": "Hello, what can you do?"}
{"task": "Can you help me find something?"}
{"task": "What tools do you have available?"}
```

---

## 📞 Quick Help

**Application won't start?**
- Check PostgreSQL is running
- Check Ollama is running on port 11434
- Check ports 5432 and 8080 are available

**Tasks always fail?**
- Check logs for error messages
- Verify database credentials
- Test Ollama with `curl http://localhost:11434/api/tags`

**Agent returns wrong results?**
- Check the exact prompt it's using
- Verify database table/column names match
- Try with a different LLM model

**Performance is slow?**
- Ollama inference takes time (5-30s depending on model)
- Use smaller models for faster responses
- Pre-warm Ollama before load testing

---

**For detailed information, see**:
- `README.md` - Complete documentation
- `ARCHITECTURE.md` - Deep technical details
- Source code comments - Implementation details


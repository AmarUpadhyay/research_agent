# AI Agent Application - Architecture & Design Document

**Document Version**: 1.0  
**Last Updated**: April 3, 2026  
**Application**: Autonomous AI Agent Platform

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Component Deep Dive](#component-deep-dive)
3. [Agentic Loop Workflow](#agentic-loop-workflow)
4. [Tool Ecosystem](#tool-ecosystem)
5. [API Reference](#api-reference)
6. [Data Models](#data-models)
7. [Error Handling](#error-handling)
8. [Extension Points](#extension-points)

---

## System Overview

### What is an Autonomous AI Agent?

An autonomous AI agent is a software system that:
1. **Perceives** user goals and environmental context
2. **Reasons** about the best sequence of actions
3. **Acts** by invoking external tools or services
4. **Observes** the outcomes of those actions
5. **Iterates** until the goal is satisfied or an error occurs

This application implements this pattern using:
- **LLM (Large Language Model)**: Decision engine via Ollama
- **Tools**: Database, Email, and Logging integrations
- **Executor**: Orchestrates the agentic loop with safety constraints
- **Storage**: In-memory state management for task tracking

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Runtime | Java | 25+ |
| Framework | Spring Boot | 3.5.6 |
| LLM Framework | LangChain4j | 0.30.0 |
| Local LLM | Ollama | Latest |
| Database | PostgreSQL | 12+ |
| Build Tool | Maven | 3.8+ |

---

## Component Deep Dive

### 1. AgentController (REST API Layer)

**File**: `src/main/java/com/researchagent/controller/AgentController.java`

**Responsibility**: HTTP request handling and response serialization

**Endpoints**:
- `GET /api/agent/health` - Service availability check
- `POST /api/agent/tasks` - Submit a task for execution
- `GET /api/agent/tasks/{taskId}` - Retrieve task details
- `GET /api/agent/tasks/{taskId}/steps` - Get execution steps

**Key Implementation Details**:
```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;
    
    @PostMapping("/tasks")
    public ResponseEntity<AgentTask> executeTask(@RequestBody TaskRequest request) {
        return ResponseEntity.ok(agentService.execute(request.getTask()));
    }
}
```

**Request/Response Models**:
- **Input**: `TaskRequest` with `task` field
- **Output**: `AgentTask` with full execution history

---

### 2. AgentService (Business Logic Layer)

**File**: `src/main/java/com/researchagent/service/AgentService.java`

**Responsibility**: Task lifecycle management and orchestration

**Key Methods**:
- `execute(String goal)`: Entry point for task execution
- `getTask(String taskId)`: Retrieve stored task
- `getTaskSteps(String taskId)`: Get step details

**Implementation Pattern**:
```java
public AgentTask execute(String goal) {
    return agentExecutor.run(goal);  // Delegates to executor
}
```

---

### 3. AgentExecutor (Core Agentic Loop)

**File**: `src/main/java/com/researchagent/agent/AgentExecutor.java`

**Responsibility**: Implements the agentic loop with safety constraints

**Agentic Loop Algorithm**:

```
1. Create task in RUNNING state
2. FOR each step (1 to maxSteps):
   a. Build prompt from task context and available tools
   b. Call LLM (TaskAgent) with prompt
   c. Parse JSON decision from LLM
   d. IF decision is FINAL:
      - Save task as COMPLETED
      - Return task
   e. IF decision is TOOL:
      - Validate tool exists
      - Check for repeated tool calls
      - Execute tool
      - Observe result
      - Add step to task history
      - Continue loop
3. IF loop completes without FINAL:
   - Set task as FAILED
   - Return task
```

**Safety Constraints**:

| Constraint | Purpose | Implementation |
|-----------|---------|-----------------|
| Max Steps (5) | Prevent infinite loops | Check `stepNumber <= maxSteps` |
| No Repeated Tools | Detect stuck loops | Compare current vs previous tool call |
| Tool Validation | Prevent unknown tools | Lookup in `toolsByName` registry |
| JSON Validation | Ensure structured output | Jackson JSON parsing with error handling |
| Decision Type Check | Only TOOL or FINAL | Reject unsupported decision types |

**Code Snippet**:
```java
public AgentTask run(String goal) {
    AgentTask task = inMemoryStore.createTask(goal, DEFAULT_MAX_STEPS);
    
    for (int stepNumber = 1; stepNumber <= task.getMaxSteps(); stepNumber++) {
        String prompt = buildPrompt(task, stepNumber);
        String rawDecision = taskAgent.decideNextAction(prompt);
        AgentDecision decision = objectMapper.readValue(rawDecision, AgentDecision.class);
        
        if ("FINAL".equalsIgnoreCase(decision.getDecisionType())) {
            task.setStatus(AgentTaskStatus.COMPLETED);
            return task;
        }
        
        AgentTool tool = toolsByName.get(decision.getToolName());
        ToolResult result = tool.execute(decision.getToolInput());
        // Add to task steps...
    }
}
```

---

### 4. TaskAgent (LLM Interface)

**File**: `src/main/java/com/researchagent/agent/TaskAgent.java`

**Responsibility**: LLM decision interface with structured output

**Interface Definition**:
```java
public interface TaskAgent {
    @SystemMessage("""
        You are the decision engine for an autonomous AI agent.
        You must always return valid JSON and nothing else.
        
        Supported response schema: {...}
        Rules: {...}
    """)
    String decideNextAction(String executorInput);
}
```

**System Message Instructions to LLM**:
1. Always respond with valid JSON
2. Choose decision type: TOOL or FINAL
3. One tool per step maximum
4. Don't invent tool names
5. Don't repeat successful tool calls
6. When previous observations are sufficient, use FINAL

**How It Works**:
- Implemented via LangChain4j with Ollama backend
- Spring creates a proxy that calls the LLM
- Prompt includes task goal, available tools, and step history
- Returns raw JSON string for parsing

---

### 5. Tool Registry & Tools

**Base Interface**: `src/main/java/com/researchagent/tools/AgentTool.java`

```java
public interface AgentTool {
    String getName();
    String getDescription();
    ToolResult execute(Map<String, Object> input);
    default String getPromptContext() { return ""; }
}
```

#### Tool 1: Database Tool

**Class**: `DatabaseTool.java`  
**Name**: `database`

**Purpose**: Execute read-only SQL queries or simple table lookups

**Features**:
- ✅ Full SQL support with parameterized queries
- ✅ Simple table lookup mode for basic searches
- ✅ SQL injection prevention
- ✅ Schema caching (60s TTL)
- ✅ Regex validation for table/column names

**Input Modes**:

**Mode 1 - SQL Query**:
```json
{
  "sql": "SELECT * FROM users WHERE status = ?",
  "params": ["active"]
}
```

**Mode 2 - Table Lookup**:
```json
{
  "table": "task_context",
  "column": "content",
  "criteria": "important",
  "limit": 10
}
```

**Output**:
```json
{
  "toolName": "database",
  "success": true,
  "output": "[{\"id\":1,\"status\":\"active\"}]"
}
```

**Configuration**:
```yaml
agent:
  database:
    default-table: task_context
    default-search-column: content
    default-limit: 5
    schema-name: public
    schema-context-ttl-seconds: 60
```

**Security Model**:
- SELECT-only enforcement (regex check)
- Parameterized queries prevent injection
- Identifier validation (table/column names)
- No direct SQL in prompts (schema context provided instead)

#### Tool 2: Email Tool

**Class**: `EmailTool.java`  
**Name**: `email`

**Purpose**: Queue notification emails with custom recipients/subject/body

**Input Schema**:
```json
{
  "recipient": "admin@example.com",
  "subject": "Task Complete",
  "body": "The requested task has finished."
}
```

**Parameters**:
- `recipient` (optional, default: "ops@research-agent.local")
- `subject` (optional, default: "Agent notification")
- `body` (optional, default: "")

**Output**:
```json
{
  "toolName": "email",
  "success": true,
  "output": "Email queued for admin@example.com..."
}
```

**Current Implementation**: Mock queue (prints to logs)  
**Production Path**: Integrate SMTP service

#### Tool 3: Logging Tool

**Class**: `LoggingTool.java`  
**Name**: `logging`

**Purpose**: Write structured audit logs

**Input Schema**:
```json
{
  "message": "User query returned 15 results"
}
```

**Output**:
```json
{
  "toolName": "logging",
  "success": true,
  "output": "Logged message: User query returned 15 results"
}
```

**Best Practices**:
- Use for audit trails and compliance logging
- Not for conversational replies (use FINAL instead)
- Should be sanitized of sensitive data

---

### 6. InMemoryStore (State Management)

**File**: `src/main/java/com/researchagent/memory/InMemoryStore.java`

**Responsibility**: Ephemeral task and step storage

**Key Methods**:
```java
AgentTask createTask(String goal, int maxSteps)
void saveTask(AgentTask task)
AgentTask getTask(String taskId)
List<AgentStep> getTaskSteps(String taskId)
```

**Implementation Details**:
- Uses `ConcurrentHashMap` for thread-safe storage
- Task ID format: `task-{timestamp}`
- No database persistence (suitable for stateless deployments)
- **Note**: Data is lost on application restart

**For Production**:
- Replace with database-backed implementation
- Use Spring Data JPA or custom JDBC repository
- Implement cleanup jobs for old tasks

---

## Agentic Loop Workflow

### Example: Database Query Task

**Goal**: "Find all users with status=pending and email them a notification"

```
┌─────────────────────────────────────────────────────────────┐
│ STEP 1: PLAN                                                 │
│ Agent decides: "I need to query the database first"          │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ STEP 1: ACTION                                               │
│ Tool: database                                               │
│ Input: {table: "users", criteria: "pending"}               │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ STEP 1: OBSERVATION                                          │
│ Result: [{id: 1, email: "john@ex.com"}, {id: 2, ...}]     │
│ Status: SUCCESS                                              │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ STEP 2: PLAN                                                 │
│ Agent decides: "Now I'll send emails to these users"        │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ STEP 2: ACTION                                               │
│ Tool: email                                                  │
│ Input: {recipient: "john@ex.com", body: "Action needed"} │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ STEP 2: OBSERVATION                                          │
│ Result: "Email queued for john@ex.com"                      │
│ Status: SUCCESS                                              │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ STEP 3: FINAL                                                │
│ Response: "I found 2 pending users and sent emails to both" │
│ Status: COMPLETED                                            │
└─────────────────────────────────────────────────────────────┘
```

---

## Tool Ecosystem

### Tool Registry Mechanism

Tools are automatically discovered and registered via Spring Component scanning:

```java
@Component
public class DatabaseTool implements AgentTool { ... }

@Component
public class EmailTool implements AgentTool { ... }

@Component
public class LoggingTool implements AgentTool { ... }
```

**In AgentExecutor**:
```java
public AgentExecutor(List<AgentTool> tools) {
    this.toolsByName = tools.stream()
        .collect(Collectors.toUnmodifiableMap(
            AgentTool::getName, 
            tool -> tool
        ));
}
```

### Adding a Custom Tool

**Step 1**: Create new component
```java
@Component
public class CustomTool implements AgentTool {
    
    @Override
    public String getName() {
        return "custom-tool";
    }
    
    @Override
    public String getDescription() {
        return "Does something custom";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> input) {
        // Implementation
        return new ToolResult("custom-tool", true, "Success message");
    }
    
    @Override
    public String getPromptContext() {
        return "Additional info for the LLM";
    }
}
```

**Step 2**: Update TaskAgent system message
```java
// Add "custom-tool" to the instructions
@SystemMessage("""
    ...
    Available tools: database, email, logging, custom-tool
    ...
""")
```

**Step 3**: Deploy and use
- The executor will automatically discover and register the tool
- The LLM can now invoke it by name

---

## API Reference

### Request/Response Flow

```
Client Request
    │
    ▼
AgentController (REST layer)
    │
    ▼
AgentService (business logic)
    │
    ▼
AgentExecutor (agentic loop)
    │
    ├─→ TaskAgent (LLM) → Ollama
    │
    └─→ AgentTools (database, email, logging)
    │
    ▼
InMemoryStore (persist task state)
    │
    ▼
AgentController (serialize response)
    │
    ▼
Client Response
```

### Error Handling in API

**Invalid JSON from LLM**:
- Status: 200 OK (HTTP level)
- Task Status: FAILED
- Step Type: ERROR
- Message: "Agent returned invalid JSON: ..."

**Unknown Tool Requested**:
- Status: 200 OK
- Task Status: FAILED
- Message: "Unknown tool requested: ..."

**Max Steps Exceeded**:
- Status: 200 OK
- Task Status: FAILED
- Message: "Execution stopped after reaching maximum steps"

**Repeated Tool Call**:
- Status: 200 OK
- Task Status: FAILED
- Message: "Repeated tool call blocked for tool '...'"

---

## Data Models

### AgentTask
```json
{
  "id": "task-1234567890",
  "goal": "User's objective",
  "status": "RUNNING|COMPLETED|FAILED",
  "maxSteps": 5,
  "steps": [
    {
      "stepNumber": 1,
      "type": "PLAN|ACTION|OBSERVATION|FINAL|ERROR",
      "content": "Step description",
      "toolResult": {
        "toolName": "database",
        "success": true,
        "output": "..."
      }
    }
  ],
  "finalResponse": "Agent's final answer",
  "createdAt": "2026-04-03T10:30:00Z"
}
```

### AgentDecision (LLM Output)
```json
{
  "decisionType": "TOOL|FINAL",
  "summary": "Why this decision",
  "toolName": "database|email|logging",
  "toolInput": {
    "key": "value"
  },
  "finalResponse": "Answer when FINAL"
}
```

### ToolResult
```json
{
  "toolName": "database",
  "success": true|false,
  "output": "Tool output or error message"
}
```

---

## Error Handling

### Execution Error Scenarios

| Scenario | Detection | Handling | Recovery |
|----------|-----------|----------|----------|
| Invalid JSON | JSON parsing exception | Set ERROR step, mark FAILED | None (task ends) |
| Unknown tool | Lookup returns null | Set ERROR step, mark FAILED | None (task ends) |
| Repeated tool | Compare previous ACTION | Set ERROR step, mark FAILED | None (task ends) |
| DB error | SQLException | Catch, return error result | Tool can retry in next step |
| Max steps | Loop counter reaches limit | Set ERROR step, mark FAILED | Increase DEFAULT_MAX_STEPS |

### Logging & Debugging

**Application Logs** (`application.yml`):
```yaml
logging:
  level:
    com.researchagent: DEBUG
    dev.langchain4j: INFO
```

**Task Steps as Audit Trail**:
- Each step is recorded with type, content, and tool result
- Complete history available via `/tasks/{taskId}/steps`
- Useful for debugging agent behavior

---

## Extension Points

### 1. Replace InMemoryStore

**Current**: `InMemoryStore` (ephemeral, HashMap-based)

**To Persist to Database**:
```java
@Component
public class DatabaseTaskStore implements TaskStore {
    // Implement using Spring Data JPA or JDBC
}
```

### 2. Add New Tool

See "Tool Ecosystem" section above.

### 3. Replace Ollama with Different LLM

**Current**: Ollama via LangChain4j

**Alternatives**:
- OpenAI GPT models
- Anthropic Claude
- Google PaLM
- Local models via Hugging Face

**Change**: Update `application.yml` and Maven dependencies

### 4. Custom Decision Validation

Add validation in `AgentExecutor.run()`:
```java
private void validateDecision(AgentDecision decision) {
    // Custom validation logic
}
```

### 5. Implement Tool Retry Logic

Modify tool execution in `AgentExecutor`:
```java
ToolResult result = executeWithRetry(tool, decision.getToolInput());
```

---

## Configuration Reference

### application.yml

```yaml
spring:
  application:
    name: ai-agent-app
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/reno_build}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:admin}
    driver-class-name: org.postgresql.Driver

server:
  port: 8080

ai:
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:gemma:2b}

agent:
  database:
    default-table: ${AGENT_DB_DEFAULT_TABLE:task_context}
    default-search-column: ${AGENT_DB_SEARCH_COLUMN:content}
    default-limit: ${AGENT_DB_DEFAULT_LIMIT:5}
    schema-name: ${AGENT_DB_SCHEMA_NAME:public}
    schema-context-ttl-seconds: ${AGENT_DB_SCHEMA_TTL_SECONDS:60}
```

### Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/reno_build` | PostgreSQL connection |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `admin` | Database password |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama service URL |
| `OLLAMA_MODEL` | `gemma:2b` | LLM model to use |
| `AGENT_DB_DEFAULT_TABLE` | `task_context` | Default search table |
| `AGENT_DB_SEARCH_COLUMN` | `content` | Default search column |
| `AGENT_DB_DEFAULT_LIMIT` | `5` | Max rows returned |
| `AGENT_DB_SCHEMA_NAME` | `public` | Database schema |
| `AGENT_DB_SCHEMA_TTL_SECONDS` | `60` | Schema cache TTL |

---

## Performance Considerations

### Bottlenecks

1. **LLM Inference**: Most time spent waiting for Ollama
   - Mitigation: Use faster models, increase worker threads

2. **Database Queries**: Complex queries on large tables
   - Mitigation: Add indexes, use limit defaults

3. **Schema Caching**: Reduces repeated metadata lookups
   - Default TTL: 60 seconds

### Optimization Tips

- Use smaller LLM models (e.g., `mistral:7b` vs `llama2:13b`)
- Pre-warm Ollama before load testing
- Add database indexes on frequently queried columns
- Increase `schema-context-ttl-seconds` for stable schemas
- Implement query result caching in DatabaseTool

---

## Monitoring & Observability

### Key Metrics to Track

- Tasks submitted per minute
- Average execution steps per task
- Tool invocation frequency
- Error rate and types
- LLM response time
- Database query duration

### Logs to Monitor

```
[ERROR] Agent returned invalid JSON
[ERROR] Unknown tool requested
[ERROR] Repeated tool call blocked
[WARN] Execution stopped after reaching maximum steps
[INFO] Task completed successfully
```

---

**Document End**


# AI Agent Application

An autonomous AI agent platform built with **Spring Boot** and **LangChain4j**, powered by **Ollama** for local LLM inference. This application demonstrates an agentic loop where an AI model makes decisions, executes tools, observes results, and iterates until goals are achieved.

## 🎯 Overview

The AI Agent Application is a framework for building intelligent autonomous agents that can:
- Reason about user goals and determine the best course of action
- Execute tools dynamically based on LLM decisions
- Maintain conversation context and task history
- Handle complex multi-step workflows
- Escalate failures gracefully with detailed error reporting

### Key Features
- **Autonomous Decision Making**: LLM-powered agent decides whether to use tools or provide final responses
- **Tool Integration**: Extensible tool system with database queries, email notifications, and audit logging
- **In-Memory State Management**: Track task progress, steps, and execution history
- **Error Handling**: Built-in guards against infinite loops and invalid decisions
- **REST API**: Full HTTP interface for task execution and retrieval
- **Type-Safe**: Structured JSON decision schema with validation

---

## 🏗️ Architecture

### System Components

```
┌─────────────────────────────────────────────────────────┐
│                    REST API                              │
│              (AgentController)                            │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│                AgentService                              │
│         (Orchestrates execution)                         │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│              AgentExecutor                               │
│    (Agentic Loop: Plan → Act → Observe)                 │
└────┬────────────────────────────────────────┬───────────┘
     │                                        │
     │                                        │
┌────▼──────────────┐            ┌───────────▼──────┐
│   TaskAgent       │            │   Agent Tools    │
│ (LLM Inference)   │            │   Registry       │
│  via Ollama       │            └───────────────────┘
└─────────────────┘                     │
                                        │
                    ┌───────────────────┼─────────────────┐
                    │                   │                 │
             ┌──────▼──────┐    ┌──────▼──────┐   ┌──────▼──────┐
             │ Database    │    │ Email Tool  │   │ Logging     │
             │ Tool        │    │             │   │ Tool        │
             └─────────────┘    └─────────────┘   └─────────────┘
```

### Component Descriptions

| Component | Purpose |
|-----------|---------|
| **AgentController** | REST endpoint handler for task submission, status retrieval, and step history |
| **AgentService** | Business logic layer managing task lifecycle and execution flow |
| **AgentExecutor** | Core agentic loop - decides, executes tools, and manages iteration |
| **TaskAgent** | LangChain4j interface to Ollama LLM for decision making |
| **InMemoryStore** | Ephemeral task and step storage (suitable for stateless deployments) |
| **Agent Tools** | Pluggable tools the LLM can invoke (database, email, logging) |

---

## 🔌 REST API Endpoints

### Base URL
```
http://localhost:8080/api/agent
```

### Endpoints

#### 1. Health Check
```http
GET /api/agent/health
```
**Description**: Check if the agent service is running.

**Response** (200 OK):
```json
{
  "status": "up"
}
```

#### 2. Execute Task
```http
POST /api/agent/tasks
Content-Type: application/json
```

**Description**: Submit a goal for the agent to execute.

**Request Body**:
```json
{
  "task": "Find all users in the database where the content contains 'important'"
}
```

**Response** (200 OK):
```json
{
  "id": "task-1234567890",
  "goal": "Find all users in the database where the content contains 'important'",
  "status": "COMPLETED",
  "maxSteps": 5,
  "steps": [
    {
      "stepNumber": 1,
      "type": "PLAN",
      "content": "The goal is to search for users with 'important' in their content. I'll use the database tool to query for this.",
      "toolResult": null
    },
    {
      "stepNumber": 1,
      "type": "ACTION",
      "content": "Calling tool 'database' with input {\"table\":\"users\",\"column\":\"content\",\"criteria\":\"important\"}",
      "toolResult": null
    },
    {
      "stepNumber": 1,
      "type": "OBSERVATION",
      "content": "[{\"id\":1,\"content\":\"important task\"}]",
      "toolResult": {
        "toolName": "database",
        "success": true,
        "output": "[{\"id\":1,\"content\":\"important task\"}]"
      }
    },
    {
      "stepNumber": 2,
      "type": "FINAL",
      "content": "I found 1 user with 'important' in their content.",
      "toolResult": null
    }
  ],
  "finalResponse": "I found 1 user with 'important' in their content.",
  "createdAt": "2026-04-03T10:30:00Z"
}
```

**Status Codes**:
- `200 OK`: Task executed successfully (check `status` field for completion state)
- `400 Bad Request`: Invalid task request
- `500 Internal Server Error`: Server-side execution failure

#### 3. Get Task Status
```http
GET /api/agent/tasks/{taskId}
```

**Description**: Retrieve the current status and details of a specific task.

**Path Parameters**:
- `taskId` (string): The unique task identifier from the execute response

**Response** (200 OK):
```json
{
  "id": "task-1234567890",
  "goal": "Find all users in the database where the content contains 'important'",
  "status": "COMPLETED",
  "maxSteps": 5,
  "steps": [...],
  "finalResponse": "I found 1 user with 'important' in their content.",
  "createdAt": "2026-04-03T10:30:00Z"
}
```

#### 4. Get Task Steps
```http
GET /api/agent/tasks/{taskId}/steps
```

**Description**: Retrieve the execution steps for a task.

**Path Parameters**:
- `taskId` (string): The unique task identifier

**Response** (200 OK):
```json
[
  {
    "stepNumber": 1,
    "type": "PLAN",
    "content": "The goal is to search for users...",
    "toolResult": null
  },
  {
    "stepNumber": 1,
    "type": "ACTION",
    "content": "Calling tool 'database'...",
    "toolResult": null
  },
  {
    "stepNumber": 1,
    "type": "OBSERVATION",
    "content": "[{\"id\":1,\"content\":\"important task\"}]",
    "toolResult": {
      "toolName": "database",
      "success": true,
      "output": "[{\"id\":1,\"content\":\"important task\"}]"
    }
  }
]
```

---

## 🛠️ Available Tools

The LLM model can invoke the following tools during task execution:

### 1. Database Tool
**Name**: `database`

**Description**: Execute read-only SQL queries against PostgreSQL database or perform table lookups.

**Invoke Signature**:
```json
{
  "decisionType": "TOOL",
  "toolName": "database",
  "toolInput": {
    "sql": "SELECT * FROM users WHERE status = ?",
    "params": ["active"]
  }
}
```

**Parameters**:
- `sql` (string): SELECT query only. Parameterized queries for security
- `params` (array): Bind parameters for the query
- **OR**
- `table` (string): Table name for simple lookup
- `criteria` (string): Search criteria
- `column` (string): Column to search in (default: "content")
- `limit` (integer): Max rows to return (default: 5)

**Output Example**:
```json
{
  "toolName": "database",
  "success": true,
  "output": "[{\"id\":1,\"name\":\"John\",\"status\":\"active\"},{\"id\":2,\"name\":\"Jane\",\"status\":\"active\"}]"
}
```

**Security Features**:
- Read-only queries only (SELECT enforced)
- Table and column name validation with regex
- SQL injection protection via parameterized queries
- Schema context caching for performance

**Schema Context**:
The tool automatically provides schema information to the LLM:
```
Schema 'public' tables -> users: id (bigint), name (varchar), status (varchar) | 
  orders: id (bigint), user_id (bigint), amount (numeric)
```

---

### 2. Email Tool
**Name**: `email`

**Description**: Send email notifications with recipient, subject, and body.

**Invoke Signature**:
```json
{
  "decisionType": "TOOL",
  "toolName": "email",
  "toolInput": {
    "recipient": "admin@example.com",
    "subject": "Agent Task Complete",
    "body": "The requested task has finished processing."
  }
}
```

**Parameters**:
- `recipient` (string): Email address (default: "ops@research-agent.local")
- `subject` (string): Email subject (default: "Agent notification")
- `body` (string): Email body content

**Output Example**:
```json
{
  "toolName": "email",
  "success": true,
  "output": "Email queued for admin@example.com with subject 'Agent Task Complete' and body 'The requested task has finished processing.'"
}
```

**Notes**:
- Currently queues emails (mock implementation)
- Can be extended with SMTP integration
- Useful for notifications and escalations

---

### 3. Logging Tool
**Name**: `logging`

**Description**: Write audit log messages to application logs.

**Invoke Signature**:
```json
{
  "decisionType": "TOOL",
  "toolName": "logging",
  "toolInput": {
    "message": "User database query returned 15 results"
  }
}
```

**Parameters**:
- `message` (string): Log message content (default: "No message provided")

**Output Example**:
```json
{
  "toolName": "logging",
  "success": true,
  "output": "Logged message: User database query returned 15 results"
}
```

**Use Cases**:
- Audit trails for compliance
- Workflow tracking for debugging
- Performance metrics logging
- Not recommended for ordinary user-facing replies

---

## 🧠 LLM Decision Schema

The TaskAgent interface (powered by Ollama) expects structured JSON responses following this schema:

```json
{
  "decisionType": "TOOL" | "FINAL",
  "summary": "Brief reason for the next step",
  "toolName": "database" | "email" | "logging",
  "toolInput": {
    "key": "value"
  },
  "finalResponse": "Final answer when decisionType is FINAL"
}
```

### Decision Rules

| Decision Type | Use Case | Example |
|---------------|----------|---------|
| **TOOL** | Agent needs external action | Query database, send email, log event |
| **FINAL** | Goal satisfied, provide answer | "I found 5 matching records", Greetings, Explanations |

### Key Constraints (Enforced by System)

1. **No Repeated Tool Calls**: Agent cannot call the same tool with identical input twice in a row
2. **One Tool Per Step**: Only one tool invocation per decision
3. **Valid JSON Only**: Raw decisions must be valid JSON (markdown code blocks auto-stripped)
4. **Known Tools Only**: Tool names must match registered tools (database, email, logging)
5. **Max 5 Steps**: Default iteration limit prevents infinite loops (configurable)
6. **Graceful Failures**: Invalid decisions halt execution with error context

---

## 🚀 Getting Started

### Prerequisites
- Java 25+
- Maven 3.8+
- Docker (for PostgreSQL and Ollama)
- 4GB+ RAM for Ollama

### Installation

#### 1. Clone Repository
```bash
git clone <repository-url>
cd research_agent
```

#### 2. Start PostgreSQL
```bash
docker run --name postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=reno_build \
  -p 5432:5432 \
  -d postgres:latest
```

#### 3. Start Ollama
```bash
# Option 1: Docker
docker run -d --name ollama \
  -p 11434:11434 \
  -v ollama:/root/.ollama \
  ollama/ollama

# Then pull a model (e.g., gemma:2b)
docker exec ollama ollama pull gemma:2b

# Option 2: Local Installation
ollama serve  # In one terminal
ollama pull gemma:2b  # In another terminal
```

#### 4. Build Application
```bash
mvn clean package
```

#### 5. Run Application
```bash
java -jar target/ai-agent-app-1.0-SNAPSHOT.jar
```

Or with environment variables:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/reno_build"
export DB_USERNAME="postgres"
export DB_PASSWORD="admin"
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="gemma:2b"

mvn spring-boot:run
```

### Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/reno_build
    username: postgres
    password: admin

ai:
  ollama:
    base-url: http://localhost:11434
    model: gemma:2b

agent:
  database:
    default-table: task_context
    default-search-column: content
    default-limit: 5
    schema-name: public
    schema-context-ttl-seconds: 60
```

---

## 📋 Example Usage

### Example 1: Simple Database Query
```bash
curl -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Find all records in the task_context table that mention deadline"
  }'
```

### Example 2: Multi-Step Workflow
```bash
curl -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Find users with status=pending in the database and send them a notification email"
  }'
```

The agent will:
1. Decide to query the database for pending users
2. Observe the results
3. Decide to send email notifications
4. Provide a final response with summary

### Example 3: Check Task Status
```bash
# After executing a task, you'll get task ID like "task-1680000000000"
curl http://localhost:8080/api/agent/tasks/task-1680000000000
```

---

## 📊 Task Lifecycle

```
INITIAL → RUNNING → [Loop: Plan → Act → Observe]
                           ↓
                    Goal satisfied?
                    ├─ YES → COMPLETED
                    └─ NO  → Max steps reached? 
                            ├─ YES → FAILED
                            └─ NO  → Continue loop
```

### Task Status Values
- **RUNNING**: Currently executing agent loop
- **COMPLETED**: Goal achieved successfully
- **FAILED**: Execution stopped (max steps, invalid decision, or error)

### Step Types
- **PLAN**: Agent's reasoning for the next action
- **ACTION**: Tool invocation details
- **OBSERVATION**: Tool execution result
- **FINAL**: Final response from agent
- **ERROR**: Execution error (repeated tool, unknown tool, etc.)

---

## 🔐 Security Considerations

### Database Tool
- ✅ Read-only SELECT queries enforced
- ✅ Parameterized queries prevent SQL injection
- ✅ Identifier validation for table/column names
- ✅ Schema context caching prevents enumeration delays

### Email Tool
- ⚠️ Currently mock implementation (queued, not sent)
- 🔒 Should implement SMTP with TLS in production
- 🔒 Consider rate limiting to prevent spam

### Logging Tool
- ✅ Standard SLF4J integration
- 🔒 Ensure logs don't contain sensitive data
- 📝 Implement log aggregation for audit trails

### General
- 🔒 API should run behind authentication gateway in production
- 🔒 Validate all user input before passing to LLM
- 🔒 Rate limit task submission to prevent resource exhaustion
- 🔒 Monitor token usage on Ollama for cost control

---

## 🧪 Testing

Run unit tests:
```bash
mvn test
```

Manual test with curl:
```bash
# Health check
curl http://localhost:8080/api/agent/health

# Submit task
TASK_ID=$(curl -s -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{"task":"Hello, who are you?"}' | jq -r '.id')

# Check status
curl http://localhost:8080/api/agent/tasks/$TASK_ID

# Get steps
curl http://localhost:8080/api/agent/tasks/$TASK_ID/steps
```

---

## 🛣️ Roadmap

- [ ] SQLite backend support for edge deployments
- [ ] Custom tool registration via Spring Boot plugins
- [ ] Persistent task storage (replace in-memory)
- [ ] WebSocket support for streaming responses
- [ ] Tool retry logic with exponential backoff
- [ ] Multi-agent orchestration
- [ ] Prompt optimization and few-shot examples
- [ ] Observability (metrics, tracing, profiling)

---

## 📝 Dependencies

- **Spring Boot 3.5.6**: Web framework and DI container
- **Spring JDBC**: Database connectivity
- **LangChain4j 0.30.0**: LLM integration framework
- **LangChain4j Ollama**: Ollama provider for local LLM inference
- **PostgreSQL Driver**: Database driver
- **Jackson**: JSON serialization

See `pom.xml` for complete dependency tree.

---

## 📄 License

This project is part of the Research Agent framework.

---

## 🤝 Contributing

To extend the agent with new tools:

1. Implement `AgentTool` interface:
```java
@Component
public class MyTool implements AgentTool {
    @Override
    public String getName() { return "my-tool"; }
    
    @Override
    public String getDescription() { return "Tool description"; }
    
    @Override
    public ToolResult execute(Map<String, Object> input) {
        // Implementation
    }
}
```

2. Update `TaskAgent` system prompt with new tool name
3. The `AgentExecutor` will automatically discover and register the tool

---

## 📚 Additional Resources

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Ollama Getting Started](https://ollama.ai/)
- [Spring Boot Reference](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## ❓ FAQ

**Q: How do I add a new tool?**  
A: Implement the `AgentTool` interface, mark it with `@Component`, and it's automatically registered.

**Q: Can I use a different LLM?**  
A: Yes! LangChain4j supports OpenAI, Anthropic, Azure, and others. Replace the Ollama configuration.

**Q: How do I increase iteration limit?**  
A: Set `DEFAULT_MAX_STEPS` in `AgentExecutor` (currently 5).

**Q: Can I persist tasks to a database?**  
A: Yes, replace `InMemoryStore` with a database-backed implementation.

**Q: What happens if the LLM returns invalid JSON?**  
A: The executor catches the error and halts execution with a detailed error message.

---

**Last Updated**: April 3, 2026  
**Version**: 1.0-SNAPSHOT

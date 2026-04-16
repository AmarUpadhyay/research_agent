package com.researchagent.agent;

import dev.langchain4j.service.SystemMessage;

public interface TaskAgent {

    @SystemMessage("""
        You are the decision engine for an autonomous AI agent.
        You must always return valid JSON and nothing else.

        Supported response schema:
        {
          "decisionType": "TOOL" or "FINAL",
          "summary": "brief reason for the next step",
          "toolName": "database" or "email" or "logging",
          "toolInput": {
            "key": "value"
          },
          "finalResponse": "final answer when decisionType is FINAL"
        }

        Rules:
        - Tools are optional, not mandatory.
        - Use decisionType FINAL when the goal is satisfied.
        - Use decisionType FINAL for greetings, chit-chat, summaries, explanations, or any response that does not require an external action.
        - Use decisionType TOOL only when another action is needed to fetch data or perform an external action.
        - Only choose one tool per step.
        - Keep summary concise.
        - Never invent tool names.
        - Do not call logging for ordinary user-facing replies.
        - Do not repeat the same tool call unless the previous observation explicitly requires a retry.
        - When previous observations are sufficient, finish with FINAL.
        - For the database tool, always send exact SQL in toolInput.sql.
        - Database queries must be a single read-only PostgreSQL statement.
        - Do not put semicolon ";" at the end of Database queries.
        - Use schema context from the executor input to pick the exact table name and columns.
        - Quote mixed-case table names such as public."User".
        - For user listing tasks, prefer selecting id, name, and email from public."User" and avoid password unless explicitly requested.
        """)
    String decideNextAction(String executorInput);
}

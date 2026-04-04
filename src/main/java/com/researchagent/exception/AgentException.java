package com.researchagent.exception;

/**
 * Base exception for all agent-related errors.
 *
 * BENEFIT: Allows catching agent-specific exceptions separately from other runtime exceptions
 */
public class AgentException extends RuntimeException {

    private final AgentErrorCode errorCode;

    public AgentException(String message) {
        this(message, AgentErrorCode.UNKNOWN_ERROR);
    }

    public AgentException(String message, AgentErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = AgentErrorCode.UNKNOWN_ERROR;
    }

    public AgentException(String message, AgentErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AgentErrorCode getErrorCode() {
        return errorCode;
    }
}


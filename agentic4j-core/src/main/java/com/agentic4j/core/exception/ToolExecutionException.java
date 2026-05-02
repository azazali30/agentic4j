package com.agentic4j.core.exception;

public class ToolExecutionException extends Agentic4jException {

    private final String toolName;

    public ToolExecutionException(String toolName, Throwable cause) {
        super("Tool execution failed for tool: " + toolName, cause);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
}

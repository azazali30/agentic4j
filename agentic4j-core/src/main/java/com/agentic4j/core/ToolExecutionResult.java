package com.agentic4j.core;

public class ToolExecutionResult {

    private final String toolCallId;
    private final String toolName;
    private final String result;

    public ToolExecutionResult(String toolCallId, String toolName, String result) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.result = result;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getResult() {
        return result;
    }
}

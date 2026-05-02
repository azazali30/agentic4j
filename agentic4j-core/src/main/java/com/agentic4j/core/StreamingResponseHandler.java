package com.agentic4j.core;

public interface StreamingResponseHandler {
    void onToken(String token);
    void onToolExecution(ToolExecutionResult result);
    void onComplete(ChatResponse response);
    void onError(Throwable error);
}

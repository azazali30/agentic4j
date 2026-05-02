package com.agentic4j.core;

import java.util.function.Consumer;

public interface StreamingResponse {
    StreamingResponse onToken(Consumer<String> handler);
    StreamingResponse onToolExecuted(Consumer<ToolExecutionResult> handler);
    StreamingResponse onComplete(Consumer<ChatResponse> handler);
    StreamingResponse onError(Consumer<Throwable> handler);
    void start();
}

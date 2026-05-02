package com.agentic4j.core;

public interface StreamingChatModel {
    void send(ChatRequest request, StreamingResponseHandler handler);
}

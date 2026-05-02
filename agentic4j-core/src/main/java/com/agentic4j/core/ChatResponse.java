package com.agentic4j.core;

public class ChatResponse {

    private final ChatMessage message;
    private final TokenUsage usage;
    private final FinishReason finishReason;

    public ChatResponse(ChatMessage message, TokenUsage usage, FinishReason finishReason) {
        this.message = message;
        this.usage = usage;
        this.finishReason = finishReason;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public TokenUsage getUsage() {
        return usage;
    }

    public FinishReason getFinishReason() {
        return finishReason;
    }
}

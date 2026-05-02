package com.agentic4j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRequest {

    private final List<ChatMessage> messages;
    private final List<ToolDefinition> tools;

    private ChatRequest(List<ChatMessage> messages, List<ToolDefinition> tools) {
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
        this.tools = Collections.unmodifiableList(new ArrayList<ToolDefinition>(tools));
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<ChatMessage> messages = new ArrayList<ChatMessage>();
        private final List<ToolDefinition> tools = new ArrayList<ToolDefinition>();

        private Builder() {
        }

        public Builder addMessage(ChatMessage message) {
            messages.add(message);
            return this;
        }

        public Builder addMessages(List<ChatMessage> messages) {
            this.messages.addAll(messages);
            return this;
        }

        public Builder addTool(ToolDefinition tool) {
            tools.add(tool);
            return this;
        }

        public Builder addTools(List<ToolDefinition> tools) {
            this.tools.addAll(tools);
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(messages, tools);
        }
    }
}

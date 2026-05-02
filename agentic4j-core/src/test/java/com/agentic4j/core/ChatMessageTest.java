package com.agentic4j.core;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class ChatMessageTest {

    @Test
    public void testRoleValues() {
        assertEquals(4, Role.values().length);
        assertNotNull(Role.SYSTEM);
        assertNotNull(Role.USER);
        assertNotNull(Role.ASSISTANT);
        assertNotNull(Role.TOOL);
    }

    @Test
    public void testFinishReasonValues() {
        assertEquals(4, FinishReason.values().length);
        assertNotNull(FinishReason.STOP);
        assertNotNull(FinishReason.TOOL_CALLS);
        assertNotNull(FinishReason.LENGTH);
        assertNotNull(FinishReason.CONTENT_FILTER);
    }

    @Test
    public void testTokenUsage() {
        TokenUsage usage = new TokenUsage(10, 20, 30);
        assertEquals(10, usage.getPromptTokens());
        assertEquals(20, usage.getCompletionTokens());
        assertEquals(30, usage.getTotalTokens());
    }

    @Test
    public void testToolCall() {
        ToolCall call = new ToolCall("call_123", "searchProducts", "{\"term\":\"milk\"}");
        assertEquals("call_123", call.getId());
        assertEquals("searchProducts", call.getName());
        assertEquals("{\"term\":\"milk\"}", call.getArguments());
    }

    @Test
    public void testToolParameter() {
        ToolParameter param = new ToolParameter("query", "The SQL query", "string", true);
        assertEquals("query", param.getName());
        assertEquals("The SQL query", param.getDescription());
        assertEquals("string", param.getType());
        assertTrue(param.isRequired());
    }

    @Test
    public void testToolDefinition() {
        ToolParameter param = new ToolParameter("term", "search term", "string", true);
        ToolDefinition def = new ToolDefinition("searchProducts", "Search for products", Collections.singletonList(param));
        assertEquals("searchProducts", def.getName());
        assertEquals("Search for products", def.getDescription());
        assertEquals(1, def.getParameters().size());
        assertEquals("term", def.getParameters().get(0).getName());
    }

    @Test
    public void testToolExecutionResult() {
        ToolExecutionResult result = new ToolExecutionResult("call_123", "searchProducts", "Found 3 products");
        assertEquals("call_123", result.getToolCallId());
        assertEquals("searchProducts", result.getToolName());
        assertEquals("Found 3 products", result.getResult());
    }

    @Test
    public void testChatMessageUser() {
        ChatMessage msg = ChatMessage.user("Hello");
        assertEquals(Role.USER, msg.getRole());
        assertEquals("Hello", msg.getContent());
        assertNull(msg.getToolCalls());
        assertNull(msg.getToolCallId());
    }

    @Test
    public void testChatMessageSystem() {
        ChatMessage msg = ChatMessage.system("You are a helpful assistant");
        assertEquals(Role.SYSTEM, msg.getRole());
        assertEquals("You are a helpful assistant", msg.getContent());
    }

    @Test
    public void testChatMessageAssistantWithToolCalls() {
        ToolCall call = new ToolCall("call_1", "search", "{\"q\":\"test\"}");
        ChatMessage msg = ChatMessage.assistantWithToolCalls(Collections.singletonList(call));
        assertEquals(Role.ASSISTANT, msg.getRole());
        assertNull(msg.getContent());
        assertEquals(1, msg.getToolCalls().size());
        assertEquals("call_1", msg.getToolCalls().get(0).getId());
    }

    @Test
    public void testChatMessageToolResult() {
        ChatMessage msg = ChatMessage.toolResult("call_1", "result text");
        assertEquals(Role.TOOL, msg.getRole());
        assertEquals("result text", msg.getContent());
        assertEquals("call_1", msg.getToolCallId());
    }
}

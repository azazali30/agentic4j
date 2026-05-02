package com.agentic4j.core;

import com.agentic4j.core.agent.AgentBuilder;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.exception.MaxIterationsException;
import com.agentic4j.core.memory.SlidingWindowMemory;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class AgentBuilderSyncTest {

    interface SimpleAssistant {
        @SystemPrompt("You are a helpful assistant")
        String chat(String message);
    }

    interface ResourceAssistant {
        @SystemPrompt(fromResource = "test-system-prompt.txt")
        String chat(String message);
    }

    public static class MockSearchTool {
        @AgentTool("Search for products")
        public String searchProducts(@Param("search term") String term) {
            return "Found: milk, cheese, yogurt";
        }
    }

    @Test
    public void testSimpleChatNoTools() {
        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                return new ChatResponse(ChatMessage.assistant("Hello! How can I help?"), new TokenUsage(10, 5, 15), FinishReason.STOP);
            }
        };
        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class).chatModel(mockModel).memory(new SlidingWindowMemory(20)).build();
        assertEquals("Hello! How can I help?", assistant.chat("Hi"));
    }

    @Test
    public void testSystemPromptIncludedInRequest() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();
        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                return new ChatResponse(ChatMessage.assistant("OK"), new TokenUsage(10, 5, 15), FinishReason.STOP);
            }
        };
        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class).chatModel(mockModel).memory(new SlidingWindowMemory(20)).build();
        assistant.chat("Hello");
        assertEquals(Role.SYSTEM, captured.get(0).getMessages().get(0).getRole());
        assertEquals("You are a helpful assistant", captured.get(0).getMessages().get(0).getContent());
        assertEquals(Role.USER, captured.get(0).getMessages().get(1).getRole());
    }

    @Test
    public void testReActLoopWithToolCall() {
        final int[] callCount = {0};
        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                callCount[0]++;
                if (callCount[0] == 1) {
                    ToolCall toolCall = new ToolCall("call_1", "searchProducts", "{\"term\":\"milk\"}");
                    return new ChatResponse(ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall)), new TokenUsage(20, 10, 30), FinishReason.TOOL_CALLS);
                } else {
                    return new ChatResponse(ChatMessage.assistant("I found milk, cheese, and yogurt for you."), new TokenUsage(30, 15, 45), FinishReason.STOP);
                }
            }
        };
        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class).chatModel(mockModel).memory(new SlidingWindowMemory(20)).tools(new MockSearchTool()).build();
        String reply = assistant.chat("Find dairy products");
        assertEquals("I found milk, cheese, and yogurt for you.", reply);
        assertEquals(2, callCount[0]);
    }

    @Test
    public void testToolDefinitionsSentToModel() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();
        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                return new ChatResponse(ChatMessage.assistant("OK"), new TokenUsage(10, 5, 15), FinishReason.STOP);
            }
        };
        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class).chatModel(mockModel).memory(new SlidingWindowMemory(20)).tools(new MockSearchTool()).build();
        assistant.chat("test");
        assertFalse(captured.get(0).getTools().isEmpty());
        assertEquals("searchProducts", captured.get(0).getTools().get(0).getName());
    }

    @Test
    public void testMemoryPersistsAcrossCalls() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();
        final int[] callCount = {0};
        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                callCount[0]++;
                return new ChatResponse(ChatMessage.assistant("Reply " + callCount[0]), new TokenUsage(10, 5, 15), FinishReason.STOP);
            }
        };
        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class).chatModel(mockModel).memory(new SlidingWindowMemory(20)).build();
        assistant.chat("First message");
        assistant.chat("Second message");
        ChatRequest secondReq = captured.get(1);
        assertEquals(4, secondReq.getMessages().size());
        assertEquals(Role.SYSTEM, secondReq.getMessages().get(0).getRole());
        assertEquals("First message", secondReq.getMessages().get(1).getContent());
        assertEquals("Reply 1", secondReq.getMessages().get(2).getContent());
        assertEquals("Second message", secondReq.getMessages().get(3).getContent());
    }

    @Test(expected = MaxIterationsException.class)
    public void testMaxIterationsThrows() {
        ChatModel infiniteToolModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                ToolCall toolCall = new ToolCall("call_x", "searchProducts", "{\"term\":\"loop\"}");
                return new ChatResponse(ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall)), new TokenUsage(10, 5, 15), FinishReason.TOOL_CALLS);
            }
        };
        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class).chatModel(infiniteToolModel).memory(new SlidingWindowMemory(20)).tools(new MockSearchTool()).maxToolIterations(3).build();
        assistant.chat("Loop forever");
    }

    @Test
    public void testSystemPromptFromResource() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();
        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                return new ChatResponse(ChatMessage.assistant("OK"), new TokenUsage(10, 5, 15), FinishReason.STOP);
            }
        };
        ResourceAssistant assistant = AgentBuilder.forInterface(ResourceAssistant.class).chatModel(mockModel).memory(new SlidingWindowMemory(20)).build();
        assistant.chat("test");
        assertEquals(Role.SYSTEM, captured.get(0).getMessages().get(0).getRole());
        assertTrue(captured.get(0).getMessages().get(0).getContent().length() > 0);
    }
}

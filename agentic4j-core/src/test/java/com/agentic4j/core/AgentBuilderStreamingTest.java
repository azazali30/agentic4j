package com.agentic4j.core;

import com.agentic4j.core.agent.AgentBuilder;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.memory.SlidingWindowMemory;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class AgentBuilderStreamingTest {

    interface StreamAssistant {
        @SystemPrompt("You are a helpful assistant")
        StreamingResponse chat(String message);
    }

    public static class MockTool {
        @AgentTool("Test tool")
        public String doWork(@Param("input") String input) { return "Tool result for: " + input; }
    }

    @Test
    public void testStreamingSimpleResponse() throws Exception {
        StreamingChatModel mockStreamModel = new StreamingChatModel() {
            @Override
            public void send(ChatRequest request, StreamingResponseHandler handler) {
                handler.onToken("Hello ");
                handler.onToken("world!");
                handler.onComplete(new ChatResponse(ChatMessage.assistant("Hello world!"), new TokenUsage(10, 5, 15), FinishReason.STOP));
            }
        };
        StreamAssistant assistant = AgentBuilder.forInterface(StreamAssistant.class).streamingChatModel(mockStreamModel).memory(new SlidingWindowMemory(20)).build();
        final List<String> tokens = new ArrayList<String>();
        final ChatResponse[] finalResponse = new ChatResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);
        assistant.chat("Hi")
            .onToken(new Consumer<String>() { @Override public void accept(String t) { tokens.add(t); } })
            .onComplete(new Consumer<ChatResponse>() { @Override public void accept(ChatResponse r) { finalResponse[0] = r; latch.countDown(); } })
            .start();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, tokens.size());
        assertEquals("Hello ", tokens.get(0));
        assertEquals("Hello world!", finalResponse[0].getMessage().getContent());
    }

    @Test
    public void testStreamingWithToolExecution() throws Exception {
        final int[] callCount = {0};
        StreamingChatModel mockStreamModel = new StreamingChatModel() {
            @Override
            public void send(ChatRequest request, StreamingResponseHandler handler) {
                callCount[0]++;
                if (callCount[0] == 1) {
                    ToolCall toolCall = new ToolCall("call_1", "doWork", "{\"input\":\"test\"}");
                    handler.onComplete(new ChatResponse(ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall)), new TokenUsage(10, 5, 15), FinishReason.TOOL_CALLS));
                } else {
                    handler.onToken("Done!");
                    handler.onComplete(new ChatResponse(ChatMessage.assistant("Done!"), new TokenUsage(10, 5, 15), FinishReason.STOP));
                }
            }
        };
        StreamAssistant assistant = AgentBuilder.forInterface(StreamAssistant.class).streamingChatModel(mockStreamModel).memory(new SlidingWindowMemory(20)).tools(new MockTool()).build();
        final List<String> tokens = new ArrayList<String>();
        final List<ToolExecutionResult> toolResults = new ArrayList<ToolExecutionResult>();
        final CountDownLatch latch = new CountDownLatch(1);
        assistant.chat("Do work")
            .onToken(new Consumer<String>() { @Override public void accept(String t) { tokens.add(t); } })
            .onToolExecuted(new Consumer<ToolExecutionResult>() { @Override public void accept(ToolExecutionResult r) { toolResults.add(r); } })
            .onComplete(new Consumer<ChatResponse>() { @Override public void accept(ChatResponse r) { latch.countDown(); } })
            .start();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, toolResults.size());
        assertEquals("doWork", toolResults.get(0).getToolName());
        assertTrue(toolResults.get(0).getResult().contains("Tool result for: test"));
        assertEquals(1, tokens.size());
        assertEquals("Done!", tokens.get(0));
    }

    @Test
    public void testStreamingError() throws Exception {
        StreamingChatModel mockStreamModel = new StreamingChatModel() {
            @Override
            public void send(ChatRequest request, StreamingResponseHandler handler) {
                handler.onError(new RuntimeException("Connection failed"));
            }
        };
        StreamAssistant assistant = AgentBuilder.forInterface(StreamAssistant.class).streamingChatModel(mockStreamModel).memory(new SlidingWindowMemory(20)).build();
        final Throwable[] capturedError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);
        assistant.chat("test")
            .onError(new Consumer<Throwable>() { @Override public void accept(Throwable e) { capturedError[0] = e; latch.countDown(); } })
            .start();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Connection failed", capturedError[0].getMessage());
    }
}

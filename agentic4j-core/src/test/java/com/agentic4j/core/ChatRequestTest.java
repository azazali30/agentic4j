package com.agentic4j.core;

import com.agentic4j.core.exception.Agentic4jException;
import com.agentic4j.core.exception.MaxIterationsException;
import com.agentic4j.core.exception.ModelException;
import com.agentic4j.core.exception.ToolExecutionException;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class ChatRequestTest {

    @Test
    public void testChatRequestBuilder() {
        ChatMessage msg = ChatMessage.user("Hello");
        ToolParameter param = new ToolParameter("q", "query", "string", true);
        ToolDefinition tool = new ToolDefinition("search", "Search", Collections.singletonList(param));
        ChatRequest request = ChatRequest.builder().addMessage(msg).addTool(tool).build();
        assertEquals(1, request.getMessages().size());
        assertEquals("Hello", request.getMessages().get(0).getContent());
        assertEquals(1, request.getTools().size());
        assertEquals("search", request.getTools().get(0).getName());
    }

    @Test
    public void testChatRequestBuilderNoTools() {
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Hello")).build();
        assertEquals(1, request.getMessages().size());
        assertTrue(request.getTools().isEmpty());
    }

    @Test
    public void testChatRequestMessagesAreUnmodifiable() {
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("test")).build();
        try {
            request.getMessages().add(ChatMessage.user("hack"));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) { }
    }

    @Test
    public void testChatResponse() {
        ChatMessage msg = ChatMessage.assistant("Hi there");
        TokenUsage usage = new TokenUsage(5, 10, 15);
        ChatResponse response = new ChatResponse(msg, usage, FinishReason.STOP);
        assertEquals("Hi there", response.getMessage().getContent());
        assertEquals(15, response.getUsage().getTotalTokens());
        assertEquals(FinishReason.STOP, response.getFinishReason());
    }

    @Test
    public void testAgentic4jException() {
        Agentic4jException ex = new Agentic4jException("test error");
        assertEquals("test error", ex.getMessage());
        Agentic4jException exWithCause = new Agentic4jException("wrapped", new RuntimeException("root"));
        assertEquals("root", exWithCause.getCause().getMessage());
    }

    @Test
    public void testModelException() {
        ModelException ex = new ModelException(429, "Rate limited", "Too many requests");
        assertEquals(429, ex.getStatusCode());
        assertEquals("Rate limited", ex.getMessage());
        assertEquals("Too many requests", ex.getResponseBody());
        assertTrue(ex instanceof Agentic4jException);
    }

    @Test
    public void testToolExecutionException() {
        ToolExecutionException ex = new ToolExecutionException("searchProducts", new RuntimeException("DB down"));
        assertEquals("searchProducts", ex.getToolName());
        assertTrue(ex.getMessage().contains("searchProducts"));
        assertTrue(ex instanceof Agentic4jException);
    }

    @Test
    public void testMaxIterationsException() {
        MaxIterationsException ex = new MaxIterationsException(10);
        assertEquals(10, ex.getMaxIterations());
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex instanceof Agentic4jException);
    }
}

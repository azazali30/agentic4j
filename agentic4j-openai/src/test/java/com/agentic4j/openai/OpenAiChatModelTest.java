package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.agentic4j.core.exception.ModelException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class OpenAiChatModelTest {
    private MockWebServer server;
    private OpenAiChatModel model;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        model = OpenAiChatModel.builder().apiKey("test-key").baseUrl(server.url("/v1").toString()).modelName("gpt-4o-mini").temperature(0.0).build();
    }

    @After
    public void tearDown() throws Exception { server.shutdown(); }

    @Test
    public void testSimpleChat() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello!\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}").setHeader("Content-Type", "application/json"));
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Hi")).build();
        ChatResponse response = model.send(request);
        assertEquals("Hello!", response.getMessage().getContent());
        assertEquals(FinishReason.STOP, response.getFinishReason());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getPath().contains("/chat/completions"));
        assertTrue(recorded.getHeader("Authorization").contains("test-key"));
    }

    @Test
    public void testResponseWithToolCalls() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"search\",\"arguments\":\"{\\\"term\\\":\\\"milk\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}],\"usage\":{\"prompt_tokens\":20,\"completion_tokens\":10,\"total_tokens\":30}}").setHeader("Content-Type", "application/json"));
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Find milk")).build();
        ChatResponse response = model.send(request);
        assertEquals(FinishReason.TOOL_CALLS, response.getFinishReason());
        assertEquals(1, response.getMessage().getToolCalls().size());
        assertEquals("search", response.getMessage().getToolCalls().get(0).getName());
    }

    @Test
    public void testAuthError() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));
        try {
            model.send(ChatRequest.builder().addMessage(ChatMessage.user("Hi")).build());
            fail("Should throw ModelException");
        } catch (ModelException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testRateLimitError() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":{\"message\":\"Rate limit exceeded\"}}"));
        try {
            model.send(ChatRequest.builder().addMessage(ChatMessage.user("Hi")).build());
            fail("Should throw ModelException");
        } catch (ModelException e) {
            assertEquals(429, e.getStatusCode());
        }
    }

    @Test
    public void testRequestIncludesTools() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"OK\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}").setHeader("Content-Type", "application/json"));
        ToolParameter param = new ToolParameter("query", "SQL query", "string", true);
        ToolDefinition tool = new ToolDefinition("executeSql", "Run SQL", Collections.singletonList(param));
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Run query")).addTool(tool).build();
        model.send(request);
        RecordedRequest recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("\"tools\""));
        assertTrue(body.contains("executeSql"));
    }
}

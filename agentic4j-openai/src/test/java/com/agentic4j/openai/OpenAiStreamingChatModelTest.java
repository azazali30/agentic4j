package com.agentic4j.openai;

import com.agentic4j.core.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class OpenAiStreamingChatModelTest {
    private MockWebServer server;
    private OpenAiStreamingChatModel model;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        model = OpenAiStreamingChatModel.builder().apiKey("test-key").baseUrl(server.url("/v1").toString()).modelName("gpt-4o-mini").temperature(0.0).build();
    }

    @After
    public void tearDown() throws Exception { server.shutdown(); }

    @Test
    public void testStreamTokens() throws Exception {
        String sseBody = "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":\"Hello\"},\"finish_reason\":null}]}\n\n"
            + "data: {\"choices\":[{\"delta\":{\"content\":\" world\"},\"finish_reason\":null}]}\n\n"
            + "data: {\"choices\":[{\"delta\":{\"content\":\"!\"},\"finish_reason\":null}]}\n\n"
            + "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":3,\"total_tokens\":8}}\n\n"
            + "data: [DONE]\n\n";
        server.enqueue(new MockResponse().setBody(sseBody).setHeader("Content-Type", "text/event-stream"));
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Hi")).build();
        final List<String> tokens = new ArrayList<String>();
        final ChatResponse[] finalResponse = new ChatResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);
        model.send(request, new StreamingResponseHandler() {
            @Override public void onToken(String token) { tokens.add(token); }
            @Override public void onToolExecution(ToolExecutionResult result) { }
            @Override public void onComplete(ChatResponse response) { finalResponse[0] = response; latch.countDown(); }
            @Override public void onError(Throwable error) { latch.countDown(); }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, tokens.size());
        assertEquals("Hello", tokens.get(0));
        assertEquals(" world", tokens.get(1));
        assertEquals("!", tokens.get(2));
        assertNotNull(finalResponse[0]);
        assertEquals("Hello world!", finalResponse[0].getMessage().getContent());
        assertEquals(FinishReason.STOP, finalResponse[0].getFinishReason());
    }

    @Test
    public void testStreamWithToolCalls() throws Exception {
        String sseBody = "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"search\",\"arguments\":\"\"}}]},\"finish_reason\":null}]}\n\n"
            + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"term\\\"\"}}]},\"finish_reason\":null}]}\n\n"
            + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\":\\\"milk\\\"}\"}}]},\"finish_reason\":null}]}\n\n"
            + "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"tool_calls\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}\n\n"
            + "data: [DONE]\n\n";
        server.enqueue(new MockResponse().setBody(sseBody).setHeader("Content-Type", "text/event-stream"));
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Find milk")).build();
        final ChatResponse[] finalResponse = new ChatResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);
        model.send(request, new StreamingResponseHandler() {
            @Override public void onToken(String token) { }
            @Override public void onToolExecution(ToolExecutionResult result) { }
            @Override public void onComplete(ChatResponse response) { finalResponse[0] = response; latch.countDown(); }
            @Override public void onError(Throwable error) { latch.countDown(); }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(finalResponse[0]);
        assertEquals(FinishReason.TOOL_CALLS, finalResponse[0].getFinishReason());
        assertNotNull(finalResponse[0].getMessage().getToolCalls());
        assertEquals(1, finalResponse[0].getMessage().getToolCalls().size());
        assertEquals("call_1", finalResponse[0].getMessage().getToolCalls().get(0).getId());
        assertEquals("search", finalResponse[0].getMessage().getToolCalls().get(0).getName());
        assertEquals("{\"term\":\"milk\"}", finalResponse[0].getMessage().getToolCalls().get(0).getArguments());
    }

    @Test
    public void testStreamHttpError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Hi")).build();
        final Throwable[] capturedError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);
        model.send(request, new StreamingResponseHandler() {
            @Override public void onToken(String token) { }
            @Override public void onToolExecution(ToolExecutionResult result) { }
            @Override public void onComplete(ChatResponse response) { latch.countDown(); }
            @Override public void onError(Throwable error) { capturedError[0] = error; latch.countDown(); }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(capturedError[0]);
    }
}

package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.agentic4j.core.agent.AgentBuilder;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.memory.SlidingWindowMemory;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class IntegrationSmokeTest {
    private MockWebServer server;

    public interface PriceAssistant {
        @SystemPrompt("You are a price data assistant")
        String chat(String message);
    }

    public static class MockSearchTool {
        @AgentTool("Search for products by name")
        public String searchProducts(@Param("The search term") String term) {
            if (term.toLowerCase().contains("milk")) return "Found: Fresh Milk (ID: 1), Milk Powder (ID: 2)";
            return "No products found for: " + term;
        }
    }

    @Before
    public void setUp() throws Exception { server = new MockWebServer(); server.start(); }

    @After
    public void tearDown() throws Exception { server.shutdown(); }

    @Test
    public void testFullReActLoopWithMockServer() throws Exception {
        final AtomicInteger requestCount = new AtomicInteger(0);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                int count = requestCount.incrementAndGet();
                if (count == 1) {
                    return new MockResponse()
                        .setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"searchProducts\",\"arguments\":\"{\\\"term\\\":\\\"milk\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}],\"usage\":{\"prompt_tokens\":50,\"completion_tokens\":20,\"total_tokens\":70}}")
                        .setHeader("Content-Type", "application/json");
                } else {
                    return new MockResponse()
                        .setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"I found 2 milk products: Fresh Milk and Milk Powder.\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":20,\"total_tokens\":120}}")
                        .setHeader("Content-Type", "application/json");
                }
            }
        });

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .apiKey("test-key").baseUrl(server.url("/v1").toString()).modelName("gpt-4o-mini").temperature(0.0).build();

        PriceAssistant assistant = AgentBuilder.forInterface(PriceAssistant.class)
            .chatModel(chatModel).memory(new SlidingWindowMemory(20)).tools(new MockSearchTool()).build();

        String response = assistant.chat("What milk products do you have?");
        assertEquals("I found 2 milk products: Fresh Milk and Milk Powder.", response);
        assertEquals(2, requestCount.get());
    }
}

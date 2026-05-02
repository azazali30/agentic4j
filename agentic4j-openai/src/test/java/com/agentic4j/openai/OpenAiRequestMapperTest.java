package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class OpenAiRequestMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testMapSimpleRequest() throws Exception {
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.system("You are helpful")).addMessage(ChatMessage.user("Hello")).build();
        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);
        assertEquals("gpt-4o-mini", node.get("model").asText());
        assertEquals(0.0, node.get("temperature").asDouble(), 0.001);
        assertEquals(2, node.get("messages").size());
        assertEquals("system", node.get("messages").get(0).get("role").asText());
        assertEquals("user", node.get("messages").get(1).get("role").asText());
        assertFalse(node.has("stream"));
    }

    @Test
    public void testMapRequestWithStream() throws Exception {
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Hi")).build();
        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.5, null, true);
        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.get("stream").asBoolean());
    }

    @Test
    public void testMapRequestWithTools() throws Exception {
        ToolParameter param = new ToolParameter("query", "The SQL query", "string", true);
        ToolDefinition tool = new ToolDefinition("executeSql", "Execute SQL", Collections.singletonList(param));
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Run query")).addTool(tool).build();
        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.has("tools"));
        assertEquals(1, node.get("tools").size());
        assertEquals("function", node.get("tools").get(0).get("type").asText());
        assertEquals("executeSql", node.get("tools").get(0).get("function").get("name").asText());
        JsonNode paramsSchema = node.get("tools").get(0).get("function").get("parameters");
        assertEquals("object", paramsSchema.get("type").asText());
        assertTrue(paramsSchema.get("properties").has("query"));
        assertEquals("string", paramsSchema.get("properties").get("query").get("type").asText());
    }

    @Test
    public void testMapRequestWithMaxTokens() throws Exception {
        ChatRequest request = ChatRequest.builder().addMessage(ChatMessage.user("Hi")).build();
        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, 100, false);
        JsonNode node = objectMapper.readTree(json);
        assertEquals(100, node.get("max_tokens").asInt());
    }

    @Test
    public void testMapAssistantMessageWithToolCalls() throws Exception {
        ToolCall toolCall = new ToolCall("call_abc", "search", "{\"q\":\"test\"}");
        ChatMessage msg = ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall));
        ChatRequest request = ChatRequest.builder().addMessage(msg).build();
        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);
        JsonNode assistantNode = node.get("messages").get(0);
        assertEquals("assistant", assistantNode.get("role").asText());
        assertTrue(assistantNode.has("tool_calls"));
        assertEquals("call_abc", assistantNode.get("tool_calls").get(0).get("id").asText());
    }

    @Test
    public void testMapToolResultMessage() throws Exception {
        ChatMessage msg = ChatMessage.toolResult("call_abc", "Found 3 results");
        ChatRequest request = ChatRequest.builder().addMessage(msg).build();
        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);
        JsonNode toolNode = node.get("messages").get(0);
        assertEquals("tool", toolNode.get("role").asText());
        assertEquals("Found 3 results", toolNode.get("content").asText());
        assertEquals("call_abc", toolNode.get("tool_call_id").asText());
    }

    @Test
    public void testParseSimpleResponse() throws Exception {
        String responseJson = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello!\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}";
        ChatResponse response = OpenAiRequestMapper.parseResponse(responseJson);
        assertEquals("Hello!", response.getMessage().getContent());
        assertEquals(FinishReason.STOP, response.getFinishReason());
        assertEquals(15, response.getUsage().getTotalTokens());
    }

    @Test
    public void testParseResponseWithToolCalls() throws Exception {
        String responseJson = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call_123\",\"type\":\"function\",\"function\":{\"name\":\"search\",\"arguments\":\"{\\\"term\\\":\\\"milk\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}],\"usage\":{\"prompt_tokens\":20,\"completion_tokens\":10,\"total_tokens\":30}}";
        ChatResponse response = OpenAiRequestMapper.parseResponse(responseJson);
        assertNotNull(response.getMessage().getToolCalls());
        assertEquals(1, response.getMessage().getToolCalls().size());
        assertEquals("call_123", response.getMessage().getToolCalls().get(0).getId());
        assertEquals("search", response.getMessage().getToolCalls().get(0).getName());
        assertEquals(FinishReason.TOOL_CALLS, response.getFinishReason());
    }
}

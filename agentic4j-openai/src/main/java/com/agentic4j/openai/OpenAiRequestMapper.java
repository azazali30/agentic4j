package com.agentic4j.openai;

import com.agentic4j.core.ChatMessage;
import com.agentic4j.core.ChatRequest;
import com.agentic4j.core.ChatResponse;
import com.agentic4j.core.FinishReason;
import com.agentic4j.core.Role;
import com.agentic4j.core.TokenUsage;
import com.agentic4j.core.ToolCall;
import com.agentic4j.core.ToolDefinition;
import com.agentic4j.core.ToolParameter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class OpenAiRequestMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OpenAiRequestMapper() {
    }

    public static String toJson(ChatRequest request, String model, double temperature,
                                Integer maxTokens, boolean stream) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("model", model);
        root.put("temperature", temperature);

        if (maxTokens != null) {
            root.put("max_tokens", maxTokens.intValue());
        }

        if (stream) {
            root.put("stream", true);
        }

        ArrayNode messagesArray = root.putArray("messages");
        for (ChatMessage message : request.getMessages()) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", message.getRole().name().toLowerCase());

            if (message.getRole() == Role.TOOL) {
                msgNode.put("content", message.getContent());
                msgNode.put("tool_call_id", message.getToolCallId());
            } else if (message.getRole() == Role.ASSISTANT && message.getToolCalls() != null
                    && !message.getToolCalls().isEmpty()) {
                if (message.getContent() != null) {
                    msgNode.put("content", message.getContent());
                } else {
                    msgNode.putNull("content");
                }
                ArrayNode toolCallsArray = msgNode.putArray("tool_calls");
                for (ToolCall toolCall : message.getToolCalls()) {
                    ObjectNode tcNode = toolCallsArray.addObject();
                    tcNode.put("id", toolCall.getId());
                    tcNode.put("type", "function");
                    ObjectNode fnNode = tcNode.putObject("function");
                    fnNode.put("name", toolCall.getName());
                    fnNode.put("arguments", toolCall.getArguments());
                }
            } else {
                msgNode.put("content", message.getContent());
            }
        }

        if (!request.getTools().isEmpty()) {
            ArrayNode toolsArray = root.putArray("tools");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                ObjectNode fnNode = toolNode.putObject("function");
                fnNode.put("name", tool.getName());
                fnNode.put("description", tool.getDescription());

                ObjectNode paramsNode = fnNode.putObject("parameters");
                paramsNode.put("type", "object");
                ObjectNode propsNode = paramsNode.putObject("properties");
                ArrayNode requiredArray = paramsNode.putArray("required");

                for (ToolParameter param : tool.getParameters()) {
                    ObjectNode propNode = propsNode.putObject(param.getName());
                    propNode.put("type", param.getType());
                    propNode.put("description", param.getDescription());
                    if (param.isRequired()) {
                        requiredArray.add(param.getName());
                    }
                }
            }
        }

        return root.toString();
    }

    public static ChatResponse parseResponse(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode choiceNode = root.get("choices").get(0);
            JsonNode messageNode = choiceNode.get("message");

            String role = messageNode.get("role").asText();
            String content = messageNode.has("content") && !messageNode.get("content").isNull()
                    ? messageNode.get("content").asText()
                    : null;

            List<ToolCall> toolCalls = null;
            if (messageNode.has("tool_calls") && messageNode.get("tool_calls").isArray()) {
                toolCalls = new ArrayList<ToolCall>();
                for (JsonNode tcNode : messageNode.get("tool_calls")) {
                    String id = tcNode.get("id").asText();
                    JsonNode fnNode = tcNode.get("function");
                    String name = fnNode.get("name").asText();
                    String arguments = fnNode.get("arguments").asText();
                    toolCalls.add(new ToolCall(id, name, arguments));
                }
            }

            String finishReasonStr = choiceNode.get("finish_reason").asText();
            FinishReason finishReason = parseFinishReason(finishReasonStr);

            TokenUsage usage = null;
            if (root.has("usage")) {
                JsonNode usageNode = root.get("usage");
                int promptTokens = usageNode.get("prompt_tokens").asInt();
                int completionTokens = usageNode.get("completion_tokens").asInt();
                int totalTokens = usageNode.get("total_tokens").asInt();
                usage = new TokenUsage(promptTokens, completionTokens, totalTokens);
            }

            ChatMessage message;
            if (toolCalls != null && !toolCalls.isEmpty()) {
                message = ChatMessage.assistantWithToolCalls(toolCalls);
            } else {
                message = ChatMessage.assistant(content);
            }

            return new ChatResponse(message, usage, finishReason);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    static FinishReason parseFinishReason(String reason) {
        if (reason == null) {
            return null;
        }
        if ("stop".equals(reason)) {
            return FinishReason.STOP;
        } else if ("tool_calls".equals(reason)) {
            return FinishReason.TOOL_CALLS;
        } else if ("length".equals(reason)) {
            return FinishReason.LENGTH;
        } else if ("content_filter".equals(reason)) {
            return FinishReason.CONTENT_FILTER;
        }
        return null;
    }
}

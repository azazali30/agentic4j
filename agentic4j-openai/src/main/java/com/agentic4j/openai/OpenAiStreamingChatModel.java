package com.agentic4j.openai;

import com.agentic4j.core.ChatMessage;
import com.agentic4j.core.ChatRequest;
import com.agentic4j.core.ChatResponse;
import com.agentic4j.core.FinishReason;
import com.agentic4j.core.StreamingChatModel;
import com.agentic4j.core.StreamingResponseHandler;
import com.agentic4j.core.TokenUsage;
import com.agentic4j.core.ToolCall;
import com.agentic4j.core.exception.ModelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenAiStreamingChatModel implements StreamingChatModel {

    private static final Logger LOG = Logger.getLogger(OpenAiStreamingChatModel.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final double temperature;
    private final Integer maxTokens;
    private final OkHttpClient httpClient;
    private final boolean logRequests;
    private final boolean logResponses;

    private OpenAiStreamingChatModel(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(builder.timeout, TimeUnit.SECONDS)
                .readTimeout(builder.timeout, TimeUnit.SECONDS)
                .writeTimeout(builder.timeout, TimeUnit.SECONDS)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void send(ChatRequest request, StreamingResponseHandler handler) {
        String jsonBody = OpenAiRequestMapper.toJson(request, modelName, temperature, maxTokens, true);

        if (logRequests) {
            LOG.info("OpenAI streaming request: " + jsonBody);
        }

        String url = baseUrl + "/chat/completions";
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonBody);
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        Response response = null;
        try {
            response = httpClient.newCall(httpRequest).execute();

            if (!response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                String responseString = responseBody != null ? responseBody.string() : "";
                handler.onError(new ModelException(
                        response.code(),
                        "OpenAI API error: " + response.code() + " " + response.message(),
                        responseString
                ));
                return;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                handler.onError(new RuntimeException("Empty response body from OpenAI API"));
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream(), "UTF-8"));
            StringBuilder contentBuilder = new StringBuilder();
            TreeMap<Integer, ToolCallAccumulator> toolCallAccumulators = new TreeMap<Integer, ToolCallAccumulator>();
            FinishReason finishReason = null;
            TokenUsage tokenUsage = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }

                if (logResponses) {
                    LOG.info("OpenAI streaming chunk: " + data);
                }

                JsonNode chunk = OBJECT_MAPPER.readTree(data);
                JsonNode choicesNode = chunk.get("choices");
                if (choicesNode != null && choicesNode.size() > 0) {
                    JsonNode choice = choicesNode.get(0);
                    JsonNode deltaNode = choice.get("delta");

                    if (deltaNode != null) {
                        // Handle content
                        if (deltaNode.has("content") && !deltaNode.get("content").isNull()) {
                            String content = deltaNode.get("content").asText();
                            contentBuilder.append(content);
                            handler.onToken(content);
                        }

                        // Handle tool_calls
                        if (deltaNode.has("tool_calls") && deltaNode.get("tool_calls").isArray()) {
                            for (JsonNode tcDelta : deltaNode.get("tool_calls")) {
                                int index = tcDelta.get("index").asInt();
                                ToolCallAccumulator accumulator = toolCallAccumulators.get(index);
                                if (accumulator == null) {
                                    accumulator = new ToolCallAccumulator();
                                    toolCallAccumulators.put(index, accumulator);
                                }
                                if (tcDelta.has("id")) {
                                    accumulator.id = tcDelta.get("id").asText();
                                }
                                if (tcDelta.has("function")) {
                                    JsonNode fnNode = tcDelta.get("function");
                                    if (fnNode.has("name")) {
                                        accumulator.name = fnNode.get("name").asText();
                                    }
                                    if (fnNode.has("arguments")) {
                                        accumulator.arguments.append(fnNode.get("arguments").asText());
                                    }
                                }
                            }
                        }
                    }

                    // Handle finish_reason
                    if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                        finishReason = OpenAiRequestMapper.parseFinishReason(choice.get("finish_reason").asText());
                    }
                }

                // Handle usage
                if (chunk.has("usage") && !chunk.get("usage").isNull()) {
                    JsonNode usageNode = chunk.get("usage");
                    int promptTokens = usageNode.get("prompt_tokens").asInt();
                    int completionTokens = usageNode.get("completion_tokens").asInt();
                    int totalTokens = usageNode.get("total_tokens").asInt();
                    tokenUsage = new TokenUsage(promptTokens, completionTokens, totalTokens);
                }
            }

            // Build final message
            ChatMessage message;
            if (!toolCallAccumulators.isEmpty()) {
                List<ToolCall> toolCalls = new ArrayList<ToolCall>();
                for (ToolCallAccumulator accumulator : toolCallAccumulators.values()) {
                    toolCalls.add(new ToolCall(accumulator.id, accumulator.name, accumulator.arguments.toString()));
                }
                message = ChatMessage.assistantWithToolCalls(toolCalls);
            } else {
                message = ChatMessage.assistant(contentBuilder.toString());
            }

            ChatResponse chatResponse = new ChatResponse(message, tokenUsage, finishReason);
            handler.onComplete(chatResponse);

        } catch (Exception e) {
            handler.onError(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private static class ToolCallAccumulator {
        String id;
        String name;
        final StringBuilder arguments = new StringBuilder();
    }

    public static class Builder {

        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "gpt-4o-mini";
        private double temperature = 0.7;
        private Integer maxTokens;
        private long timeout = 60;
        private boolean logRequests;
        private boolean logResponses;

        private Builder() {
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            if (baseUrl != null && baseUrl.endsWith("/")) {
                this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            } else {
                this.baseUrl = baseUrl;
            }
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiStreamingChatModel build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey must not be null or empty");
            }
            return new OpenAiStreamingChatModel(this);
        }
    }
}

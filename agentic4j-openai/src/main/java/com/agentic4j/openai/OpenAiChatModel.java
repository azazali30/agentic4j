package com.agentic4j.openai;

import com.agentic4j.core.ChatModel;
import com.agentic4j.core.ChatRequest;
import com.agentic4j.core.ChatResponse;
import com.agentic4j.core.exception.ModelException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenAiChatModel implements ChatModel {

    private static final Logger LOG = Logger.getLogger(OpenAiChatModel.class.getName());
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final double temperature;
    private final Integer maxTokens;
    private final OkHttpClient httpClient;
    private final boolean logRequests;
    private final boolean logResponses;

    private OpenAiChatModel(Builder builder) {
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
    public ChatResponse send(ChatRequest request) {
        String jsonBody = OpenAiRequestMapper.toJson(request, modelName, temperature, maxTokens, false);

        if (logRequests) {
            LOG.info("OpenAI request: " + jsonBody);
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
            ResponseBody responseBody = response.body();
            String responseString = responseBody != null ? responseBody.string() : "";

            if (logResponses) {
                LOG.info("OpenAI response: " + responseString);
            }

            if (!response.isSuccessful()) {
                throw new ModelException(
                        response.code(),
                        "OpenAI API error: " + response.code() + " " + response.message(),
                        responseString
                );
            }

            return OpenAiRequestMapper.parseResponse(responseString);
        } catch (ModelException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
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
            // Remove trailing slash if present
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

        public OpenAiChatModel build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey must not be null or empty");
            }
            return new OpenAiChatModel(this);
        }
    }
}

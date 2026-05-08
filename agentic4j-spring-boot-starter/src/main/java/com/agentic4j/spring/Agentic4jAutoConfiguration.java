package com.agentic4j.spring;

import com.agentic4j.core.ChatModel;
import com.agentic4j.openai.OpenAiChatModel;
import com.agentic4j.openai.OpenAiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties(Agentic4jProperties.class)
public class Agentic4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("agentic4j.openai.api-key")
    public OpenAiChatModel openAiChatModel(Agentic4jProperties props) {
        Agentic4jProperties.OpenAiProperties openai = props.getOpenai();
        OpenAiChatModel.Builder builder = OpenAiChatModel.builder()
                .apiKey(openai.getApiKey())
                .baseUrl(openai.getBaseUrl())
                .modelName(openai.getModelName())
                .temperature(openai.getTemperature())
                .timeout(openai.getTimeout())
                .logRequests(openai.isLogRequests())
                .logResponses(openai.isLogResponses());
        if (openai.getMaxTokens() != null) {
            builder.maxTokens(openai.getMaxTokens());
        }
        if (openai.getMaxCompletionTokens() != null) {
            builder.maxCompletionTokens(openai.getMaxCompletionTokens());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("agentic4j.openai.api-key")
    public OpenAiStreamingChatModel openAiStreamingChatModel(Agentic4jProperties props) {
        Agentic4jProperties.OpenAiProperties openai = props.getOpenai();
        OpenAiStreamingChatModel.Builder builder = OpenAiStreamingChatModel.builder()
                .apiKey(openai.getApiKey())
                .baseUrl(openai.getBaseUrl())
                .modelName(openai.getModelName())
                .temperature(openai.getTemperature())
                .timeout(openai.getTimeout())
                .logRequests(openai.isLogRequests())
                .logResponses(openai.isLogResponses());
        if (openai.getMaxTokens() != null) {
            builder.maxTokens(openai.getMaxTokens());
        }
        if (openai.getMaxCompletionTokens() != null) {
            builder.maxCompletionTokens(openai.getMaxCompletionTokens());
        }
        return builder.build();
    }
}

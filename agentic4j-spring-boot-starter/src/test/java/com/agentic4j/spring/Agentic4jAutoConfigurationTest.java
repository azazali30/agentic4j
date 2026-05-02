package com.agentic4j.spring;

import com.agentic4j.openai.OpenAiChatModel;
import com.agentic4j.openai.OpenAiStreamingChatModel;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.Assert.*;

public class Agentic4jAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Agentic4jAutoConfiguration.class));

    @Test
    public void testBeansCreatedWithApiKey() {
        contextRunner
            .withPropertyValues("agentic4j.openai.api-key=test-key", "agentic4j.openai.model-name=gpt-4o-mini")
            .run(context -> {
                assertTrue(context.containsBean("openAiChatModel"));
                assertTrue(context.containsBean("openAiStreamingChatModel"));
                assertNotNull(context.getBean(OpenAiChatModel.class));
                assertNotNull(context.getBean(OpenAiStreamingChatModel.class));
            });
    }

    @Test
    public void testNoBeansWithoutApiKey() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("openAiChatModel"));
            assertFalse(context.containsBean("openAiStreamingChatModel"));
        });
    }

    @Test
    public void testCustomBaseUrl() {
        contextRunner
            .withPropertyValues("agentic4j.openai.api-key=test-key", "agentic4j.openai.base-url=https://openrouter.ai/api/v1", "agentic4j.openai.model-name=openai/gpt-4o-mini", "agentic4j.openai.temperature=0.0")
            .run(context -> assertTrue(context.containsBean("openAiChatModel")));
    }
}

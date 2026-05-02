package com.agentic4j.core.agent;

import com.agentic4j.core.ChatMessage;
import com.agentic4j.core.ChatModel;
import com.agentic4j.core.ChatRequest;
import com.agentic4j.core.ChatResponse;
import com.agentic4j.core.FinishReason;
import com.agentic4j.core.ToolCall;
import com.agentic4j.core.ToolDefinition;
import com.agentic4j.core.ToolExecutionResult;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.exception.MaxIterationsException;
import com.agentic4j.core.memory.ChatMemory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

public class AgentInvocationHandler implements InvocationHandler {

    private final ChatModel chatModel;
    private final ChatMemory memory;
    private final ToolExecutor toolExecutor;
    private final List<ToolDefinition> toolDefinitions;
    private final int maxToolIterations;

    public AgentInvocationHandler(ChatModel chatModel, ChatMemory memory,
                                   ToolExecutor toolExecutor, List<ToolDefinition> toolDefinitions,
                                   int maxToolIterations) {
        this.chatModel = chatModel;
        this.memory = memory;
        this.toolExecutor = toolExecutor;
        this.toolDefinitions = toolDefinitions;
        this.maxToolIterations = maxToolIterations;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        String userMessage = (String) args[0];
        String systemPrompt = resolveSystemPrompt(method);

        memory.add(ChatMessage.user(userMessage));

        for (int iteration = 0; iteration < maxToolIterations; iteration++) {
            ChatRequest.Builder builder = ChatRequest.builder();

            if (systemPrompt != null) {
                builder.addMessage(ChatMessage.system(systemPrompt));
            }

            builder.addMessages(memory.messages());
            builder.addTools(toolDefinitions);

            ChatRequest request = builder.build();
            ChatResponse response = chatModel.send(request);
            ChatMessage assistantMessage = response.getMessage();

            if (response.getFinishReason() == FinishReason.TOOL_CALLS
                    && assistantMessage.getToolCalls() != null
                    && !assistantMessage.getToolCalls().isEmpty()) {

                memory.add(assistantMessage);

                for (ToolCall toolCall : assistantMessage.getToolCalls()) {
                    ToolExecutionResult result = toolExecutor.execute(toolCall);
                    memory.add(ChatMessage.toolResult(toolCall.getId(), result.getResult()));
                }
            } else {
                memory.add(assistantMessage);
                return assistantMessage.getContent();
            }
        }

        throw new MaxIterationsException(maxToolIterations);
    }

    static String resolveSystemPrompt(Method method) {
        SystemPrompt annotation = method.getAnnotation(SystemPrompt.class);
        if (annotation == null) {
            return null;
        }

        String fromResource = annotation.fromResource();
        if (fromResource != null && !fromResource.isEmpty()) {
            return loadResource(fromResource);
        }

        String value = annotation.value();
        if (value != null && !value.isEmpty()) {
            return value;
        }

        return null;
    }

    private static String loadResource(String resourcePath) {
        InputStream is = AgentInvocationHandler.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resourcePath, e);
        }
    }
}

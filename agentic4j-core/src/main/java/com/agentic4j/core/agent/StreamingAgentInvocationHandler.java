package com.agentic4j.core.agent;

import com.agentic4j.core.StreamingChatModel;
import com.agentic4j.core.ToolDefinition;
import com.agentic4j.core.memory.ChatMemory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

public class StreamingAgentInvocationHandler implements InvocationHandler {

    private final StreamingChatModel streamingChatModel;
    private final ChatMemory memory;
    private final ToolExecutor toolExecutor;
    private final List<ToolDefinition> toolDefinitions;
    private final int maxToolIterations;

    public StreamingAgentInvocationHandler(StreamingChatModel streamingChatModel,
                                            ChatMemory memory,
                                            ToolExecutor toolExecutor,
                                            List<ToolDefinition> toolDefinitions,
                                            int maxToolIterations) {
        this.streamingChatModel = streamingChatModel;
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
        String systemPrompt = AgentInvocationHandler.resolveSystemPrompt(method);

        return new DefaultStreamingResponse(
                streamingChatModel,
                memory,
                toolExecutor,
                toolDefinitions,
                maxToolIterations,
                userMessage,
                systemPrompt
        );
    }
}

package com.agentic4j.core.agent;

import com.agentic4j.core.ChatModel;
import com.agentic4j.core.StreamingChatModel;
import com.agentic4j.core.StreamingResponse;
import com.agentic4j.core.ToolDefinition;
import com.agentic4j.core.memory.ChatMemory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class AgentBuilder<T> {

    private final Class<T> interfaceClass;
    private ChatModel chatModel;
    private StreamingChatModel streamingChatModel;
    private ChatMemory memory;
    private Object[] toolInstances = new Object[0];
    private int maxToolIterations = 10;

    private AgentBuilder(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public static <T> AgentBuilder<T> forInterface(Class<T> interfaceClass) {
        return new AgentBuilder<T>(interfaceClass);
    }

    public AgentBuilder<T> chatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        return this;
    }

    public AgentBuilder<T> streamingChatModel(StreamingChatModel streamingChatModel) {
        this.streamingChatModel = streamingChatModel;
        return this;
    }

    public AgentBuilder<T> memory(ChatMemory memory) {
        this.memory = memory;
        return this;
    }

    public AgentBuilder<T> tools(Object... toolInstances) {
        this.toolInstances = toolInstances;
        return this;
    }

    public AgentBuilder<T> maxToolIterations(int maxToolIterations) {
        this.maxToolIterations = maxToolIterations;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T build() {
        List<ToolDefinition> toolDefinitions = ToolScanner.scan(toolInstances);
        ToolExecutor toolExecutor = new ToolExecutor(toolInstances);

        InvocationHandler handler;
        if (streamingChatModel != null && hasStreamingReturnType()) {
            handler = new StreamingAgentInvocationHandler(
                    streamingChatModel, memory, toolExecutor, toolDefinitions, maxToolIterations);
        } else if (chatModel != null) {
            handler = new AgentInvocationHandler(
                    chatModel, memory, toolExecutor, toolDefinitions, maxToolIterations);
        } else {
            throw new IllegalStateException("Either chatModel or streamingChatModel must be provided");
        }

        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                handler);
    }

    private boolean hasStreamingReturnType() {
        for (Method method : interfaceClass.getMethods()) {
            if (StreamingResponse.class.isAssignableFrom(method.getReturnType())) {
                return true;
            }
        }
        return false;
    }
}

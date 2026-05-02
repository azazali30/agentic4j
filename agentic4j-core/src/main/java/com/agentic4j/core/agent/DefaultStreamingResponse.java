package com.agentic4j.core.agent;

import com.agentic4j.core.ChatMessage;
import com.agentic4j.core.ChatRequest;
import com.agentic4j.core.ChatResponse;
import com.agentic4j.core.FinishReason;
import com.agentic4j.core.StreamingChatModel;
import com.agentic4j.core.StreamingResponse;
import com.agentic4j.core.StreamingResponseHandler;
import com.agentic4j.core.ToolCall;
import com.agentic4j.core.ToolDefinition;
import com.agentic4j.core.ToolExecutionResult;
import com.agentic4j.core.exception.MaxIterationsException;
import com.agentic4j.core.memory.ChatMemory;

import java.util.List;
import java.util.function.Consumer;

public class DefaultStreamingResponse implements StreamingResponse {

    private final StreamingChatModel streamingChatModel;
    private final ChatMemory memory;
    private final ToolExecutor toolExecutor;
    private final List<ToolDefinition> toolDefinitions;
    private final int maxToolIterations;
    private final String userMessage;
    private final String systemPrompt;

    private Consumer<String> tokenHandler;
    private Consumer<ToolExecutionResult> toolExecutedHandler;
    private Consumer<ChatResponse> completeHandler;
    private Consumer<Throwable> errorHandler;

    public DefaultStreamingResponse(StreamingChatModel streamingChatModel,
                                     ChatMemory memory,
                                     ToolExecutor toolExecutor,
                                     List<ToolDefinition> toolDefinitions,
                                     int maxToolIterations,
                                     String userMessage,
                                     String systemPrompt) {
        this.streamingChatModel = streamingChatModel;
        this.memory = memory;
        this.toolExecutor = toolExecutor;
        this.toolDefinitions = toolDefinitions;
        this.maxToolIterations = maxToolIterations;
        this.userMessage = userMessage;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public StreamingResponse onToken(Consumer<String> handler) {
        this.tokenHandler = handler;
        return this;
    }

    @Override
    public StreamingResponse onToolExecuted(Consumer<ToolExecutionResult> handler) {
        this.toolExecutedHandler = handler;
        return this;
    }

    @Override
    public StreamingResponse onComplete(Consumer<ChatResponse> handler) {
        this.completeHandler = handler;
        return this;
    }

    @Override
    public StreamingResponse onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
        return this;
    }

    @Override
    public void start() {
        memory.add(ChatMessage.user(userMessage));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendRequest(0);
            }
        }, "agentic4j-streaming");
        thread.setDaemon(true);
        thread.start();
    }

    private void sendRequest(int iteration) {
        if (iteration >= maxToolIterations) {
            if (errorHandler != null) {
                errorHandler.accept(new MaxIterationsException(maxToolIterations));
            }
            return;
        }

        ChatRequest.Builder builder = ChatRequest.builder();

        if (systemPrompt != null) {
            builder.addMessage(ChatMessage.system(systemPrompt));
        }

        builder.addMessages(memory.messages());
        builder.addTools(toolDefinitions);

        ChatRequest request = builder.build();

        final int currentIteration = iteration;

        streamingChatModel.send(request, new StreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                if (tokenHandler != null) {
                    tokenHandler.accept(token);
                }
            }

            @Override
            public void onToolExecution(ToolExecutionResult result) {
                if (toolExecutedHandler != null) {
                    toolExecutedHandler.accept(result);
                }
            }

            @Override
            public void onComplete(ChatResponse response) {
                ChatMessage assistantMessage = response.getMessage();

                if (response.getFinishReason() == FinishReason.TOOL_CALLS
                        && assistantMessage.getToolCalls() != null
                        && !assistantMessage.getToolCalls().isEmpty()) {

                    memory.add(assistantMessage);

                    for (ToolCall toolCall : assistantMessage.getToolCalls()) {
                        ToolExecutionResult result = toolExecutor.execute(toolCall);
                        memory.add(ChatMessage.toolResult(toolCall.getId(), result.getResult()));

                        if (toolExecutedHandler != null) {
                            toolExecutedHandler.accept(result);
                        }
                    }

                    sendRequest(currentIteration + 1);
                } else {
                    memory.add(assistantMessage);
                    if (completeHandler != null) {
                        completeHandler.accept(response);
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                if (errorHandler != null) {
                    errorHandler.accept(error);
                }
            }
        });
    }
}

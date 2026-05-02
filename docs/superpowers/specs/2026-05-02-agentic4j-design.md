# agentic4j Design Spec

A lightweight, Java 8 compatible AI agent framework — inspired by LangChain4j but with its own clean API. Built to support the exact feature set used by the price-agent project, with a pluggable architecture for future extension.

## Goals

- Java 8 compatible (client constraint: no Java 17 on production servers)
- Drop-in conceptual replacement for LangChain4j's AiServices, tool annotations, memory, and streaming
- Clean-room API design (own naming, own patterns — not a copy of LangChain4j)
- Provider-agnostic core with pluggable model interface
- Ships with OpenAI-compatible provider (covers OpenAI, OpenRouter, Azure OpenAI, Ollama)
- Optional Spring Boot 2.7.x starter for auto-configuration
- Open source friendly

## Non-Goals

- RAG, embeddings, document loaders, vector stores
- Prompt-based tool calling fallback (requires native function calling from the LLM)
- Automatic retries or circuit breakers (users add their own)
- Chains or workflows beyond the ReAct loop

---

## Module Structure

```
agentic4j/
├── agentic4j-core/                        # Zero external dependencies, Java 8
│   └── Interfaces, annotations, memory, agent builder, ReAct loop
├── agentic4j-openai/                      # OkHttp 3.x + Jackson
│   └── OpenAI-compatible sync + streaming chat model
└── agentic4j-spring-boot-starter/         # Spring Boot 2.7.x auto-config
    └── Auto-wires models from application.properties
```

**Dependency flow:**

- `agentic4j-core` → JDK 8 only
- `agentic4j-openai` → `agentic4j-core` + `okhttp3` + `jackson-databind`
- `agentic4j-spring-boot-starter` → `agentic4j-core` + Spring Boot 2.7.x

Users pull `agentic4j-openai` (transitively gets `core`) and optionally the Spring starter.

---

## Core API

### Chat Model Interfaces

```java
public interface ChatModel {
    ChatResponse send(ChatRequest request);
}

public interface StreamingChatModel {
    void send(ChatRequest request, StreamingResponseHandler handler);
}

public interface StreamingResponseHandler {
    void onToken(String token);
    void onToolExecution(ToolExecutionResult result);
    void onComplete(ChatResponse response);
    void onError(Throwable error);
}
```

### Chat Messages

```java
public class ChatMessage {
    private final Role role;    // SYSTEM, USER, ASSISTANT, TOOL
    private final String content;
    // + optional: List<ToolCall> toolCalls, String toolCallId
}

public enum Role { SYSTEM, USER, ASSISTANT, TOOL }

public class ChatRequest {
    private final List<ChatMessage> messages;
    private final List<ToolDefinition> tools;
    // builder pattern
}

public class ChatResponse {
    private final ChatMessage message;
    private final TokenUsage usage;
    private final FinishReason finishReason;
}

public enum FinishReason { STOP, TOOL_CALLS, LENGTH, CONTENT_FILTER }

public class TokenUsage {
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;
}
```

### Tool Definitions

```java
public class ToolDefinition {
    private final String name;
    private final String description;
    private final Map<String, ToolParameter> parameters;
}

public class ToolParameter {
    private final String name;
    private final String description;
    private final String type;       // "string", "integer", "number", "boolean"
    private final boolean required;
}

public class ToolCall {
    private final String id;
    private final String name;
    private final String arguments;  // JSON string
}

public class ToolExecutionResult {
    private final String toolCallId;
    private final String toolName;
    private final String result;
}
```

### Annotations

```java
@Retention(RUNTIME)
@Target(METHOD)
public @interface AgentTool {
    String value();   // tool description
}

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Param {
    String value();   // parameter description
}

@Retention(RUNTIME)
@Target(METHOD)
public @interface SystemPrompt {
    String value() default "";
    String fromResource() default "";   // load from classpath resource
}
```

### Chat Memory

```java
public interface ChatMemory {
    void add(ChatMessage message);
    List<ChatMessage> messages();
    void clear();
}

public class SlidingWindowMemory implements ChatMemory {
    public SlidingWindowMemory(int maxMessages) { ... }
}
```

### Streaming Response

```java
public interface StreamingResponse {
    StreamingResponse onToken(Consumer<String> handler);
    StreamingResponse onToolExecuted(Consumer<ToolExecutionResult> handler);
    StreamingResponse onComplete(Consumer<ChatResponse> handler);
    StreamingResponse onError(Consumer<Throwable> handler);
    void start();
}
```

`java.util.function.Consumer` is available in Java 8 natively.

---

## Agent Builder

The core of the framework. Takes a user-defined interface and creates a dynamic proxy at runtime.

```java
public class AgentBuilder<T> {
    public static <T> AgentBuilder<T> forInterface(Class<T> interfaceClass) { ... }

    public AgentBuilder<T> chatModel(ChatModel model) { ... }
    public AgentBuilder<T> streamingChatModel(StreamingChatModel model) { ... }
    public AgentBuilder<T> memory(ChatMemory memory) { ... }
    public AgentBuilder<T> tools(Object... toolInstances) { ... }
    public AgentBuilder<T> maxToolIterations(int max) { ... }  // default: 10

    public T build() { ... }
}
```

### Usage

```java
public interface PriceAssistant {
    @SystemPrompt(fromResource = "system-prompt.txt")
    String chat(String userMessage);
}

public interface StreamingPriceAssistant {
    @SystemPrompt(fromResource = "system-prompt.txt")
    StreamingResponse chat(String userMessage);
}

PriceAssistant agent = AgentBuilder.forInterface(PriceAssistant.class)
    .chatModel(chatModel)
    .memory(new SlidingWindowMemory(20))
    .tools(new ExecuteSqlTool(db), new GetDimensionValuesTool(db))
    .build();

String answer = agent.chat("What's the price of milk in Dubai?");
```

### Return type conventions

- Method returns `String` → sync mode, uses `ChatModel`, returns final text
- Method returns `StreamingResponse` → streaming mode, uses `StreamingChatModel`

### ReAct Loop (internal)

```
1. Build ChatRequest: system prompt + memory messages + tool definitions
2. Send to ChatModel
3. If response has tool calls:
   a. For each tool call → find matching @AgentTool method → invoke via reflection
   b. Add assistant message (with tool calls) + tool result messages to memory
   c. Go to step 2
4. If response is plain text → add to memory → return to caller
5. Safety: abort after maxToolIterations (default 10) → throw MaxIterationsException
```

### Tool scanning

At `build()` time:

1. Scan all `tools(...)` instances for methods annotated with `@AgentTool`
2. For each tool method, read `@Param` annotations on parameters
3. Infer JSON Schema types from Java types (String→"string", int/Integer→"integer", double/Double→"number", boolean/Boolean→"boolean")
4. Generate `ToolDefinition` objects to include in `ChatRequest`
5. When the LLM returns a `ToolCall`, deserialize arguments JSON and invoke the method via reflection

---

## OpenAI Provider Module

### OpenAiChatModel

```java
public class OpenAiChatModel implements ChatModel {
    public static Builder builder() { ... }

    public static class Builder {
        public Builder apiKey(String apiKey) { ... }
        public Builder baseUrl(String baseUrl) { ... }      // default: https://api.openai.com/v1
        public Builder modelName(String model) { ... }       // default: gpt-4o-mini
        public Builder temperature(double temp) { ... }
        public Builder maxTokens(int max) { ... }
        public Builder timeout(Duration timeout) { ... }     // default: 60s
        public Builder logRequests(boolean log) { ... }
        public Builder logResponses(boolean log) { ... }
        public OpenAiChatModel build() { ... }
    }

    @Override
    public ChatResponse send(ChatRequest request) { ... }
}
```

Implementation: maps `ChatRequest` → OpenAI JSON, POSTs to `{baseUrl}/chat/completions` via OkHttp, parses response JSON → `ChatResponse`.

### OpenAiStreamingChatModel

Same builder pattern. Implementation: POSTs with `stream: true`, parses SSE `data:` lines as they arrive, calls `StreamingResponseHandler` callbacks, accumulates tool call deltas.

### OpenAI API Mapping

| agentic4j | OpenAI API |
|---|---|
| `ChatMessage(SYSTEM, text)` | `{"role": "system", "content": "..."}` |
| `ChatMessage(USER, text)` | `{"role": "user", "content": "..."}` |
| `ChatMessage(ASSISTANT, text, toolCalls)` | `{"role": "assistant", "content": "...", "tool_calls": [...]}` |
| `ChatMessage(TOOL, result, toolCallId)` | `{"role": "tool", "content": "...", "tool_call_id": "..."}` |
| `ToolDefinition` | `{"type": "function", "function": {"name", "description", "parameters"}}` |

### JSON Schema Generation

`@AgentTool` + `@Param` annotations are converted to the `parameters` JSON Schema at build time via reflection. No annotation processors or code generation.

---

## Spring Boot Starter Module

Targets **Spring Boot 2.7.x** (last Java 8 compatible version).

### Auto-Configuration

```java
@Configuration
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties(Agentic4jProperties.class)
public class Agentic4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("agentic4j.openai.api-key")
    public OpenAiChatModel openAiChatModel(Agentic4jProperties props) { ... }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("agentic4j.openai.api-key")
    public OpenAiStreamingChatModel openAiStreamingChatModel(Agentic4jProperties props) { ... }
}
```

### Properties

```properties
agentic4j.openai.api-key=${OPENAI_API_KEY}
agentic4j.openai.base-url=https://openrouter.ai/api/v1
agentic4j.openai.model-name=openai/gpt-4o-mini
agentic4j.openai.temperature=0.0
```

### Scope

The starter auto-wires model beans only. It does NOT auto-create agents — users still call `AgentBuilder.forInterface()` themselves, because agent interfaces and tools are application-specific.

### Registration

Uses `META-INF/spring.factories` for Spring Boot 2.7.x auto-configuration discovery.

---

## Error Handling

### Exception Hierarchy

```java
public class Agentic4jException extends RuntimeException { ... }
public class ModelException extends Agentic4jException { ... }          // LLM errors (auth, rate limit, invalid request)
public class ToolExecutionException extends Agentic4jException { ... }  // @AgentTool method threw
public class MaxIterationsException extends Agentic4jException { ... }  // ReAct loop exhausted
```

### Tool Error Behavior

When an `@AgentTool` method throws, the exception message is sent back to the LLM as the tool result (prefixed with `"Error: "`), giving it a chance to recover. Only if `maxToolIterations` is exhausted does it throw `MaxIterationsException` to the caller.

### Model Error Behavior

HTTP errors (401, 429, 500) are wrapped in `ModelException` with status code and response body accessible. No automatic retries.

---

## Testing Strategy

| Layer | Approach |
|---|---|
| Core (memory, annotations, tool scanning) | Unit tests — pure Java, no mocks |
| Agent proxy + ReAct loop | Unit tests with mock `ChatModel` returning scripted responses |
| OpenAI provider (JSON mapping) | Unit tests against recorded JSON fixtures |
| OpenAI provider (HTTP) | Integration tests with real endpoint (skipped in CI, manual) |
| Spring starter | `@SpringBootTest` with test properties |

### Test Dependencies

- JUnit 4 (Java 8 compatible)
- Mockito 2.x
- OkHttp MockWebServer (HTTP fixture tests)

---

## LangChain4j → agentic4j Migration Mapping

| LangChain4j | agentic4j |
|---|---|
| `AiServices.builder(X.class)` | `AgentBuilder.forInterface(X.class)` |
| `.chatLanguageModel(m)` | `.chatModel(m)` |
| `.streamingChatLanguageModel(m)` | `.streamingChatModel(m)` |
| `.chatMemory(MessageWindowChatMemory.withMaxMessages(n))` | `.memory(new SlidingWindowMemory(n))` |
| `.tools(...)` | `.tools(...)` |
| `@Tool("desc")` | `@AgentTool("desc")` |
| `@P("desc")` | `@Param("desc")` |
| `@SystemMessage(fromResource = "x.txt")` | `@SystemPrompt(fromResource = "x.txt")` |
| `TokenStream` | `StreamingResponse` |
| `tokenStream.onNext(...)` | `streamingResponse.onToken(...)` |
| `tokenStream.onToolExecuted(...)` | `streamingResponse.onToolExecuted(...)` |
| `tokenStream.onComplete(...)` | `streamingResponse.onComplete(...)` |
| `tokenStream.onError(...)` | `streamingResponse.onError(...)` |
| `OpenAiChatModel` | `OpenAiChatModel` (same name, different package) |
| `OpenAiStreamingChatModel` | `OpenAiStreamingChatModel` (same name, different package) |

---

## Key Dependencies and Versions

| Dependency | Module | Version | Purpose |
|---|---|---|---|
| JDK | all | 8+ | Language level |
| OkHttp | openai | 3.14.x | HTTP client |
| Jackson Databind | openai | 2.13.x | JSON serialization |
| Spring Boot | starter | 2.7.x | Auto-configuration |
| JUnit | test | 4.13.x | Testing |
| Mockito | test | 2.x | Mocking |
| OkHttp MockWebServer | test | 3.14.x | HTTP fixture tests |

---

## Package Structure

```
com.agentic4j.core
├── ChatModel
├── StreamingChatModel
├── StreamingResponseHandler
├── ChatMessage
├── ChatRequest
├── ChatResponse
├── Role
├── FinishReason
├── TokenUsage
├── ToolDefinition
├── ToolParameter
├── ToolCall
├── ToolExecutionResult
├── StreamingResponse
├── annotation
│   ├── AgentTool
│   ├── Param
│   └── SystemPrompt
├── memory
│   ├── ChatMemory
│   └── SlidingWindowMemory
├── agent
│   └── AgentBuilder
└── exception
    ├── Agentic4jException
    ├── ModelException
    ├── ToolExecutionException
    └── MaxIterationsException

com.agentic4j.openai
├── OpenAiChatModel
└── OpenAiStreamingChatModel

com.agentic4j.spring
├── Agentic4jAutoConfiguration
└── Agentic4jProperties
```

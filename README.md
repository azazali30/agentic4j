# agentic4j

A lightweight, Java 8 compatible AI agent framework with a pluggable model interface.

## Features

- **Java 8 compatible** - runs on legacy JVMs where modern frameworks can't
- **Annotation-driven agents** - define agent interfaces with `@SystemPrompt`, register tools with `@AgentTool`
- **ReAct loop built-in** - automatic tool calling and reasoning loop via dynamic proxies
- **Sync and streaming** - both blocking and callback-based streaming responses
- **Pluggable models** - implement `ChatModel` or `StreamingChatModel` for any provider
- **Sliding window memory** - configurable chat history per session
- **OpenAI-compatible provider** - works with OpenAI, OpenRouter, Azure OpenAI, Ollama, and any OpenAI-compatible API
- **Spring Boot starter** - optional auto-configuration for Spring Boot 2.7.x

## Modules

| Module | Description |
|---|---|
| `agentic4j-core` | Interfaces, annotations, memory, agent builder (zero external dependencies) |
| `agentic4j-openai` | OpenAI-compatible provider using OkHttp 3.x + Jackson |
| `agentic4j-spring-boot-starter` | Spring Boot 2.7.x auto-configuration |

## Quick Start

### Maven

```xml
<dependency>
    <groupId>com.agentic4j</groupId>
    <artifactId>agentic4j-openai</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Define an Agent

```java
public interface MyAssistant {
    @SystemPrompt("You are a helpful assistant.")
    String chat(String message);
}
```

### Create Tools

```java
public class SearchTool {
    @AgentTool("Search for products by name")
    public String search(@Param("The search term") String term) {
        // your implementation
        return "Found: item1, item2";
    }
}
```

### Build and Use

```java
OpenAiChatModel model = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-4o-mini")
    .build();

MyAssistant assistant = AgentBuilder.forInterface(MyAssistant.class)
    .chatModel(model)
    .memory(new SlidingWindowMemory(20))
    .tools(new SearchTool())
    .build();

String reply = assistant.chat("Find me some products");
```

### Streaming

```java
public interface StreamingAssistant {
    @SystemPrompt("You are a helpful assistant.")
    StreamingResponse chat(String message);
}

OpenAiStreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-4o-mini")
    .build();

StreamingAssistant assistant = AgentBuilder.forInterface(StreamingAssistant.class)
    .streamingChatModel(streamingModel)
    .memory(new SlidingWindowMemory(20))
    .tools(new SearchTool())
    .build();

assistant.chat("Find me some products")
    .onToken(token -> System.out.print(token))
    .onComplete(response -> System.out.println("\nDone!"))
    .onError(error -> error.printStackTrace())
    .start();
```

### Spring Boot

Add the starter dependency:

```xml
<dependency>
    <groupId>com.agentic4j</groupId>
    <artifactId>agentic4j-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Configure in `application.properties`:

```properties
agentic4j.openai.api-key=${OPENAI_API_KEY}
agentic4j.openai.model-name=gpt-4o-mini
agentic4j.openai.base-url=https://api.openai.com/v1
agentic4j.openai.temperature=0.0
```

Then inject the auto-configured model:

```java
@Component
public class MyAgentConfig {
    @Bean
    public MyAssistant myAssistant(OpenAiChatModel model) {
        return AgentBuilder.forInterface(MyAssistant.class)
            .chatModel(model)
            .memory(new SlidingWindowMemory(20))
            .tools(new SearchTool())
            .build();
    }
}
```

## Custom Model Providers

Implement `ChatModel` for sync or `StreamingChatModel` for streaming:

```java
public class MyCustomModel implements ChatModel {
    @Override
    public ChatResponse send(ChatRequest request) {
        // call your LLM provider
    }
}
```

## Building

```bash
mvn clean verify
```

Requires Java 8+ and Maven 3.6+.

## License

[Apache License 2.0](LICENSE)

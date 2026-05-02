# agentic4j Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Java 8 compatible AI agent framework (agentic4j) with a pluggable model interface, OpenAI provider, and optional Spring Boot starter — replacing LangChain4j for the price-agent project.

**Architecture:** Multi-module Maven project. `agentic4j-core` has zero external dependencies (JDK 8 only) and contains all interfaces, annotations, memory, and the agent builder with ReAct loop. `agentic4j-openai` implements the OpenAI-compatible provider using OkHttp 3.x + Jackson. `agentic4j-spring-boot-starter` provides Spring Boot 2.7.x auto-configuration.

**Tech Stack:** Java 8, Maven, OkHttp 3.14.x, Jackson 2.13.x, Spring Boot 2.7.x (optional), JUnit 4.13.x, Mockito 2.x, OkHttp MockWebServer

---

## File Structure

```
agentic4j/
├── pom.xml                                                          # Parent POM (module aggregator)
├── agentic4j-core/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/agentic4j/core/
│       │   ├── ChatModel.java                                       # Sync model interface
│       │   ├── StreamingChatModel.java                              # Streaming model interface
│       │   ├── StreamingResponseHandler.java                        # Callback interface for streaming
│       │   ├── ChatMessage.java                                     # Message POJO (role, content, toolCalls, toolCallId)
│       │   ├── Role.java                                            # Enum: SYSTEM, USER, ASSISTANT, TOOL
│       │   ├── ChatRequest.java                                     # Request POJO with builder
│       │   ├── ChatResponse.java                                    # Response POJO
│       │   ├── FinishReason.java                                    # Enum: STOP, TOOL_CALLS, LENGTH, CONTENT_FILTER
│       │   ├── TokenUsage.java                                      # Token count POJO
│       │   ├── ToolDefinition.java                                  # Tool schema POJO
│       │   ├── ToolParameter.java                                   # Tool param POJO
│       │   ├── ToolCall.java                                        # Tool invocation from LLM
│       │   ├── ToolExecutionResult.java                             # Tool result POJO
│       │   ├── StreamingResponse.java                               # Fluent streaming callback interface
│       │   ├── annotation/
│       │   │   ├── AgentTool.java                                   # @AgentTool annotation
│       │   │   ├── Param.java                                       # @Param annotation
│       │   │   └── SystemPrompt.java                                # @SystemPrompt annotation
│       │   ├── memory/
│       │   │   ├── ChatMemory.java                                  # Memory interface
│       │   │   └── SlidingWindowMemory.java                         # Sliding window implementation
│       │   ├── agent/
│       │   │   ├── AgentBuilder.java                                # Main builder + proxy factory
│       │   │   ├── AgentInvocationHandler.java                      # Dynamic proxy handler (sync)
│       │   │   ├── StreamingAgentInvocationHandler.java             # Dynamic proxy handler (streaming)
│       │   │   ├── ToolExecutor.java                                # Scans @AgentTool, invokes via reflection
│       │   │   └── ToolScanner.java                                 # Scans tool instances → ToolDefinition list
│       │   └── exception/
│       │       ├── Agentic4jException.java                          # Base exception
│       │       ├── ModelException.java                              # LLM errors
│       │       ├── ToolExecutionException.java                      # Tool method errors
│       │       └── MaxIterationsException.java                      # ReAct loop exhausted
│       └── test/java/com/agentic4j/core/
│           ├── ChatMessageTest.java                                 # Message creation tests
│           ├── ChatRequestTest.java                                 # Request builder tests
│           ├── SlidingWindowMemoryTest.java                         # Memory sliding window tests
│           ├── ToolScannerTest.java                                 # Annotation scanning tests
│           ├── ToolExecutorTest.java                                # Tool invocation tests
│           ├── AgentBuilderSyncTest.java                            # Sync agent proxy + ReAct loop tests
│           └── AgentBuilderStreamingTest.java                       # Streaming agent proxy tests
├── agentic4j-openai/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/agentic4j/openai/
│       │   ├── OpenAiChatModel.java                                 # Sync OpenAI implementation
│       │   ├── OpenAiStreamingChatModel.java                        # Streaming OpenAI implementation
│       │   └── OpenAiRequestMapper.java                             # ChatRequest ↔ OpenAI JSON mapping
│       └── test/java/com/agentic4j/openai/
│           ├── OpenAiRequestMapperTest.java                         # JSON mapping tests
│           ├── OpenAiChatModelTest.java                             # Sync model tests with MockWebServer
│           └── OpenAiStreamingChatModelTest.java                    # Streaming model tests with MockWebServer
└── agentic4j-spring-boot-starter/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/agentic4j/spring/
        │   │   ├── Agentic4jAutoConfiguration.java                  # Auto-config class
        │   │   └── Agentic4jProperties.java                         # @ConfigurationProperties
        │   └── resources/META-INF/
        │       └── spring.factories                                 # Auto-config registration
        └── test/java/com/agentic4j/spring/
            └── Agentic4jAutoConfigurationTest.java                  # Spring context tests
```

---

### Task 1: Maven Multi-Module Project Setup

**Files:**
- Create: `pom.xml` (parent)
- Create: `agentic4j-core/pom.xml`
- Create: `agentic4j-openai/pom.xml`
- Create: `agentic4j-spring-boot-starter/pom.xml`

- [ ] **Step 1: Create parent POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.agentic4j</groupId>
    <artifactId>agentic4j-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>agentic4j</name>
    <description>Lightweight Java 8 compatible AI agent framework</description>

    <modules>
        <module>agentic4j-core</module>
        <module>agentic4j-openai</module>
        <module>agentic4j-spring-boot-starter</module>
    </modules>

    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <okhttp.version>3.14.9</okhttp.version>
        <jackson.version>2.13.5</jackson.version>
        <spring-boot.version>2.7.18</spring-boot.version>
        <junit.version>4.13.2</junit.version>
        <mockito.version>2.28.2</mockito.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.agentic4j</groupId>
                <artifactId>agentic4j-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.squareup.okhttp3</groupId>
                <artifactId>okhttp</artifactId>
                <version>${okhttp.version}</version>
            </dependency>
            <dependency>
                <groupId>com.squareup.okhttp3</groupId>
                <artifactId>mockwebserver</artifactId>
                <version>${okhttp.version}</version>
            </dependency>
            <dependency>
                <groupId>com.fasterxml.jackson.core</groupId>
                <artifactId>jackson-databind</artifactId>
                <version>${jackson.version}</version>
            </dependency>
            <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>${junit.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>${mockito.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create core module POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.agentic4j</groupId>
        <artifactId>agentic4j-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>agentic4j-core</artifactId>
    <name>agentic4j Core</name>
    <description>Core interfaces, annotations, memory, and agent builder</description>

    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create openai module POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.agentic4j</groupId>
        <artifactId>agentic4j-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>agentic4j-openai</artifactId>
    <name>agentic4j OpenAI Provider</name>
    <description>OpenAI-compatible chat model provider using OkHttp and Jackson</description>

    <dependencies>
        <dependency>
            <groupId>com.agentic4j</groupId>
            <artifactId>agentic4j-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>mockwebserver</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create spring-boot-starter module POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.agentic4j</groupId>
        <artifactId>agentic4j-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>agentic4j-spring-boot-starter</artifactId>
    <name>agentic4j Spring Boot Starter</name>
    <description>Spring Boot 2.7.x auto-configuration for agentic4j</description>

    <dependencies>
        <dependency>
            <groupId>com.agentic4j</groupId>
            <artifactId>agentic4j-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.agentic4j</groupId>
            <artifactId>agentic4j-openai</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <version>${spring-boot.version}</version>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>${spring-boot.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Create directory structure**

Run:
```bash
mkdir -p agentic4j-core/src/main/java/com/agentic4j/core/annotation
mkdir -p agentic4j-core/src/main/java/com/agentic4j/core/memory
mkdir -p agentic4j-core/src/main/java/com/agentic4j/core/agent
mkdir -p agentic4j-core/src/main/java/com/agentic4j/core/exception
mkdir -p agentic4j-core/src/test/java/com/agentic4j/core
mkdir -p agentic4j-openai/src/main/java/com/agentic4j/openai
mkdir -p agentic4j-openai/src/test/java/com/agentic4j/openai
mkdir -p agentic4j-spring-boot-starter/src/main/java/com/agentic4j/spring
mkdir -p agentic4j-spring-boot-starter/src/main/resources/META-INF
mkdir -p agentic4j-spring-boot-starter/src/test/java/com/agentic4j/spring
```

- [ ] **Step 6: Verify build compiles**

Run: `mvn compile`
Expected: BUILD SUCCESS (empty modules, no source files yet)

- [ ] **Step 7: Commit**

```bash
git add pom.xml agentic4j-core/pom.xml agentic4j-openai/pom.xml agentic4j-spring-boot-starter/pom.xml
git commit -m "feat: setup multi-module Maven project structure"
```

---

### Task 2: Core Enums and Value Types

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/Role.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/FinishReason.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/TokenUsage.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/ToolCall.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/ToolParameter.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/ToolDefinition.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/ToolExecutionResult.java`
- Test: `agentic4j-core/src/test/java/com/agentic4j/core/ChatMessageTest.java`

- [ ] **Step 1: Write test for Role enum and basic value types**

```java
package com.agentic4j.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ChatMessageTest {

    @Test
    public void testRoleValues() {
        assertEquals(4, Role.values().length);
        assertNotNull(Role.SYSTEM);
        assertNotNull(Role.USER);
        assertNotNull(Role.ASSISTANT);
        assertNotNull(Role.TOOL);
    }

    @Test
    public void testFinishReasonValues() {
        assertEquals(4, FinishReason.values().length);
        assertNotNull(FinishReason.STOP);
        assertNotNull(FinishReason.TOOL_CALLS);
        assertNotNull(FinishReason.LENGTH);
        assertNotNull(FinishReason.CONTENT_FILTER);
    }

    @Test
    public void testTokenUsage() {
        TokenUsage usage = new TokenUsage(10, 20, 30);
        assertEquals(10, usage.getPromptTokens());
        assertEquals(20, usage.getCompletionTokens());
        assertEquals(30, usage.getTotalTokens());
    }

    @Test
    public void testToolCall() {
        ToolCall call = new ToolCall("call_123", "searchProducts", "{\"term\":\"milk\"}");
        assertEquals("call_123", call.getId());
        assertEquals("searchProducts", call.getName());
        assertEquals("{\"term\":\"milk\"}", call.getArguments());
    }

    @Test
    public void testToolParameter() {
        ToolParameter param = new ToolParameter("query", "The SQL query", "string", true);
        assertEquals("query", param.getName());
        assertEquals("The SQL query", param.getDescription());
        assertEquals("string", param.getType());
        assertTrue(param.isRequired());
    }

    @Test
    public void testToolDefinition() {
        ToolParameter param = new ToolParameter("term", "search term", "string", true);
        ToolDefinition def = new ToolDefinition("searchProducts", "Search for products", Collections.singletonList(param));
        assertEquals("searchProducts", def.getName());
        assertEquals("Search for products", def.getDescription());
        assertEquals(1, def.getParameters().size());
        assertEquals("term", def.getParameters().get(0).getName());
    }

    @Test
    public void testToolExecutionResult() {
        ToolExecutionResult result = new ToolExecutionResult("call_123", "searchProducts", "Found 3 products");
        assertEquals("call_123", result.getToolCallId());
        assertEquals("searchProducts", result.getToolName());
        assertEquals("Found 3 products", result.getResult());
    }

    @Test
    public void testChatMessageUser() {
        ChatMessage msg = ChatMessage.user("Hello");
        assertEquals(Role.USER, msg.getRole());
        assertEquals("Hello", msg.getContent());
        assertNull(msg.getToolCalls());
        assertNull(msg.getToolCallId());
    }

    @Test
    public void testChatMessageSystem() {
        ChatMessage msg = ChatMessage.system("You are a helpful assistant");
        assertEquals(Role.SYSTEM, msg.getRole());
        assertEquals("You are a helpful assistant", msg.getContent());
    }

    @Test
    public void testChatMessageAssistantWithToolCalls() {
        ToolCall call = new ToolCall("call_1", "search", "{\"q\":\"test\"}");
        ChatMessage msg = ChatMessage.assistantWithToolCalls(Collections.singletonList(call));
        assertEquals(Role.ASSISTANT, msg.getRole());
        assertNull(msg.getContent());
        assertEquals(1, msg.getToolCalls().size());
        assertEquals("call_1", msg.getToolCalls().get(0).getId());
    }

    @Test
    public void testChatMessageToolResult() {
        ChatMessage msg = ChatMessage.toolResult("call_1", "result text");
        assertEquals(Role.TOOL, msg.getRole());
        assertEquals("result text", msg.getContent());
        assertEquals("call_1", msg.getToolCallId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd agentic4j-core && mvn test -Dtest=ChatMessageTest -pl . -q`
Expected: FAIL — classes do not exist yet

- [ ] **Step 3: Implement Role enum**

```java
package com.agentic4j.core;

public enum Role {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}
```

- [ ] **Step 4: Implement FinishReason enum**

```java
package com.agentic4j.core;

public enum FinishReason {
    STOP,
    TOOL_CALLS,
    LENGTH,
    CONTENT_FILTER
}
```

- [ ] **Step 5: Implement TokenUsage**

```java
package com.agentic4j.core;

public class TokenUsage {

    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;

    public TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }
}
```

- [ ] **Step 6: Implement ToolCall**

```java
package com.agentic4j.core;

public class ToolCall {

    private final String id;
    private final String name;
    private final String arguments;

    public ToolCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArguments() {
        return arguments;
    }
}
```

- [ ] **Step 7: Implement ToolParameter**

```java
package com.agentic4j.core;

public class ToolParameter {

    private final String name;
    private final String description;
    private final String type;
    private final boolean required;

    public ToolParameter(String name, String description, String type, boolean required) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }
}
```

- [ ] **Step 8: Implement ToolDefinition**

```java
package com.agentic4j.core;

import java.util.Collections;
import java.util.List;

public class ToolDefinition {

    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;

    public ToolDefinition(String name, String description, List<ToolParameter> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = Collections.unmodifiableList(parameters);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ToolParameter> getParameters() {
        return parameters;
    }
}
```

- [ ] **Step 9: Implement ToolExecutionResult**

```java
package com.agentic4j.core;

public class ToolExecutionResult {

    private final String toolCallId;
    private final String toolName;
    private final String result;

    public ToolExecutionResult(String toolCallId, String toolName, String result) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.result = result;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getResult() {
        return result;
    }
}
```

- [ ] **Step 10: Implement ChatMessage**

```java
package com.agentic4j.core;

import java.util.Collections;
import java.util.List;

public class ChatMessage {

    private final Role role;
    private final String content;
    private final List<ToolCall> toolCalls;
    private final String toolCallId;

    private ChatMessage(Role role, String content, List<ToolCall> toolCalls, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content, null, null);
    }

    public static ChatMessage assistantWithToolCalls(List<ToolCall> toolCalls) {
        return new ChatMessage(Role.ASSISTANT, null, Collections.unmodifiableList(toolCalls), null);
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        return new ChatMessage(Role.TOOL, content, null, toolCallId);
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }
}
```

- [ ] **Step 11: Run tests to verify they pass**

Run: `cd agentic4j-core && mvn test -Dtest=ChatMessageTest -pl . -q`
Expected: All 10 tests PASS

- [ ] **Step 12: Commit**

```bash
git add agentic4j-core/src
git commit -m "feat(core): add enums, value types, and ChatMessage"
```

---

### Task 3: ChatRequest, ChatResponse, and Exceptions

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/ChatRequest.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/ChatResponse.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/exception/Agentic4jException.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/exception/ModelException.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/exception/ToolExecutionException.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/exception/MaxIterationsException.java`
- Test: `agentic4j-core/src/test/java/com/agentic4j/core/ChatRequestTest.java`

- [ ] **Step 1: Write test for ChatRequest builder and ChatResponse**

```java
package com.agentic4j.core;

import com.agentic4j.core.exception.Agentic4jException;
import com.agentic4j.core.exception.MaxIterationsException;
import com.agentic4j.core.exception.ModelException;
import com.agentic4j.core.exception.ToolExecutionException;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ChatRequestTest {

    @Test
    public void testChatRequestBuilder() {
        ChatMessage msg = ChatMessage.user("Hello");
        ToolParameter param = new ToolParameter("q", "query", "string", true);
        ToolDefinition tool = new ToolDefinition("search", "Search", Collections.singletonList(param));

        ChatRequest request = ChatRequest.builder()
                .addMessage(msg)
                .addTool(tool)
                .build();

        assertEquals(1, request.getMessages().size());
        assertEquals("Hello", request.getMessages().get(0).getContent());
        assertEquals(1, request.getTools().size());
        assertEquals("search", request.getTools().get(0).getName());
    }

    @Test
    public void testChatRequestBuilderNoTools() {
        ChatMessage msg = ChatMessage.user("Hello");

        ChatRequest request = ChatRequest.builder()
                .addMessage(msg)
                .build();

        assertEquals(1, request.getMessages().size());
        assertTrue(request.getTools().isEmpty());
    }

    @Test
    public void testChatRequestMessagesAreUnmodifiable() {
        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("test"))
                .build();

        try {
            request.getMessages().add(ChatMessage.user("hack"));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testChatResponse() {
        ChatMessage msg = ChatMessage.assistant("Hi there");
        TokenUsage usage = new TokenUsage(5, 10, 15);
        ChatResponse response = new ChatResponse(msg, usage, FinishReason.STOP);

        assertEquals("Hi there", response.getMessage().getContent());
        assertEquals(Role.ASSISTANT, response.getMessage().getRole());
        assertEquals(15, response.getUsage().getTotalTokens());
        assertEquals(FinishReason.STOP, response.getFinishReason());
    }

    @Test
    public void testAgentic4jException() {
        Agentic4jException ex = new Agentic4jException("test error");
        assertEquals("test error", ex.getMessage());

        Agentic4jException exWithCause = new Agentic4jException("wrapped", new RuntimeException("root"));
        assertEquals("wrapped", exWithCause.getMessage());
        assertEquals("root", exWithCause.getCause().getMessage());
    }

    @Test
    public void testModelException() {
        ModelException ex = new ModelException(429, "Rate limited", "Too many requests");
        assertEquals(429, ex.getStatusCode());
        assertEquals("Rate limited", ex.getMessage());
        assertEquals("Too many requests", ex.getResponseBody());
        assertTrue(ex instanceof Agentic4jException);
    }

    @Test
    public void testToolExecutionException() {
        ToolExecutionException ex = new ToolExecutionException("searchProducts", new RuntimeException("DB down"));
        assertEquals("searchProducts", ex.getToolName());
        assertTrue(ex.getMessage().contains("searchProducts"));
        assertTrue(ex instanceof Agentic4jException);
    }

    @Test
    public void testMaxIterationsException() {
        MaxIterationsException ex = new MaxIterationsException(10);
        assertEquals(10, ex.getMaxIterations());
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex instanceof Agentic4jException);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd agentic4j-core && mvn test -Dtest=ChatRequestTest -pl . -q`
Expected: FAIL — classes do not exist

- [ ] **Step 3: Implement ChatRequest**

```java
package com.agentic4j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRequest {

    private final List<ChatMessage> messages;
    private final List<ToolDefinition> tools;

    private ChatRequest(List<ChatMessage> messages, List<ToolDefinition> tools) {
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
        this.tools = Collections.unmodifiableList(new ArrayList<ToolDefinition>(tools));
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<ChatMessage> messages = new ArrayList<ChatMessage>();
        private final List<ToolDefinition> tools = new ArrayList<ToolDefinition>();

        public Builder addMessage(ChatMessage message) {
            messages.add(message);
            return this;
        }

        public Builder addMessages(List<ChatMessage> messages) {
            this.messages.addAll(messages);
            return this;
        }

        public Builder addTool(ToolDefinition tool) {
            tools.add(tool);
            return this;
        }

        public Builder addTools(List<ToolDefinition> tools) {
            this.tools.addAll(tools);
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(messages, tools);
        }
    }
}
```

- [ ] **Step 4: Implement ChatResponse**

```java
package com.agentic4j.core;

public class ChatResponse {

    private final ChatMessage message;
    private final TokenUsage usage;
    private final FinishReason finishReason;

    public ChatResponse(ChatMessage message, TokenUsage usage, FinishReason finishReason) {
        this.message = message;
        this.usage = usage;
        this.finishReason = finishReason;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public TokenUsage getUsage() {
        return usage;
    }

    public FinishReason getFinishReason() {
        return finishReason;
    }
}
```

- [ ] **Step 5: Implement exception hierarchy**

`Agentic4jException.java`:
```java
package com.agentic4j.core.exception;

public class Agentic4jException extends RuntimeException {

    public Agentic4jException(String message) {
        super(message);
    }

    public Agentic4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

`ModelException.java`:
```java
package com.agentic4j.core.exception;

public class ModelException extends Agentic4jException {

    private final int statusCode;
    private final String responseBody;

    public ModelException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
```

`ToolExecutionException.java`:
```java
package com.agentic4j.core.exception;

public class ToolExecutionException extends Agentic4jException {

    private final String toolName;

    public ToolExecutionException(String toolName, Throwable cause) {
        super("Tool '" + toolName + "' execution failed: " + cause.getMessage(), cause);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
}
```

`MaxIterationsException.java`:
```java
package com.agentic4j.core.exception;

public class MaxIterationsException extends Agentic4jException {

    private final int maxIterations;

    public MaxIterationsException(int maxIterations) {
        super("Agent reached maximum tool iterations (" + maxIterations + ") without producing a final response");
        this.maxIterations = maxIterations;
    }

    public int getMaxIterations() {
        return maxIterations;
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd agentic4j-core && mvn test -Dtest=ChatRequestTest -pl . -q`
Expected: All 8 tests PASS

- [ ] **Step 7: Commit**

```bash
git add agentic4j-core/src
git commit -m "feat(core): add ChatRequest, ChatResponse, and exception hierarchy"
```

---

### Task 4: Annotations and Model Interfaces

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/annotation/AgentTool.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/annotation/Param.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/annotation/SystemPrompt.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/ChatModel.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/StreamingChatModel.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/StreamingResponseHandler.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/StreamingResponse.java`

- [ ] **Step 1: Implement @AgentTool annotation**

```java
package com.agentic4j.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AgentTool {
    String value();
}
```

- [ ] **Step 2: Implement @Param annotation**

```java
package com.agentic4j.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {
    String value();
}
```

- [ ] **Step 3: Implement @SystemPrompt annotation**

```java
package com.agentic4j.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SystemPrompt {
    String value() default "";
    String fromResource() default "";
}
```

- [ ] **Step 4: Implement ChatModel interface**

```java
package com.agentic4j.core;

public interface ChatModel {
    ChatResponse send(ChatRequest request);
}
```

- [ ] **Step 5: Implement StreamingChatModel and StreamingResponseHandler**

`StreamingChatModel.java`:
```java
package com.agentic4j.core;

public interface StreamingChatModel {
    void send(ChatRequest request, StreamingResponseHandler handler);
}
```

`StreamingResponseHandler.java`:
```java
package com.agentic4j.core;

public interface StreamingResponseHandler {
    void onToken(String token);
    void onToolExecution(ToolExecutionResult result);
    void onComplete(ChatResponse response);
    void onError(Throwable error);
}
```

- [ ] **Step 6: Implement StreamingResponse**

```java
package com.agentic4j.core;

import java.util.function.Consumer;

public interface StreamingResponse {
    StreamingResponse onToken(Consumer<String> handler);
    StreamingResponse onToolExecuted(Consumer<ToolExecutionResult> handler);
    StreamingResponse onComplete(Consumer<ChatResponse> handler);
    StreamingResponse onError(Consumer<Throwable> handler);
    void start();
}
```

- [ ] **Step 7: Verify build compiles**

Run: `cd agentic4j-core && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add agentic4j-core/src
git commit -m "feat(core): add annotations, model interfaces, and StreamingResponse"
```

---

### Task 5: Chat Memory

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/memory/ChatMemory.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/memory/SlidingWindowMemory.java`
- Test: `agentic4j-core/src/test/java/com/agentic4j/core/SlidingWindowMemoryTest.java`

- [ ] **Step 1: Write test for SlidingWindowMemory**

```java
package com.agentic4j.core;

import com.agentic4j.core.memory.ChatMemory;
import com.agentic4j.core.memory.SlidingWindowMemory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SlidingWindowMemoryTest {

    @Test
    public void testAddAndRetrieve() {
        ChatMemory memory = new SlidingWindowMemory(10);
        memory.add(ChatMessage.user("Hello"));
        memory.add(ChatMessage.assistant("Hi there"));

        List<ChatMessage> messages = memory.messages();
        assertEquals(2, messages.size());
        assertEquals("Hello", messages.get(0).getContent());
        assertEquals("Hi there", messages.get(1).getContent());
    }

    @Test
    public void testSlidingWindowEvictsOldest() {
        ChatMemory memory = new SlidingWindowMemory(3);
        memory.add(ChatMessage.user("msg1"));
        memory.add(ChatMessage.assistant("msg2"));
        memory.add(ChatMessage.user("msg3"));
        memory.add(ChatMessage.assistant("msg4"));

        List<ChatMessage> messages = memory.messages();
        assertEquals(3, messages.size());
        assertEquals("msg2", messages.get(0).getContent());
        assertEquals("msg3", messages.get(1).getContent());
        assertEquals("msg4", messages.get(2).getContent());
    }

    @Test
    public void testClear() {
        ChatMemory memory = new SlidingWindowMemory(10);
        memory.add(ChatMessage.user("Hello"));
        memory.add(ChatMessage.assistant("Hi"));

        memory.clear();
        assertTrue(memory.messages().isEmpty());
    }

    @Test
    public void testWindowSizeOne() {
        ChatMemory memory = new SlidingWindowMemory(1);
        memory.add(ChatMessage.user("first"));
        memory.add(ChatMessage.user("second"));

        List<ChatMessage> messages = memory.messages();
        assertEquals(1, messages.size());
        assertEquals("second", messages.get(0).getContent());
    }

    @Test
    public void testMessagesReturnsCopy() {
        ChatMemory memory = new SlidingWindowMemory(10);
        memory.add(ChatMessage.user("Hello"));

        List<ChatMessage> messages = memory.messages();
        assertEquals(1, messages.size());

        // Modifying returned list should not affect internal state
        try {
            messages.add(ChatMessage.user("hack"));
            // If no exception, verify internal state unchanged
            assertEquals(1, memory.messages().size());
        } catch (UnsupportedOperationException e) {
            // also acceptable — unmodifiable list
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd agentic4j-core && mvn test -Dtest=SlidingWindowMemoryTest -pl . -q`
Expected: FAIL — classes do not exist

- [ ] **Step 3: Implement ChatMemory interface**

```java
package com.agentic4j.core.memory;

import com.agentic4j.core.ChatMessage;

import java.util.List;

public interface ChatMemory {
    void add(ChatMessage message);
    List<ChatMessage> messages();
    void clear();
}
```

- [ ] **Step 4: Implement SlidingWindowMemory**

```java
package com.agentic4j.core.memory;

import com.agentic4j.core.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SlidingWindowMemory implements ChatMemory {

    private final int maxMessages;
    private final LinkedList<ChatMessage> messages = new LinkedList<ChatMessage>();

    public SlidingWindowMemory(int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages must be at least 1");
        }
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(ChatMessage message) {
        messages.addLast(message);
        while (messages.size() > maxMessages) {
            messages.removeFirst();
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
    }

    @Override
    public void clear() {
        messages.clear();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd agentic4j-core && mvn test -Dtest=SlidingWindowMemoryTest -pl . -q`
Expected: All 5 tests PASS

- [ ] **Step 6: Commit**

```bash
git add agentic4j-core/src
git commit -m "feat(core): add ChatMemory interface and SlidingWindowMemory"
```

---

### Task 6: Tool Scanner

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/agent/ToolScanner.java`
- Test: `agentic4j-core/src/test/java/com/agentic4j/core/ToolScannerTest.java`

- [ ] **Step 1: Write test for ToolScanner**

```java
package com.agentic4j.core;

import com.agentic4j.core.agent.ToolScanner;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ToolScannerTest {

    static class SimpleStringTool {
        @AgentTool("Search for items")
        public String search(@Param("The search query") String query) {
            return "found";
        }
    }

    static class MultiParamTool {
        @AgentTool("Execute a query with limit")
        public String query(
                @Param("The SQL query") String sql,
                @Param("Max rows") int limit) {
            return "result";
        }
    }

    static class NoToolMethods {
        public String notATool(String input) {
            return input;
        }
    }

    static class MixedTool {
        @AgentTool("Tool method")
        public String toolMethod(@Param("input") String input) {
            return input;
        }

        public String regularMethod(String input) {
            return input;
        }
    }

    static class TypesTool {
        @AgentTool("Test types")
        public String testTypes(
                @Param("a string") String s,
                @Param("an int") int i,
                @Param("a double") double d,
                @Param("a boolean") boolean b) {
            return "ok";
        }
    }

    @Test
    public void testScanSimpleTool() {
        List<ToolDefinition> defs = ToolScanner.scan(new SimpleStringTool());
        assertEquals(1, defs.size());

        ToolDefinition def = defs.get(0);
        assertEquals("search", def.getName());
        assertEquals("Search for items", def.getDescription());
        assertEquals(1, def.getParameters().size());

        ToolParameter param = def.getParameters().get(0);
        assertEquals("query", param.getName());
        assertEquals("The search query", param.getDescription());
        assertEquals("string", param.getType());
        assertTrue(param.isRequired());
    }

    @Test
    public void testScanMultiParamTool() {
        List<ToolDefinition> defs = ToolScanner.scan(new MultiParamTool());
        assertEquals(1, defs.size());

        ToolDefinition def = defs.get(0);
        assertEquals(2, def.getParameters().size());
        assertEquals("string", def.getParameters().get(0).getType());
        assertEquals("integer", def.getParameters().get(1).getType());
    }

    @Test
    public void testScanNoToolMethods() {
        List<ToolDefinition> defs = ToolScanner.scan(new NoToolMethods());
        assertTrue(defs.isEmpty());
    }

    @Test
    public void testScanMixedTool() {
        List<ToolDefinition> defs = ToolScanner.scan(new MixedTool());
        assertEquals(1, defs.size());
        assertEquals("toolMethod", defs.get(0).getName());
    }

    @Test
    public void testScanMultipleInstances() {
        List<ToolDefinition> defs = ToolScanner.scan(new SimpleStringTool(), new MultiParamTool());
        assertEquals(2, defs.size());
    }

    @Test
    public void testTypeMapping() {
        List<ToolDefinition> defs = ToolScanner.scan(new TypesTool());
        ToolDefinition def = defs.get(0);
        assertEquals(4, def.getParameters().size());
        assertEquals("string", def.getParameters().get(0).getType());
        assertEquals("integer", def.getParameters().get(1).getType());
        assertEquals("number", def.getParameters().get(2).getType());
        assertEquals("boolean", def.getParameters().get(3).getType());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd agentic4j-core && mvn test -Dtest=ToolScannerTest -pl . -q`
Expected: FAIL

- [ ] **Step 3: Implement ToolScanner**

```java
package com.agentic4j.core.agent;

import com.agentic4j.core.ToolDefinition;
import com.agentic4j.core.ToolParameter;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolScanner {

    private static final Map<Class<?>, String> TYPE_MAP = new HashMap<Class<?>, String>();

    static {
        TYPE_MAP.put(String.class, "string");
        TYPE_MAP.put(int.class, "integer");
        TYPE_MAP.put(Integer.class, "integer");
        TYPE_MAP.put(long.class, "integer");
        TYPE_MAP.put(Long.class, "integer");
        TYPE_MAP.put(double.class, "number");
        TYPE_MAP.put(Double.class, "number");
        TYPE_MAP.put(float.class, "number");
        TYPE_MAP.put(Float.class, "number");
        TYPE_MAP.put(boolean.class, "boolean");
        TYPE_MAP.put(Boolean.class, "boolean");
    }

    public static List<ToolDefinition> scan(Object... toolInstances) {
        List<ToolDefinition> definitions = new ArrayList<ToolDefinition>();
        for (Object instance : toolInstances) {
            for (Method method : instance.getClass().getMethods()) {
                AgentTool annotation = method.getAnnotation(AgentTool.class);
                if (annotation == null) {
                    continue;
                }
                List<ToolParameter> params = scanParameters(method);
                definitions.add(new ToolDefinition(method.getName(), annotation.value(), params));
            }
        }
        return definitions;
    }

    private static List<ToolParameter> scanParameters(Method method) {
        List<ToolParameter> params = new ArrayList<ToolParameter>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < paramTypes.length; i++) {
            Param paramAnnotation = findParamAnnotation(paramAnnotations[i]);
            String name = paramAnnotation != null ? getParamName(paramAnnotation, i) : "arg" + i;
            String description = paramAnnotation != null ? paramAnnotation.value() : "";
            String type = mapType(paramTypes[i]);
            params.add(new ToolParameter(name, description, type, true));
        }
        return params;
    }

    private static Param findParamAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Param) {
                return (Param) annotation;
            }
        }
        return null;
    }

    private static String getParamName(Param paramAnnotation, int index) {
        // Java 8 doesn't preserve parameter names by default,
        // so we use the annotation value as description and generate a name from it.
        // The actual parameter name for the JSON schema comes from the method parameter name
        // if compiled with -parameters, otherwise falls back to arg0, arg1, etc.
        return "arg" + index;
    }

    static String mapType(Class<?> type) {
        String mapped = TYPE_MAP.get(type);
        return mapped != null ? mapped : "string";
    }
}
```

- [ ] **Step 4: Run test to verify — will partially fail due to param name**

Run: `cd agentic4j-core && mvn test -Dtest=ToolScannerTest -pl . -q`
Expected: Test `testScanSimpleTool` fails because `getParamName` returns "arg0" but test expects "query"

- [ ] **Step 5: Fix ToolScanner to extract parameter names via reflection**

The Java 8 reflection API provides `Method.getParameters()` which returns `Parameter` objects. If compiled with `-parameters`, `Parameter.getName()` returns the real name. Otherwise it returns "arg0". We should try the real name first and fall back.

Update `ToolScanner.scanParameters`:

```java
    private static List<ToolParameter> scanParameters(Method method) {
        List<ToolParameter> params = new ArrayList<ToolParameter>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        java.lang.reflect.Parameter[] reflectParams = method.getParameters();

        for (int i = 0; i < paramTypes.length; i++) {
            Param paramAnnotation = findParamAnnotation(paramAnnotations[i]);
            String name = reflectParams[i].isNamePresent() ? reflectParams[i].getName() : "arg" + i;
            String description = paramAnnotation != null ? paramAnnotation.value() : "";
            String type = mapType(paramTypes[i]);
            params.add(new ToolParameter(name, description, type, true));
        }
        return params;
    }
```

Remove the unused `getParamName` method.

Also update `agentic4j-core/pom.xml` to add the `-parameters` compiler flag:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

Add the same `<build>` block to `agentic4j-openai/pom.xml` and `agentic4j-spring-boot-starter/pom.xml` as well — any module that compiles tool classes needs `-parameters`.

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd agentic4j-core && mvn test -Dtest=ToolScannerTest -pl . -q`
Expected: All 6 tests PASS

- [ ] **Step 7: Commit**

```bash
git add agentic4j-core/src agentic4j-core/pom.xml agentic4j-openai/pom.xml agentic4j-spring-boot-starter/pom.xml
git commit -m "feat(core): add ToolScanner for @AgentTool annotation discovery"
```

---

### Task 7: Tool Executor

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/agent/ToolExecutor.java`
- Test: `agentic4j-core/src/test/java/com/agentic4j/core/ToolExecutorTest.java`

- [ ] **Step 1: Write test for ToolExecutor**

```java
package com.agentic4j.core;

import com.agentic4j.core.agent.ToolExecutor;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import org.junit.Test;

import static org.junit.Assert.*;

public class ToolExecutorTest {

    static class SearchTool {
        @AgentTool("Search for products")
        public String searchProducts(@Param("search term") String term) {
            return "Found: " + term;
        }
    }

    static class MultiParamTool {
        @AgentTool("Run query")
        public String runQuery(@Param("the query") String sql, @Param("max rows") int limit) {
            return "Query: " + sql + ", limit: " + limit;
        }
    }

    static class ErrorTool {
        @AgentTool("Fails always")
        public String failingTool(@Param("input") String input) {
            throw new RuntimeException("DB connection lost");
        }
    }

    @Test
    public void testExecuteSimpleTool() {
        ToolExecutor executor = new ToolExecutor(new SearchTool());
        ToolCall call = new ToolCall("call_1", "searchProducts", "{\"term\":\"milk\"}");

        ToolExecutionResult result = executor.execute(call);

        assertEquals("call_1", result.getToolCallId());
        assertEquals("searchProducts", result.getToolName());
        assertEquals("Found: milk", result.getResult());
    }

    @Test
    public void testExecuteMultiParamTool() {
        ToolExecutor executor = new ToolExecutor(new MultiParamTool());
        ToolCall call = new ToolCall("call_2", "runQuery", "{\"sql\":\"SELECT * FROM t\",\"limit\":10}");

        ToolExecutionResult result = executor.execute(call);

        assertEquals("Query: SELECT * FROM t, limit: 10", result.getResult());
    }

    @Test
    public void testExecuteToolError() {
        ToolExecutor executor = new ToolExecutor(new ErrorTool());
        ToolCall call = new ToolCall("call_3", "failingTool", "{\"input\":\"test\"}");

        ToolExecutionResult result = executor.execute(call);

        assertTrue(result.getResult().startsWith("Error: "));
        assertTrue(result.getResult().contains("DB connection lost"));
    }

    @Test
    public void testExecuteUnknownTool() {
        ToolExecutor executor = new ToolExecutor(new SearchTool());
        ToolCall call = new ToolCall("call_4", "nonExistent", "{}");

        ToolExecutionResult result = executor.execute(call);

        assertTrue(result.getResult().startsWith("Error: "));
        assertTrue(result.getResult().contains("nonExistent"));
    }

    @Test
    public void testExecuteMultipleToolInstances() {
        ToolExecutor executor = new ToolExecutor(new SearchTool(), new MultiParamTool());
        ToolCall call1 = new ToolCall("c1", "searchProducts", "{\"term\":\"rice\"}");
        ToolCall call2 = new ToolCall("c2", "runQuery", "{\"sql\":\"SELECT 1\",\"limit\":5}");

        assertEquals("Found: rice", executor.execute(call1).getResult());
        assertEquals("Query: SELECT 1, limit: 5", executor.execute(call2).getResult());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd agentic4j-core && mvn test -Dtest=ToolExecutorTest -pl . -q`
Expected: FAIL

- [ ] **Step 3: Implement ToolExecutor**

Note: ToolExecutor needs to parse JSON arguments. Since the core module has no Jackson dependency, we implement a minimal JSON string parser that handles flat objects with string, integer, number, and boolean values. This avoids adding a dependency to the core module.

```java
package com.agentic4j.core.agent;

import com.agentic4j.core.ToolCall;
import com.agentic4j.core.ToolExecutionResult;
import com.agentic4j.core.annotation.AgentTool;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolExecutor {

    private final Map<String, ToolMethod> toolMethods = new HashMap<String, ToolMethod>();

    public ToolExecutor(Object... toolInstances) {
        for (Object instance : toolInstances) {
            for (Method method : instance.getClass().getMethods()) {
                AgentTool annotation = method.getAnnotation(AgentTool.class);
                if (annotation != null) {
                    toolMethods.put(method.getName(), new ToolMethod(instance, method));
                }
            }
        }
    }

    public ToolExecutionResult execute(ToolCall call) {
        ToolMethod toolMethod = toolMethods.get(call.getName());
        if (toolMethod == null) {
            return new ToolExecutionResult(call.getId(), call.getName(),
                    "Error: Unknown tool '" + call.getName() + "'");
        }

        try {
            Map<String, String> args = parseJsonArguments(call.getArguments());
            Object[] params = resolveParameters(toolMethod.method, args);
            Object result = toolMethod.method.invoke(toolMethod.instance, params);
            return new ToolExecutionResult(call.getId(), call.getName(),
                    result != null ? result.toString() : "null");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return new ToolExecutionResult(call.getId(), call.getName(),
                    "Error: " + cause.getMessage());
        }
    }

    private Object[] resolveParameters(Method method, Map<String, String> args) {
        java.lang.reflect.Parameter[] params = method.getParameters();
        Object[] values = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String name = params[i].isNamePresent() ? params[i].getName() : "arg" + i;
            String rawValue = args.get(name);
            values[i] = convertValue(rawValue, params[i].getType());
        }
        return values;
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            if (targetType == int.class) return 0;
            if (targetType == long.class) return 0L;
            if (targetType == double.class) return 0.0;
            if (targetType == float.class) return 0.0f;
            if (targetType == boolean.class) return false;
            return null;
        }
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        return value;
    }

    static Map<String, String> parseJsonArguments(String json) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (json == null || json.trim().isEmpty() || json.trim().equals("{}")) {
            return result;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        int i = 0;
        while (i < trimmed.length()) {
            // skip whitespace
            while (i < trimmed.length() && Character.isWhitespace(trimmed.charAt(i))) i++;
            if (i >= trimmed.length()) break;

            // read key
            if (trimmed.charAt(i) != '"') { i++; continue; }
            i++; // skip opening quote
            int keyStart = i;
            while (i < trimmed.length() && trimmed.charAt(i) != '"') i++;
            String key = trimmed.substring(keyStart, i);
            i++; // skip closing quote

            // skip colon and whitespace
            while (i < trimmed.length() && (trimmed.charAt(i) == ':' || Character.isWhitespace(trimmed.charAt(i)))) i++;

            // read value
            String value;
            if (i < trimmed.length() && trimmed.charAt(i) == '"') {
                i++; // skip opening quote
                StringBuilder sb = new StringBuilder();
                while (i < trimmed.length() && trimmed.charAt(i) != '"') {
                    if (trimmed.charAt(i) == '\\' && i + 1 < trimmed.length()) {
                        i++;
                        sb.append(trimmed.charAt(i));
                    } else {
                        sb.append(trimmed.charAt(i));
                    }
                    i++;
                }
                value = sb.toString();
                i++; // skip closing quote
            } else {
                int valueStart = i;
                while (i < trimmed.length() && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}') i++;
                value = trimmed.substring(valueStart, i).trim();
            }

            result.put(key, value);

            // skip comma
            while (i < trimmed.length() && (trimmed.charAt(i) == ',' || Character.isWhitespace(trimmed.charAt(i)))) i++;
        }
        return result;
    }

    private static class ToolMethod {
        final Object instance;
        final Method method;

        ToolMethod(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd agentic4j-core && mvn test -Dtest=ToolExecutorTest -pl . -q`
Expected: All 5 tests PASS

- [ ] **Step 5: Commit**

```bash
git add agentic4j-core/src
git commit -m "feat(core): add ToolExecutor for reflection-based tool invocation"
```

---

### Task 8: AgentBuilder — Sync Agent Proxy

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/agent/AgentBuilder.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/agent/AgentInvocationHandler.java`
- Test: `agentic4j-core/src/test/java/com/agentic4j/core/AgentBuilderSyncTest.java`

- [ ] **Step 1: Write test for sync agent proxy with ReAct loop**

```java
package com.agentic4j.core;

import com.agentic4j.core.agent.AgentBuilder;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.exception.MaxIterationsException;
import com.agentic4j.core.memory.SlidingWindowMemory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AgentBuilderSyncTest {

    interface SimpleAssistant {
        @SystemPrompt("You are a helpful assistant")
        String chat(String message);
    }

    interface ResourceAssistant {
        @SystemPrompt(fromResource = "test-system-prompt.txt")
        String chat(String message);
    }

    public static class MockSearchTool {
        @AgentTool("Search for products")
        public String searchProducts(@Param("search term") String term) {
            return "Found: milk, cheese, yogurt";
        }
    }

    @Test
    public void testSimpleChatNoTools() {
        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                return new ChatResponse(
                        ChatMessage.assistant("Hello! How can I help?"),
                        new TokenUsage(10, 5, 15),
                        FinishReason.STOP
                );
            }
        };

        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class)
                .chatModel(mockModel)
                .memory(new SlidingWindowMemory(20))
                .build();

        String reply = assistant.chat("Hi");
        assertEquals("Hello! How can I help?", reply);
    }

    @Test
    public void testSystemPromptIncludedInRequest() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();

        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                return new ChatResponse(
                        ChatMessage.assistant("OK"),
                        new TokenUsage(10, 5, 15),
                        FinishReason.STOP
                );
            }
        };

        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class)
                .chatModel(mockModel)
                .memory(new SlidingWindowMemory(20))
                .build();

        assistant.chat("Hello");

        assertEquals(1, captured.size());
        ChatRequest req = captured.get(0);
        assertEquals(Role.SYSTEM, req.getMessages().get(0).getRole());
        assertEquals("You are a helpful assistant", req.getMessages().get(0).getContent());
        assertEquals(Role.USER, req.getMessages().get(1).getRole());
        assertEquals("Hello", req.getMessages().get(1).getContent());
    }

    @Test
    public void testReActLoopWithToolCall() {
        final int[] callCount = {0};

        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                callCount[0]++;
                if (callCount[0] == 1) {
                    // First call: LLM requests a tool call
                    ToolCall toolCall = new ToolCall("call_1", "searchProducts", "{\"term\":\"milk\"}");
                    return new ChatResponse(
                            ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall)),
                            new TokenUsage(20, 10, 30),
                            FinishReason.TOOL_CALLS
                    );
                } else {
                    // Second call: LLM gives final answer after seeing tool result
                    return new ChatResponse(
                            ChatMessage.assistant("I found milk, cheese, and yogurt for you."),
                            new TokenUsage(30, 15, 45),
                            FinishReason.STOP
                    );
                }
            }
        };

        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class)
                .chatModel(mockModel)
                .memory(new SlidingWindowMemory(20))
                .tools(new MockSearchTool())
                .build();

        String reply = assistant.chat("Find dairy products");
        assertEquals("I found milk, cheese, and yogurt for you.", reply);
        assertEquals(2, callCount[0]);
    }

    @Test
    public void testToolDefinitionsSentToModel() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();

        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                return new ChatResponse(
                        ChatMessage.assistant("OK"),
                        new TokenUsage(10, 5, 15),
                        FinishReason.STOP
                );
            }
        };

        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class)
                .chatModel(mockModel)
                .memory(new SlidingWindowMemory(20))
                .tools(new MockSearchTool())
                .build();

        assistant.chat("test");

        assertFalse(captured.get(0).getTools().isEmpty());
        assertEquals("searchProducts", captured.get(0).getTools().get(0).getName());
    }

    @Test
    public void testMemoryPersistsAcrossCalls() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();
        final int[] callCount = {0};

        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                callCount[0]++;
                return new ChatResponse(
                        ChatMessage.assistant("Reply " + callCount[0]),
                        new TokenUsage(10, 5, 15),
                        FinishReason.STOP
                );
            }
        };

        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class)
                .chatModel(mockModel)
                .memory(new SlidingWindowMemory(20))
                .build();

        assistant.chat("First message");
        assistant.chat("Second message");

        // Second request should contain system + first user + first assistant + second user
        ChatRequest secondReq = captured.get(1);
        assertEquals(4, secondReq.getMessages().size());
        assertEquals(Role.SYSTEM, secondReq.getMessages().get(0).getRole());
        assertEquals("First message", secondReq.getMessages().get(1).getContent());
        assertEquals("Reply 1", secondReq.getMessages().get(2).getContent());
        assertEquals("Second message", secondReq.getMessages().get(3).getContent());
    }

    @Test(expected = MaxIterationsException.class)
    public void testMaxIterationsThrows() {
        ChatModel infiniteToolModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                ToolCall toolCall = new ToolCall("call_x", "searchProducts", "{\"term\":\"loop\"}");
                return new ChatResponse(
                        ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall)),
                        new TokenUsage(10, 5, 15),
                        FinishReason.TOOL_CALLS
                );
            }
        };

        SimpleAssistant assistant = AgentBuilder.forInterface(SimpleAssistant.class)
                .chatModel(infiniteToolModel)
                .memory(new SlidingWindowMemory(20))
                .tools(new MockSearchTool())
                .maxToolIterations(3)
                .build();

        assistant.chat("Loop forever");
    }

    @Test
    public void testSystemPromptFromResource() {
        final List<ChatRequest> captured = new ArrayList<ChatRequest>();

        ChatModel mockModel = new ChatModel() {
            @Override
            public ChatResponse send(ChatRequest request) {
                captured.add(request);
                return new ChatResponse(
                        ChatMessage.assistant("OK"),
                        new TokenUsage(10, 5, 15),
                        FinishReason.STOP
                );
            }
        };

        ResourceAssistant assistant = AgentBuilder.forInterface(ResourceAssistant.class)
                .chatModel(mockModel)
                .memory(new SlidingWindowMemory(20))
                .build();

        assistant.chat("test");

        assertEquals(Role.SYSTEM, captured.get(0).getMessages().get(0).getRole());
        assertTrue(captured.get(0).getMessages().get(0).getContent().length() > 0);
    }
}
```

- [ ] **Step 2: Create test resource file**

Create `agentic4j-core/src/test/resources/test-system-prompt.txt`:
```
You are a test assistant for unit testing purposes.
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd agentic4j-core && mvn test -Dtest=AgentBuilderSyncTest -pl . -q`
Expected: FAIL

- [ ] **Step 4: Implement AgentInvocationHandler**

```java
package com.agentic4j.core.agent;

import com.agentic4j.core.*;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.exception.MaxIterationsException;
import com.agentic4j.core.memory.ChatMemory;

import java.io.BufferedReader;
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
            ChatRequest.Builder requestBuilder = ChatRequest.builder();

            if (systemPrompt != null) {
                requestBuilder.addMessage(ChatMessage.system(systemPrompt));
            }
            requestBuilder.addMessages(memory.messages());
            requestBuilder.addTools(toolDefinitions);

            ChatResponse response = chatModel.send(requestBuilder.build());
            ChatMessage assistantMessage = response.getMessage();

            if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {
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

        if (!annotation.fromResource().isEmpty()) {
            return loadResource(annotation.fromResource());
        }

        if (!annotation.value().isEmpty()) {
            return annotation.value();
        }

        return null;
    }

    private static String loadResource(String resourceName) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resourceName, e);
        }
    }
}
```

- [ ] **Step 5: Implement AgentBuilder**

```java
package com.agentic4j.core.agent;

import com.agentic4j.core.ChatModel;
import com.agentic4j.core.StreamingChatModel;
import com.agentic4j.core.ToolDefinition;
import com.agentic4j.core.memory.ChatMemory;

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

        AgentInvocationHandler handler = new AgentInvocationHandler(
                chatModel, memory, toolExecutor, toolDefinitions, maxToolIterations);

        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                handler
        );
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd agentic4j-core && mvn test -Dtest=AgentBuilderSyncTest -pl . -q`
Expected: All 7 tests PASS

- [ ] **Step 7: Commit**

```bash
git add agentic4j-core/src
git commit -m "feat(core): add AgentBuilder with sync proxy and ReAct loop"
```

---

### Task 9: AgentBuilder — Streaming Agent Proxy

**Files:**
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/agent/StreamingAgentInvocationHandler.java`
- Create: `agentic4j-core/src/main/java/com/agentic4j/core/agent/DefaultStreamingResponse.java`
- Modify: `agentic4j-core/src/main/java/com/agentic4j/core/agent/AgentBuilder.java`
- Test: `agentic4j-core/src/test/java/com/agentic4j/core/AgentBuilderStreamingTest.java`

- [ ] **Step 1: Write test for streaming agent proxy**

```java
package com.agentic4j.core;

import com.agentic4j.core.agent.AgentBuilder;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.memory.SlidingWindowMemory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class AgentBuilderStreamingTest {

    interface StreamAssistant {
        @SystemPrompt("You are a helpful assistant")
        StreamingResponse chat(String message);
    }

    public static class MockTool {
        @AgentTool("Test tool")
        public String doWork(@Param("input") String input) {
            return "Tool result for: " + input;
        }
    }

    @Test
    public void testStreamingSimpleResponse() throws Exception {
        StreamingChatModel mockStreamModel = new StreamingChatModel() {
            @Override
            public void send(ChatRequest request, StreamingResponseHandler handler) {
                handler.onToken("Hello ");
                handler.onToken("world!");
                handler.onComplete(new ChatResponse(
                        ChatMessage.assistant("Hello world!"),
                        new TokenUsage(10, 5, 15),
                        FinishReason.STOP
                ));
            }
        };

        StreamAssistant assistant = AgentBuilder.forInterface(StreamAssistant.class)
                .streamingChatModel(mockStreamModel)
                .memory(new SlidingWindowMemory(20))
                .build();

        final List<String> tokens = new ArrayList<String>();
        final ChatResponse[] finalResponse = new ChatResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);

        assistant.chat("Hi")
                .onToken(new java.util.function.Consumer<String>() {
                    @Override
                    public void accept(String token) {
                        tokens.add(token);
                    }
                })
                .onComplete(new java.util.function.Consumer<ChatResponse>() {
                    @Override
                    public void accept(ChatResponse response) {
                        finalResponse[0] = response;
                        latch.countDown();
                    }
                })
                .start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, tokens.size());
        assertEquals("Hello ", tokens.get(0));
        assertEquals("world!", tokens.get(1));
        assertEquals("Hello world!", finalResponse[0].getMessage().getContent());
    }

    @Test
    public void testStreamingWithToolExecution() throws Exception {
        final int[] callCount = {0};

        StreamingChatModel mockStreamModel = new StreamingChatModel() {
            @Override
            public void send(ChatRequest request, StreamingResponseHandler handler) {
                callCount[0]++;
                if (callCount[0] == 1) {
                    ToolCall toolCall = new ToolCall("call_1", "doWork", "{\"input\":\"test\"}");
                    handler.onComplete(new ChatResponse(
                            ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall)),
                            new TokenUsage(10, 5, 15),
                            FinishReason.TOOL_CALLS
                    ));
                } else {
                    handler.onToken("Done!");
                    handler.onComplete(new ChatResponse(
                            ChatMessage.assistant("Done!"),
                            new TokenUsage(10, 5, 15),
                            FinishReason.STOP
                    ));
                }
            }
        };

        StreamAssistant assistant = AgentBuilder.forInterface(StreamAssistant.class)
                .streamingChatModel(mockStreamModel)
                .memory(new SlidingWindowMemory(20))
                .tools(new MockTool())
                .build();

        final List<String> tokens = new ArrayList<String>();
        final List<ToolExecutionResult> toolResults = new ArrayList<ToolExecutionResult>();
        final CountDownLatch latch = new CountDownLatch(1);

        assistant.chat("Do work")
                .onToken(new java.util.function.Consumer<String>() {
                    @Override
                    public void accept(String token) {
                        tokens.add(token);
                    }
                })
                .onToolExecuted(new java.util.function.Consumer<ToolExecutionResult>() {
                    @Override
                    public void accept(ToolExecutionResult result) {
                        toolResults.add(result);
                    }
                })
                .onComplete(new java.util.function.Consumer<ChatResponse>() {
                    @Override
                    public void accept(ChatResponse response) {
                        latch.countDown();
                    }
                })
                .start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, toolResults.size());
        assertEquals("doWork", toolResults.get(0).getToolName());
        assertTrue(toolResults.get(0).getResult().contains("Tool result for: test"));
        assertEquals(1, tokens.size());
        assertEquals("Done!", tokens.get(0));
    }

    @Test
    public void testStreamingError() throws Exception {
        StreamingChatModel mockStreamModel = new StreamingChatModel() {
            @Override
            public void send(ChatRequest request, StreamingResponseHandler handler) {
                handler.onError(new RuntimeException("Connection failed"));
            }
        };

        StreamAssistant assistant = AgentBuilder.forInterface(StreamAssistant.class)
                .streamingChatModel(mockStreamModel)
                .memory(new SlidingWindowMemory(20))
                .build();

        final Throwable[] capturedError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);

        assistant.chat("test")
                .onError(new java.util.function.Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable error) {
                        capturedError[0] = error;
                        latch.countDown();
                    }
                })
                .start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Connection failed", capturedError[0].getMessage());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd agentic4j-core && mvn test -Dtest=AgentBuilderStreamingTest -pl . -q`
Expected: FAIL

- [ ] **Step 3: Implement DefaultStreamingResponse**

```java
package com.agentic4j.core.agent;

import com.agentic4j.core.*;
import com.agentic4j.core.annotation.SystemPrompt;
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
    private final String systemPrompt;
    private final String userMessage;

    private Consumer<String> tokenHandler;
    private Consumer<ToolExecutionResult> toolExecutedHandler;
    private Consumer<ChatResponse> completeHandler;
    private Consumer<Throwable> errorHandler;

    public DefaultStreamingResponse(StreamingChatModel streamingChatModel, ChatMemory memory,
                                     ToolExecutor toolExecutor, List<ToolDefinition> toolDefinitions,
                                     int maxToolIterations, String systemPrompt, String userMessage) {
        this.streamingChatModel = streamingChatModel;
        this.memory = memory;
        this.toolExecutor = toolExecutor;
        this.toolDefinitions = toolDefinitions;
        this.maxToolIterations = maxToolIterations;
        this.systemPrompt = systemPrompt;
        this.userMessage = userMessage;
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
        sendRequest(0);
    }

    private void sendRequest(final int iteration) {
        if (iteration >= maxToolIterations) {
            if (errorHandler != null) {
                errorHandler.accept(new MaxIterationsException(maxToolIterations));
            }
            return;
        }

        ChatRequest.Builder requestBuilder = ChatRequest.builder();
        if (systemPrompt != null) {
            requestBuilder.addMessage(ChatMessage.system(systemPrompt));
        }
        requestBuilder.addMessages(memory.messages());
        requestBuilder.addTools(toolDefinitions);

        streamingChatModel.send(requestBuilder.build(), new StreamingResponseHandler() {
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
                ChatMessage message = response.getMessage();
                if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                    memory.add(message);
                    for (ToolCall toolCall : message.getToolCalls()) {
                        ToolExecutionResult result = toolExecutor.execute(toolCall);
                        memory.add(ChatMessage.toolResult(toolCall.getId(), result.getResult()));
                        if (toolExecutedHandler != null) {
                            toolExecutedHandler.accept(result);
                        }
                    }
                    sendRequest(iteration + 1);
                } else {
                    memory.add(message);
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
```

- [ ] **Step 4: Implement StreamingAgentInvocationHandler**

```java
package com.agentic4j.core.agent;

import com.agentic4j.core.*;
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

    public StreamingAgentInvocationHandler(StreamingChatModel streamingChatModel, ChatMemory memory,
                                            ToolExecutor toolExecutor, List<ToolDefinition> toolDefinitions,
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
                streamingChatModel, memory, toolExecutor, toolDefinitions,
                maxToolIterations, systemPrompt, userMessage
        );
    }
}
```

- [ ] **Step 5: Update AgentBuilder.build() to support streaming**

Replace the `build()` method in `AgentBuilder.java`:

```java
    @SuppressWarnings("unchecked")
    public T build() {
        List<ToolDefinition> toolDefinitions = ToolScanner.scan(toolInstances);
        ToolExecutor toolExecutor = new ToolExecutor(toolInstances);

        java.lang.reflect.InvocationHandler handler;

        if (streamingChatModel != null && hasStreamingReturnType()) {
            handler = new StreamingAgentInvocationHandler(
                    streamingChatModel, memory, toolExecutor, toolDefinitions, maxToolIterations);
        } else if (chatModel != null) {
            handler = new AgentInvocationHandler(
                    chatModel, memory, toolExecutor, toolDefinitions, maxToolIterations);
        } else {
            throw new IllegalStateException("Either chatModel or streamingChatModel must be provided");
        }

        return (T) java.lang.reflect.Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                handler
        );
    }

    private boolean hasStreamingReturnType() {
        for (java.lang.reflect.Method method : interfaceClass.getMethods()) {
            if (StreamingResponse.class.isAssignableFrom(method.getReturnType())) {
                return true;
            }
        }
        return false;
    }
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd agentic4j-core && mvn test -pl . -q`
Expected: ALL tests PASS (ChatMessageTest, ChatRequestTest, SlidingWindowMemoryTest, ToolScannerTest, ToolExecutorTest, AgentBuilderSyncTest, AgentBuilderStreamingTest)

- [ ] **Step 7: Commit**

```bash
git add agentic4j-core/src
git commit -m "feat(core): add streaming agent proxy with ReAct loop"
```

---

### Task 10: OpenAI Request Mapper

**Files:**
- Create: `agentic4j-openai/src/main/java/com/agentic4j/openai/OpenAiRequestMapper.java`
- Test: `agentic4j-openai/src/test/java/com/agentic4j/openai/OpenAiRequestMapperTest.java`

- [ ] **Step 1: Write test for OpenAI JSON mapping**

```java
package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class OpenAiRequestMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testMapSimpleRequest() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.system("You are helpful"))
                .addMessage(ChatMessage.user("Hello"))
                .build();

        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("gpt-4o-mini", node.get("model").asText());
        assertEquals(0.0, node.get("temperature").asDouble(), 0.001);
        assertEquals(2, node.get("messages").size());
        assertEquals("system", node.get("messages").get(0).get("role").asText());
        assertEquals("You are helpful", node.get("messages").get(0).get("content").asText());
        assertEquals("user", node.get("messages").get(1).get("role").asText());
        assertEquals("Hello", node.get("messages").get(1).get("content").asText());
        assertFalse(node.has("stream"));
    }

    @Test
    public void testMapRequestWithStream() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Hi"))
                .build();

        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.5, null, true);
        JsonNode node = objectMapper.readTree(json);

        assertTrue(node.get("stream").asBoolean());
    }

    @Test
    public void testMapRequestWithTools() throws Exception {
        ToolParameter param = new ToolParameter("query", "The SQL query", "string", true);
        ToolDefinition tool = new ToolDefinition("executeSql", "Execute SQL", Collections.singletonList(param));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Run query"))
                .addTool(tool)
                .build();

        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);

        assertTrue(node.has("tools"));
        assertEquals(1, node.get("tools").size());
        JsonNode toolNode = node.get("tools").get(0);
        assertEquals("function", toolNode.get("type").asText());
        assertEquals("executeSql", toolNode.get("function").get("name").asText());
        assertEquals("Execute SQL", toolNode.get("function").get("description").asText());

        JsonNode paramsSchema = toolNode.get("function").get("parameters");
        assertEquals("object", paramsSchema.get("type").asText());
        assertTrue(paramsSchema.get("properties").has("query"));
        assertEquals("string", paramsSchema.get("properties").get("query").get("type").asText());
        assertTrue(paramsSchema.get("required").toString().contains("query"));
    }

    @Test
    public void testMapRequestWithMaxTokens() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Hi"))
                .build();

        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, 100, false);
        JsonNode node = objectMapper.readTree(json);

        assertEquals(100, node.get("max_tokens").asInt());
    }

    @Test
    public void testMapAssistantMessageWithToolCalls() throws Exception {
        ToolCall toolCall = new ToolCall("call_abc", "search", "{\"q\":\"test\"}");
        ChatMessage msg = ChatMessage.assistantWithToolCalls(Collections.singletonList(toolCall));

        ChatRequest request = ChatRequest.builder()
                .addMessage(msg)
                .build();

        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);

        JsonNode assistantNode = node.get("messages").get(0);
        assertEquals("assistant", assistantNode.get("role").asText());
        assertTrue(assistantNode.has("tool_calls"));
        assertEquals("call_abc", assistantNode.get("tool_calls").get(0).get("id").asText());
        assertEquals("function", assistantNode.get("tool_calls").get(0).get("type").asText());
        assertEquals("search", assistantNode.get("tool_calls").get(0).get("function").get("name").asText());
    }

    @Test
    public void testMapToolResultMessage() throws Exception {
        ChatMessage msg = ChatMessage.toolResult("call_abc", "Found 3 results");

        ChatRequest request = ChatRequest.builder()
                .addMessage(msg)
                .build();

        String json = OpenAiRequestMapper.toJson(request, "gpt-4o-mini", 0.0, null, false);
        JsonNode node = objectMapper.readTree(json);

        JsonNode toolNode = node.get("messages").get(0);
        assertEquals("tool", toolNode.get("role").asText());
        assertEquals("Found 3 results", toolNode.get("content").asText());
        assertEquals("call_abc", toolNode.get("tool_call_id").asText());
    }

    @Test
    public void testParseSimpleResponse() throws Exception {
        String responseJson = "{"
                + "\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello!\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}"
                + "}";

        ChatResponse response = OpenAiRequestMapper.parseResponse(responseJson);
        assertEquals("Hello!", response.getMessage().getContent());
        assertEquals(Role.ASSISTANT, response.getMessage().getRole());
        assertEquals(FinishReason.STOP, response.getFinishReason());
        assertEquals(10, response.getUsage().getPromptTokens());
        assertEquals(5, response.getUsage().getCompletionTokens());
        assertEquals(15, response.getUsage().getTotalTokens());
    }

    @Test
    public void testParseResponseWithToolCalls() throws Exception {
        String responseJson = "{"
                + "\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                + "\"tool_calls\":[{\"id\":\"call_123\",\"type\":\"function\","
                + "\"function\":{\"name\":\"search\",\"arguments\":\"{\\\"term\\\":\\\"milk\\\"}\"}}]},"
                + "\"finish_reason\":\"tool_calls\"}],"
                + "\"usage\":{\"prompt_tokens\":20,\"completion_tokens\":10,\"total_tokens\":30}"
                + "}";

        ChatResponse response = OpenAiRequestMapper.parseResponse(responseJson);
        assertNotNull(response.getMessage().getToolCalls());
        assertEquals(1, response.getMessage().getToolCalls().size());
        assertEquals("call_123", response.getMessage().getToolCalls().get(0).getId());
        assertEquals("search", response.getMessage().getToolCalls().get(0).getName());
        assertEquals("{\"term\":\"milk\"}", response.getMessage().getToolCalls().get(0).getArguments());
        assertEquals(FinishReason.TOOL_CALLS, response.getFinishReason());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl agentic4j-openai -Dtest=OpenAiRequestMapperTest -q`
Expected: FAIL

- [ ] **Step 3: Implement OpenAiRequestMapper**

```java
package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenAiRequestMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(ChatRequest request, String model, double temperature,
                                 Integer maxTokens, boolean stream) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("temperature", temperature);

        if (maxTokens != null) {
            root.put("max_tokens", maxTokens);
        }
        if (stream) {
            root.put("stream", true);
        }

        ArrayNode messagesNode = root.putArray("messages");
        for (ChatMessage msg : request.getMessages()) {
            ObjectNode msgNode = messagesNode.addObject();
            msgNode.put("role", msg.getRole().name().toLowerCase());

            if (msg.getContent() != null) {
                msgNode.put("content", msg.getContent());
            } else {
                msgNode.putNull("content");
            }

            if (msg.getToolCallId() != null) {
                msgNode.put("tool_call_id", msg.getToolCallId());
            }

            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                ArrayNode toolCallsNode = msgNode.putArray("tool_calls");
                for (ToolCall tc : msg.getToolCalls()) {
                    ObjectNode tcNode = toolCallsNode.addObject();
                    tcNode.put("id", tc.getId());
                    tcNode.put("type", "function");
                    ObjectNode fnNode = tcNode.putObject("function");
                    fnNode.put("name", tc.getName());
                    fnNode.put("arguments", tc.getArguments());
                }
            }
        }

        if (!request.getTools().isEmpty()) {
            ArrayNode toolsNode = root.putArray("tools");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode toolNode = toolsNode.addObject();
                toolNode.put("type", "function");
                ObjectNode fnNode = toolNode.putObject("function");
                fnNode.put("name", tool.getName());
                fnNode.put("description", tool.getDescription());

                ObjectNode paramsNode = fnNode.putObject("parameters");
                paramsNode.put("type", "object");
                ObjectNode propsNode = paramsNode.putObject("properties");
                ArrayNode requiredNode = paramsNode.putArray("required");

                for (ToolParameter param : tool.getParameters()) {
                    ObjectNode paramNode = propsNode.putObject(param.getName());
                    paramNode.put("type", param.getType());
                    paramNode.put("description", param.getDescription());
                    if (param.isRequired()) {
                        requiredNode.add(param.getName());
                    }
                }
            }
        }

        return root.toString();
    }

    public static ChatResponse parseResponse(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        JsonNode choice = root.get("choices").get(0);
        JsonNode messageNode = choice.get("message");

        String role = messageNode.get("role").asText();
        String content = messageNode.has("content") && !messageNode.get("content").isNull()
                ? messageNode.get("content").asText() : null;

        List<ToolCall> toolCalls = null;
        if (messageNode.has("tool_calls") && messageNode.get("tool_calls").isArray()) {
            toolCalls = new ArrayList<ToolCall>();
            for (JsonNode tcNode : messageNode.get("tool_calls")) {
                String id = tcNode.get("id").asText();
                String name = tcNode.get("function").get("name").asText();
                String arguments = tcNode.get("function").get("arguments").asText();
                toolCalls.add(new ToolCall(id, name, arguments));
            }
        }

        ChatMessage message;
        if (toolCalls != null && !toolCalls.isEmpty()) {
            message = ChatMessage.assistantWithToolCalls(toolCalls);
        } else {
            message = ChatMessage.assistant(content != null ? content : "");
        }

        FinishReason finishReason = parseFinishReason(choice.get("finish_reason").asText());

        TokenUsage usage = new TokenUsage(0, 0, 0);
        if (root.has("usage")) {
            JsonNode usageNode = root.get("usage");
            usage = new TokenUsage(
                    usageNode.get("prompt_tokens").asInt(),
                    usageNode.get("completion_tokens").asInt(),
                    usageNode.get("total_tokens").asInt()
            );
        }

        return new ChatResponse(message, usage, finishReason);
    }

    static FinishReason parseFinishReason(String reason) {
        if (reason == null) return FinishReason.STOP;
        switch (reason) {
            case "stop": return FinishReason.STOP;
            case "tool_calls": return FinishReason.TOOL_CALLS;
            case "length": return FinishReason.LENGTH;
            case "content_filter": return FinishReason.CONTENT_FILTER;
            default: return FinishReason.STOP;
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl agentic4j-openai -Dtest=OpenAiRequestMapperTest -q`
Expected: All 8 tests PASS

- [ ] **Step 5: Commit**

```bash
git add agentic4j-openai/src
git commit -m "feat(openai): add OpenAiRequestMapper for JSON serialization"
```

---

### Task 11: OpenAI Sync Chat Model

**Files:**
- Create: `agentic4j-openai/src/main/java/com/agentic4j/openai/OpenAiChatModel.java`
- Test: `agentic4j-openai/src/test/java/com/agentic4j/openai/OpenAiChatModelTest.java`

- [ ] **Step 1: Write test using MockWebServer**

```java
package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.agentic4j.core.exception.ModelException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class OpenAiChatModelTest {

    private MockWebServer server;
    private OpenAiChatModel model;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        model = OpenAiChatModel.builder()
                .apiKey("test-key")
                .baseUrl(server.url("/v1").toString())
                .modelName("gpt-4o-mini")
                .temperature(0.0)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testSimpleChat() throws Exception {
        String responseBody = "{"
                + "\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello!\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}"
                + "}";

        server.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Hi"))
                .build();

        ChatResponse response = model.send(request);

        assertEquals("Hello!", response.getMessage().getContent());
        assertEquals(FinishReason.STOP, response.getFinishReason());
        assertEquals(15, response.getUsage().getTotalTokens());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getPath().contains("/chat/completions"));
        assertTrue(recorded.getHeader("Authorization").contains("test-key"));
        assertTrue(recorded.getHeader("Content-Type").contains("application/json"));
    }

    @Test
    public void testResponseWithToolCalls() throws Exception {
        String responseBody = "{"
                + "\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                + "\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\","
                + "\"function\":{\"name\":\"search\",\"arguments\":\"{\\\"term\\\":\\\"milk\\\"}\"}}]},"
                + "\"finish_reason\":\"tool_calls\"}],"
                + "\"usage\":{\"prompt_tokens\":20,\"completion_tokens\":10,\"total_tokens\":30}"
                + "}";

        server.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Find milk"))
                .build();

        ChatResponse response = model.send(request);

        assertEquals(FinishReason.TOOL_CALLS, response.getFinishReason());
        assertNotNull(response.getMessage().getToolCalls());
        assertEquals(1, response.getMessage().getToolCalls().size());
        assertEquals("search", response.getMessage().getToolCalls().get(0).getName());
    }

    @Test
    public void testAuthError() {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Hi"))
                .build();

        try {
            model.send(request);
            fail("Should throw ModelException");
        } catch (ModelException e) {
            assertEquals(401, e.getStatusCode());
            assertTrue(e.getResponseBody().contains("Invalid API key"));
        }
    }

    @Test
    public void testRateLimitError() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setBody("{\"error\":{\"message\":\"Rate limit exceeded\"}}"));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Hi"))
                .build();

        try {
            model.send(request);
            fail("Should throw ModelException");
        } catch (ModelException e) {
            assertEquals(429, e.getStatusCode());
        }
    }

    @Test
    public void testRequestIncludesTools() throws Exception {
        String responseBody = "{"
                + "\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"OK\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}"
                + "}";

        server.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json"));

        ToolParameter param = new ToolParameter("query", "SQL query", "string", true);
        ToolDefinition tool = new ToolDefinition("executeSql", "Run SQL", Collections.singletonList(param));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Run query"))
                .addTool(tool)
                .build();

        model.send(request);

        RecordedRequest recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("\"tools\""));
        assertTrue(body.contains("executeSql"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl agentic4j-openai -Dtest=OpenAiChatModelTest -q`
Expected: FAIL

- [ ] **Step 3: Implement OpenAiChatModel**

```java
package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.agentic4j.core.exception.ModelException;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class OpenAiChatModel implements ChatModel {

    private static final Logger LOG = Logger.getLogger(OpenAiChatModel.class.getName());
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final double temperature;
    private final Integer maxTokens;
    private final boolean logRequests;
    private final boolean logResponses;
    private final OkHttpClient httpClient;

    private OpenAiChatModel(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public ChatResponse send(ChatRequest request) {
        String json = OpenAiRequestMapper.toJson(request, modelName, temperature, maxTokens, false);

        if (logRequests) {
            LOG.info("Request: " + json);
        }

        String url = baseUrl.endsWith("/")
                ? baseUrl + "chat/completions"
                : baseUrl + "/chat/completions";

        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON_TYPE, json))
                .build();

        try {
            Response httpResponse = httpClient.newCall(httpRequest).execute();
            String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";

            if (!httpResponse.isSuccessful()) {
                throw new ModelException(httpResponse.code(),
                        "OpenAI API error (HTTP " + httpResponse.code() + ")", responseBody);
            }

            if (logResponses) {
                LOG.info("Response: " + responseBody);
            }

            return OpenAiRequestMapper.parseResponse(responseBody);
        } catch (ModelException e) {
            throw e;
        } catch (IOException e) {
            throw new ModelException(0, "HTTP request failed: " + e.getMessage(), "");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "gpt-4o-mini";
        private double temperature = 0.7;
        private Integer maxTokens;
        private long timeoutSeconds = 60;
        private boolean logRequests = false;
        private boolean logResponses = false;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder timeout(long seconds) {
            this.timeoutSeconds = seconds;
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
                throw new IllegalArgumentException("apiKey is required");
            }
            return new OpenAiChatModel(this);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl agentic4j-openai -Dtest=OpenAiChatModelTest -q`
Expected: All 5 tests PASS

- [ ] **Step 5: Commit**

```bash
git add agentic4j-openai/src
git commit -m "feat(openai): add OpenAiChatModel with sync HTTP client"
```

---

### Task 12: OpenAI Streaming Chat Model

**Files:**
- Create: `agentic4j-openai/src/main/java/com/agentic4j/openai/OpenAiStreamingChatModel.java`
- Test: `agentic4j-openai/src/test/java/com/agentic4j/openai/OpenAiStreamingChatModelTest.java`

- [ ] **Step 1: Write test for streaming model**

```java
package com.agentic4j.openai;

import com.agentic4j.core.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OpenAiStreamingChatModelTest {

    private MockWebServer server;
    private OpenAiStreamingChatModel model;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        model = OpenAiStreamingChatModel.builder()
                .apiKey("test-key")
                .baseUrl(server.url("/v1").toString())
                .modelName("gpt-4o-mini")
                .temperature(0.0)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testStreamTokens() throws Exception {
        String sseBody = "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":\"Hello\"},\"finish_reason\":null}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\" world\"},\"finish_reason\":null}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"!\"},\"finish_reason\":null}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":3,\"total_tokens\":8}}\n\n"
                + "data: [DONE]\n\n";

        server.enqueue(new MockResponse()
                .setBody(sseBody)
                .setHeader("Content-Type", "text/event-stream"));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Hi"))
                .build();

        final List<String> tokens = new ArrayList<String>();
        final ChatResponse[] finalResponse = new ChatResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);

        model.send(request, new StreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                tokens.add(token);
            }

            @Override
            public void onToolExecution(ToolExecutionResult result) {
            }

            @Override
            public void onComplete(ChatResponse response) {
                finalResponse[0] = response;
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, tokens.size());
        assertEquals("Hello", tokens.get(0));
        assertEquals(" world", tokens.get(1));
        assertEquals("!", tokens.get(2));
        assertNotNull(finalResponse[0]);
        assertEquals("Hello world!", finalResponse[0].getMessage().getContent());
        assertEquals(FinishReason.STOP, finalResponse[0].getFinishReason());
    }

    @Test
    public void testStreamWithToolCalls() throws Exception {
        String sseBody = "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"search\",\"arguments\":\"\"}}]},\"finish_reason\":null}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"term\"}}]},\"finish_reason\":null}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\":\\\"milk\\\"}\"}}]},\"finish_reason\":null}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"tool_calls\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}\n\n"
                + "data: [DONE]\n\n";

        server.enqueue(new MockResponse()
                .setBody(sseBody)
                .setHeader("Content-Type", "text/event-stream"));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Find milk"))
                .build();

        final ChatResponse[] finalResponse = new ChatResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);

        model.send(request, new StreamingResponseHandler() {
            @Override
            public void onToken(String token) {
            }

            @Override
            public void onToolExecution(ToolExecutionResult result) {
            }

            @Override
            public void onComplete(ChatResponse response) {
                finalResponse[0] = response;
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(finalResponse[0]);
        assertEquals(FinishReason.TOOL_CALLS, finalResponse[0].getFinishReason());
        assertNotNull(finalResponse[0].getMessage().getToolCalls());
        assertEquals(1, finalResponse[0].getMessage().getToolCalls().size());
        assertEquals("call_1", finalResponse[0].getMessage().getToolCalls().get(0).getId());
        assertEquals("search", finalResponse[0].getMessage().getToolCalls().get(0).getName());
        assertEquals("{\"term\":\"milk\"}", finalResponse[0].getMessage().getToolCalls().get(0).getArguments());
    }

    @Test
    public void testStreamHttpError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));

        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user("Hi"))
                .build();

        final Throwable[] capturedError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);

        model.send(request, new StreamingResponseHandler() {
            @Override
            public void onToken(String token) {
            }

            @Override
            public void onToolExecution(ToolExecutionResult result) {
            }

            @Override
            public void onComplete(ChatResponse response) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                capturedError[0] = error;
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(capturedError[0]);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl agentic4j-openai -Dtest=OpenAiStreamingChatModelTest -q`
Expected: FAIL

- [ ] **Step 3: Implement OpenAiStreamingChatModel**

```java
package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.agentic4j.core.exception.ModelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class OpenAiStreamingChatModel implements StreamingChatModel {

    private static final Logger LOG = Logger.getLogger(OpenAiStreamingChatModel.class.getName());
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final double temperature;
    private final Integer maxTokens;
    private final boolean logRequests;
    private final boolean logResponses;
    private final OkHttpClient httpClient;

    private OpenAiStreamingChatModel(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void send(ChatRequest request, StreamingResponseHandler handler) {
        String json = OpenAiRequestMapper.toJson(request, modelName, temperature, maxTokens, true);

        if (logRequests) {
            LOG.info("Streaming request: " + json);
        }

        String url = baseUrl.endsWith("/")
                ? baseUrl + "chat/completions"
                : baseUrl + "/chat/completions";

        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON_TYPE, json))
                .build();

        try {
            Response httpResponse = httpClient.newCall(httpRequest).execute();

            if (!httpResponse.isSuccessful()) {
                String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "";
                handler.onError(new ModelException(httpResponse.code(),
                        "OpenAI API error (HTTP " + httpResponse.code() + ")", errorBody));
                return;
            }

            ResponseBody body = httpResponse.body();
            if (body == null) {
                handler.onError(new ModelException(0, "Empty response body", ""));
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream(), "UTF-8"));
            StringBuilder contentBuilder = new StringBuilder();
            Map<Integer, ToolCallAccumulator> toolCallAccumulators = new TreeMap<Integer, ToolCallAccumulator>();
            FinishReason finishReason = FinishReason.STOP;
            TokenUsage usage = new TokenUsage(0, 0, 0);

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();
                if (data.equals("[DONE]")) break;

                JsonNode chunk = MAPPER.readTree(data);
                JsonNode choices = chunk.get("choices");
                if (choices == null || choices.size() == 0) continue;

                JsonNode choice = choices.get(0);
                JsonNode delta = choice.get("delta");

                if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                    finishReason = OpenAiRequestMapper.parseFinishReason(choice.get("finish_reason").asText());
                }

                if (chunk.has("usage")) {
                    JsonNode usageNode = chunk.get("usage");
                    usage = new TokenUsage(
                            usageNode.get("prompt_tokens").asInt(),
                            usageNode.get("completion_tokens").asInt(),
                            usageNode.get("total_tokens").asInt()
                    );
                }

                if (delta == null) continue;

                if (delta.has("content") && !delta.get("content").isNull()) {
                    String token = delta.get("content").asText();
                    contentBuilder.append(token);
                    handler.onToken(token);
                }

                if (delta.has("tool_calls")) {
                    for (JsonNode tcDelta : delta.get("tool_calls")) {
                        int index = tcDelta.get("index").asInt();
                        ToolCallAccumulator acc = toolCallAccumulators.get(index);
                        if (acc == null) {
                            acc = new ToolCallAccumulator();
                            toolCallAccumulators.put(index, acc);
                        }
                        if (tcDelta.has("id")) {
                            acc.id = tcDelta.get("id").asText();
                        }
                        if (tcDelta.has("function")) {
                            JsonNode fn = tcDelta.get("function");
                            if (fn.has("name")) {
                                acc.name = fn.get("name").asText();
                            }
                            if (fn.has("arguments")) {
                                acc.arguments.append(fn.get("arguments").asText());
                            }
                        }
                    }
                }
            }

            reader.close();

            ChatMessage message;
            if (!toolCallAccumulators.isEmpty()) {
                List<ToolCall> toolCalls = new ArrayList<ToolCall>();
                for (ToolCallAccumulator acc : toolCallAccumulators.values()) {
                    toolCalls.add(new ToolCall(acc.id, acc.name, acc.arguments.toString()));
                }
                message = ChatMessage.assistantWithToolCalls(toolCalls);
            } else {
                message = ChatMessage.assistant(contentBuilder.toString());
            }

            handler.onComplete(new ChatResponse(message, usage, finishReason));

        } catch (IOException e) {
            handler.onError(new ModelException(0, "HTTP request failed: " + e.getMessage(), ""));
        }
    }

    private static class ToolCallAccumulator {
        String id;
        String name;
        StringBuilder arguments = new StringBuilder();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "gpt-4o-mini";
        private double temperature = 0.7;
        private Integer maxTokens;
        private long timeoutSeconds = 60;
        private boolean logRequests = false;
        private boolean logResponses = false;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder timeout(long seconds) {
            this.timeoutSeconds = seconds;
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
                throw new IllegalArgumentException("apiKey is required");
            }
            return new OpenAiStreamingChatModel(this);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl agentic4j-openai -Dtest=OpenAiStreamingChatModelTest -q`
Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add agentic4j-openai/src
git commit -m "feat(openai): add OpenAiStreamingChatModel with SSE parsing"
```

---

### Task 13: Spring Boot Starter

**Files:**
- Create: `agentic4j-spring-boot-starter/src/main/java/com/agentic4j/spring/Agentic4jProperties.java`
- Create: `agentic4j-spring-boot-starter/src/main/java/com/agentic4j/spring/Agentic4jAutoConfiguration.java`
- Create: `agentic4j-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- Test: `agentic4j-spring-boot-starter/src/test/java/com/agentic4j/spring/Agentic4jAutoConfigurationTest.java`

- [ ] **Step 1: Write test for auto-configuration**

```java
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
                .withPropertyValues(
                        "agentic4j.openai.api-key=test-key",
                        "agentic4j.openai.model-name=gpt-4o-mini"
                )
                .run(context -> {
                    assertTrue(context.containsBean("openAiChatModel"));
                    assertTrue(context.containsBean("openAiStreamingChatModel"));
                    assertNotNull(context.getBean(OpenAiChatModel.class));
                    assertNotNull(context.getBean(OpenAiStreamingChatModel.class));
                });
    }

    @Test
    public void testNoBeansWithoutApiKey() {
        contextRunner
                .run(context -> {
                    assertFalse(context.containsBean("openAiChatModel"));
                    assertFalse(context.containsBean("openAiStreamingChatModel"));
                });
    }

    @Test
    public void testCustomBaseUrl() {
        contextRunner
                .withPropertyValues(
                        "agentic4j.openai.api-key=test-key",
                        "agentic4j.openai.base-url=https://openrouter.ai/api/v1",
                        "agentic4j.openai.model-name=openai/gpt-4o-mini",
                        "agentic4j.openai.temperature=0.0"
                )
                .run(context -> {
                    assertTrue(context.containsBean("openAiChatModel"));
                });
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl agentic4j-spring-boot-starter -Dtest=Agentic4jAutoConfigurationTest -q`
Expected: FAIL

- [ ] **Step 3: Implement Agentic4jProperties**

```java
package com.agentic4j.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentic4j")
public class Agentic4jProperties {

    private OpenAiProperties openai = new OpenAiProperties();

    public OpenAiProperties getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAiProperties openai) {
        this.openai = openai;
    }

    public static class OpenAiProperties {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "gpt-4o-mini";
        private double temperature = 0.7;
        private Integer maxTokens;
        private long timeout = 60;
        private boolean logRequests = false;
        private boolean logResponses = false;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public boolean isLogRequests() {
            return logRequests;
        }

        public void setLogRequests(boolean logRequests) {
            this.logRequests = logRequests;
        }

        public boolean isLogResponses() {
            return logResponses;
        }

        public void setLogResponses(boolean logResponses) {
            this.logResponses = logResponses;
        }
    }
}
```

- [ ] **Step 4: Implement Agentic4jAutoConfiguration**

```java
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
        return builder.build();
    }
}
```

- [ ] **Step 5: Create spring.factories**

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.agentic4j.spring.Agentic4jAutoConfiguration
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn test -pl agentic4j-spring-boot-starter -Dtest=Agentic4jAutoConfigurationTest -q`
Expected: All 3 tests PASS

- [ ] **Step 7: Commit**

```bash
git add agentic4j-spring-boot-starter/src
git commit -m "feat(spring): add Spring Boot 2.7.x auto-configuration starter"
```

---

### Task 14: Full Build Verification

**Files:** None new — this is a verification task.

- [ ] **Step 1: Run full build with all tests**

Run: `mvn clean verify`
Expected: BUILD SUCCESS, all modules compile and all tests pass

- [ ] **Step 2: Verify Java 8 compatibility**

Run: `mvn compile -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8`
Expected: BUILD SUCCESS — no Java 9+ language features used

- [ ] **Step 3: Verify no unexpected dependencies in core**

Run: `mvn dependency:tree -pl agentic4j-core`
Expected: Only JDK classes + test dependencies (junit, mockito). No OkHttp, no Jackson, no Spring.

- [ ] **Step 4: Commit**

```bash
git commit --allow-empty -m "chore: verify full build passes with Java 8 compatibility"
```

---

### Task 15: Integration Smoke Test

**Files:**
- Create: `agentic4j-openai/src/test/java/com/agentic4j/openai/IntegrationSmokeTest.java`

This test verifies the full stack works end-to-end: AgentBuilder + OpenAI model + tool execution + ReAct loop, all using MockWebServer (no real API calls).

- [ ] **Step 1: Write integration test**

```java
package com.agentic4j.openai;

import com.agentic4j.core.*;
import com.agentic4j.core.agent.AgentBuilder;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.memory.SlidingWindowMemory;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class IntegrationSmokeTest {

    private MockWebServer server;

    public interface PriceAssistant {
        @SystemPrompt("You are a price data assistant")
        String chat(String message);
    }

    public static class MockSearchTool {
        @AgentTool("Search for products by name")
        public String searchProducts(@Param("The search term") String term) {
            if (term.toLowerCase().contains("milk")) {
                return "Found: Fresh Milk (ID: 1), Milk Powder (ID: 2)";
            }
            return "No products found for: " + term;
        }
    }

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testFullReActLoopWithMockServer() throws Exception {
        final AtomicInteger requestCount = new AtomicInteger(0);

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                int count = requestCount.incrementAndGet();
                if (count == 1) {
                    // First request: model calls the search tool
                    return new MockResponse()
                            .setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                                    + "\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\","
                                    + "\"function\":{\"name\":\"searchProducts\","
                                    + "\"arguments\":\"{\\\"term\\\":\\\"milk\\\"}\"}}]},"
                                    + "\"finish_reason\":\"tool_calls\"}],"
                                    + "\"usage\":{\"prompt_tokens\":50,\"completion_tokens\":20,\"total_tokens\":70}}")
                            .setHeader("Content-Type", "application/json");
                } else {
                    // Second request: model gives final answer after seeing tool result
                    return new MockResponse()
                            .setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\","
                                    + "\"content\":\"I found 2 milk products: Fresh Milk and Milk Powder.\"},"
                                    + "\"finish_reason\":\"stop\"}],"
                                    + "\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":20,\"total_tokens\":120}}")
                            .setHeader("Content-Type", "application/json");
                }
            }
        });

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("test-key")
                .baseUrl(server.url("/v1").toString())
                .modelName("gpt-4o-mini")
                .temperature(0.0)
                .build();

        PriceAssistant assistant = AgentBuilder.forInterface(PriceAssistant.class)
                .chatModel(chatModel)
                .memory(new SlidingWindowMemory(20))
                .tools(new MockSearchTool())
                .build();

        String response = assistant.chat("What milk products do you have?");

        assertEquals("I found 2 milk products: Fresh Milk and Milk Powder.", response);
        assertEquals(2, requestCount.get());
    }
}
```

- [ ] **Step 2: Run integration test**

Run: `mvn test -pl agentic4j-openai -Dtest=IntegrationSmokeTest -q`
Expected: PASS

- [ ] **Step 3: Run all tests across all modules**

Run: `mvn test`
Expected: All tests pass across all 3 modules

- [ ] **Step 4: Commit**

```bash
git add agentic4j-openai/src/test
git commit -m "test: add end-to-end integration smoke test"
```

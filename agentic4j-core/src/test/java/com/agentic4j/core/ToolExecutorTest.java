package com.agentic4j.core;

import com.agentic4j.core.agent.ToolExecutor;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolExecutorTest {
    static class SearchTool {
        @AgentTool("Search for products")
        public String searchProducts(@Param("search term") String term) { return "Found: " + term; }
    }
    static class MultiParamTool {
        @AgentTool("Run query")
        public String runQuery(@Param("the query") String sql, @Param("max rows") int limit) { return "Query: " + sql + ", limit: " + limit; }
    }
    static class ErrorTool {
        @AgentTool("Fails always")
        public String failingTool(@Param("input") String input) { throw new RuntimeException("DB connection lost"); }
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
        assertEquals("Found: rice", executor.execute(new ToolCall("c1", "searchProducts", "{\"term\":\"rice\"}")).getResult());
        assertEquals("Query: SELECT 1, limit: 5", executor.execute(new ToolCall("c2", "runQuery", "{\"sql\":\"SELECT 1\",\"limit\":5}")).getResult());
    }
}

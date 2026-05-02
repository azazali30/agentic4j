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
        public String search(@Param("The search query") String query) { return "found"; }
    }
    static class MultiParamTool {
        @AgentTool("Execute a query with limit")
        public String query(@Param("The SQL query") String sql, @Param("Max rows") int limit) { return "result"; }
    }
    static class NoToolMethods {
        public String notATool(String input) { return input; }
    }
    static class MixedTool {
        @AgentTool("Tool method")
        public String toolMethod(@Param("input") String input) { return input; }
        public String regularMethod(String input) { return input; }
    }
    static class TypesTool {
        @AgentTool("Test types")
        public String testTypes(@Param("a string") String s, @Param("an int") int i, @Param("a double") double d, @Param("a boolean") boolean b) { return "ok"; }
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
        assertEquals(2, defs.get(0).getParameters().size());
        assertEquals("string", defs.get(0).getParameters().get(0).getType());
        assertEquals("integer", defs.get(0).getParameters().get(1).getType());
    }
    @Test
    public void testScanNoToolMethods() {
        assertTrue(ToolScanner.scan(new NoToolMethods()).isEmpty());
    }
    @Test
    public void testScanMixedTool() {
        List<ToolDefinition> defs = ToolScanner.scan(new MixedTool());
        assertEquals(1, defs.size());
        assertEquals("toolMethod", defs.get(0).getName());
    }
    @Test
    public void testScanMultipleInstances() {
        assertEquals(2, ToolScanner.scan(new SimpleStringTool(), new MultiParamTool()).size());
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

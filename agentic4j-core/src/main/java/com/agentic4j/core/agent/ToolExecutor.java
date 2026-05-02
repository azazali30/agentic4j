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
                    method.setAccessible(true);
                    toolMethods.put(method.getName(), new ToolMethod(instance, method));
                }
            }
        }
    }

    public ToolExecutionResult execute(ToolCall call) {
        ToolMethod toolMethod = toolMethods.get(call.getName());
        if (toolMethod == null) {
            return new ToolExecutionResult(call.getId(), call.getName(), "Error: Unknown tool '" + call.getName() + "'");
        }
        try {
            Map<String, String> args = parseJsonArguments(call.getArguments());
            Object[] params = resolveParameters(toolMethod.method, args);
            Object result = toolMethod.method.invoke(toolMethod.instance, params);
            return new ToolExecutionResult(call.getId(), call.getName(), result != null ? result.toString() : "null");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return new ToolExecutionResult(call.getId(), call.getName(), "Error: " + cause.getMessage());
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
        if (json == null || json.trim().isEmpty() || json.trim().equals("{}")) return result;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        int i = 0;
        while (i < trimmed.length()) {
            while (i < trimmed.length() && Character.isWhitespace(trimmed.charAt(i))) i++;
            if (i >= trimmed.length()) break;
            if (trimmed.charAt(i) != '"') { i++; continue; }
            i++;
            int keyStart = i;
            while (i < trimmed.length() && trimmed.charAt(i) != '"') i++;
            String key = trimmed.substring(keyStart, i);
            i++;
            while (i < trimmed.length() && (trimmed.charAt(i) == ':' || Character.isWhitespace(trimmed.charAt(i)))) i++;
            String value;
            if (i < trimmed.length() && trimmed.charAt(i) == '"') {
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < trimmed.length() && trimmed.charAt(i) != '"') {
                    if (trimmed.charAt(i) == '\\' && i + 1 < trimmed.length()) { i++; sb.append(trimmed.charAt(i)); }
                    else { sb.append(trimmed.charAt(i)); }
                    i++;
                }
                value = sb.toString();
                i++;
            } else {
                int valueStart = i;
                while (i < trimmed.length() && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}') i++;
                value = trimmed.substring(valueStart, i).trim();
            }
            result.put(key, value);
            while (i < trimmed.length() && (trimmed.charAt(i) == ',' || Character.isWhitespace(trimmed.charAt(i)))) i++;
        }
        return result;
    }

    private static class ToolMethod {
        final Object instance;
        final Method method;
        ToolMethod(Object instance, Method method) { this.instance = instance; this.method = method; }
    }
}

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
                if (annotation == null) continue;
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

    private static Param findParamAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Param) return (Param) annotation;
        }
        return null;
    }

    static String mapType(Class<?> type) {
        String mapped = TYPE_MAP.get(type);
        return mapped != null ? mapped : "string";
    }
}

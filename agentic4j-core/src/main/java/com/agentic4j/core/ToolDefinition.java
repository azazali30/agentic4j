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

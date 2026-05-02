package com.agentic4j.core.exception;

public class MaxIterationsException extends Agentic4jException {

    private final int maxIterations;

    public MaxIterationsException(int maxIterations) {
        super("Agent exceeded maximum iterations: " + maxIterations);
        this.maxIterations = maxIterations;
    }

    public int getMaxIterations() {
        return maxIterations;
    }
}

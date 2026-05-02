package com.agentic4j.core.exception;

public class Agentic4jException extends RuntimeException {

    public Agentic4jException(String message) {
        super(message);
    }

    public Agentic4jException(String message, Throwable cause) {
        super(message, cause);
    }
}

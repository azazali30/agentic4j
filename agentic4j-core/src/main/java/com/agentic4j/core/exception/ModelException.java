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

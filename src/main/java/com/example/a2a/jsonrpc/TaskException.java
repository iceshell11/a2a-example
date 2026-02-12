package com.example.a2a.jsonrpc;

public class TaskException extends RuntimeException {
    private final int code;

    public TaskException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}

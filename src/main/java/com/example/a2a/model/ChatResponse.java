package com.example.a2a.model;

import java.util.List;

public class ChatResponse {
    private final String content;
    private final List<String> toolCalls;

    public ChatResponse(String content, List<String> toolCalls) {
        this.content = content;
        this.toolCalls = toolCalls;
    }

    public String getContent() {
        return content;
    }

    public List<String> getToolCalls() {
        return toolCalls;
    }
}

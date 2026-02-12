package com.example.a2a.controller;

import com.example.a2a.service.StreamingService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StreamingController {

    private static final Logger logger = LoggerFactory.getLogger(StreamingController.class);

    private final StreamingService streamingService;

    public StreamingController(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleStreamingRequest(@RequestBody JsonNode request) {
        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        String id = request.path("id").asText();

        logger.info("SSE streaming request: method={}, id={}", method, id);

        String taskId = params.path("id").asText();
        String messageText = extractText(params.path("message"));

        return switch (method) {
            case "message/stream" -> streamingService.streamMessage(taskId, messageText);
            case "tasks/subscribe" -> streamingService.subscribeToTask(taskId);
            default -> {
                SseEmitter emitter = new SseEmitter();
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Method not supported for streaming: " + method));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
                yield emitter;
            }
        };
    }

    private String extractText(JsonNode params) {
        if (!params.has("message")) {
            return "";
        }
        
        JsonNode message = params.get("message");
        if (!message.has("parts")) {
            return "";
        }
        
        StringBuilder text = new StringBuilder();
        JsonNode parts = message.get("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if ("text".equals(part.path("type").asText()) || "text".equals(part.path("kind").asText())) {
                    text.append(part.path("text").asText());
                }
            }
        }
        return text.toString();
    }
}

package com.example.a2a.controller;

import com.example.a2a.service.WeatherService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class A2aRpcController {

    private static final Logger logger = LoggerFactory.getLogger(A2aRpcController.class);
    private static final String JSONRPC_VERSION = "2.0";

    private final WeatherService weatherService;
    private final ObjectMapper objectMapper;
    private final Map<String, TaskInfo> tasks = new ConcurrentHashMap<>();

    public A2aRpcController(WeatherService weatherService, ObjectMapper objectMapper) {
        this.weatherService = weatherService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> handleJsonRpc(@RequestBody JsonNode request) {
        try {
            String method = request.path("method").asText();
            JsonNode params = request.path("params");
            String id = request.path("id").asText();

            logger.info("JSON-RPC request: method={}, id={}", method, id);

            JsonNode result = switch (method) {
                case "tasks/send" -> handleTasksSend(params);
                case "tasks/get" -> handleTasksGet(params);
                case "tasks/cancel" -> handleTasksCancel(params);
                case "tasks/sendSubscribe" -> handleTasksSend(params);
                default -> throw new A2AException(-32601, "Method not found: " + method);
            };

            return ResponseEntity.ok(createResponse(id, result));

        } catch (A2AException e) {
            logger.error("JSON-RPC error: code={}, message={}", e.getCode(), e.getMessage());
            return ResponseEntity.ok(createErrorResponse(request.path("id").asText(), e));
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return ResponseEntity.ok(createErrorResponse(request.path("id").asText(), 
                new A2AException(-32603, "Internal error: " + e.getMessage())));
        }
    }

    private JsonNode handleTasksSend(JsonNode params) throws A2AException {
        String taskId = params.path("id").asText();
        JsonNode messageNode = params.path("message");
        
        String text = extractText(messageNode);
        String response = weatherService.processMessage(text).getContent();

        TaskInfo task = new TaskInfo(taskId, "completed", response);
        tasks.put(taskId, task);

        return taskToJson(task);
    }

    private JsonNode handleTasksGet(JsonNode params) throws A2AException {
        String taskId = params.path("id").asText();
        TaskInfo task = tasks.get(taskId);
        
        if (task == null) {
            throw new A2AException(-32000, "Task not found: " + taskId);
        }

        return taskToJson(task);
    }

    private JsonNode handleTasksCancel(JsonNode params) throws A2AException {
        String taskId = params.path("id").asText();
        TaskInfo task = tasks.get(taskId);
        
        if (task == null) {
            throw new A2AException(-32000, "Task not found: " + taskId);
        }

        if ("completed".equals(task.getState()) || "canceled".equals(task.getState())) {
            throw new A2AException(-32001, "Task cannot be canceled in state: " + task.getState());
        }

        TaskInfo canceled = new TaskInfo(taskId, "canceled", null);
        tasks.put(taskId, canceled);
        return taskToJson(canceled);
    }

    private String extractText(JsonNode messageNode) {
        if (messageNode == null || messageNode.isMissingNode()) {
            return "";
        }

        JsonNode partsNode = messageNode.path("parts");
        if (!partsNode.isArray()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : partsNode) {
            if ("text".equals(part.path("kind").asText())) {
                text.append(part.path("text").asText());
            }
        }
        return text.toString();
    }

    private JsonNode taskToJson(TaskInfo task) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", task.getId());
        
        ObjectNode status = objectMapper.createObjectNode();
        status.put("state", task.getState());
        result.set("status", status);

        if (task.getResult() != null) {
            ArrayNode artifacts = objectMapper.createArrayNode();
            ObjectNode artifact = objectMapper.createObjectNode();
            artifact.put("name", "result");
            
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("kind", "text");
            part.put("text", task.getResult());
            parts.add(part);
            
            artifact.set("parts", parts);
            artifacts.add(artifact);
            result.set("artifacts", artifacts);
        }

        return result;
    }

    private JsonNode createResponse(String id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.set("result", result);
        return response;
    }

    private JsonNode createErrorResponse(String id, A2AException error) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("code", error.getCode());
        errorNode.put("message", error.getMessage());
        response.set("error", errorNode);
        
        return response;
    }

    private static class TaskInfo {
        private final String id;
        private final String state;
        private final String result;
        private final Instant createdAt;

        TaskInfo(String id, String state, String result) {
            this.id = id;
            this.state = state;
            this.result = result;
            this.createdAt = Instant.now();
        }

        String getId() { return id; }
        String getState() { return state; }
        String getResult() { return result; }
        Instant getCreatedAt() { return createdAt; }
    }

    public static class A2AException extends Exception {
        private final int code;

        public A2AException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() { return code; }
    }
}

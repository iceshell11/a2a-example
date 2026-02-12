package com.example.a2a.controller;

import com.example.a2a.jsonrpc.A2aJsonRpcService;
import com.example.a2a.jsonrpc.JsonRpcDispatcher;
import com.example.a2a.jsonrpc.TaskException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JsonRpcController {

    private static final Logger logger = LoggerFactory.getLogger(JsonRpcController.class);
    private static final String JSONRPC_VERSION = "2.0";

    private final JsonRpcDispatcher dispatcher;
    private final A2aJsonRpcService rpcService;
    private final ObjectMapper objectMapper;

    public JsonRpcController(JsonRpcDispatcher dispatcher, A2aJsonRpcService rpcService, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.rpcService = rpcService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        dispatcher.registerService(rpcService);
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> handleJsonRpc(@RequestBody JsonNode request) {
        String id = request.path("id").asText();
        String method = request.path("method").asText();
        JsonNode params = request.path("params");

        logger.info("JSON-RPC request: method={}, id={}", method, id);

        try {
            Object result = dispatcher.dispatch(method, params);
            return ResponseEntity.ok(createResponse(id, result));
        } catch (JsonRpcDispatcher.JsonRpcException e) {
            logger.error("JSON-RPC error: code={}, message={}", e.getCode(), e.getMessage());
            return ResponseEntity.ok(createErrorResponse(id, e.getCode(), e.getMessage()));
        } catch (TaskException e) {
            logger.error("Task error: code={}, message={}", e.getCode(), e.getMessage());
            return ResponseEntity.ok(createErrorResponse(id, e.getCode(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return ResponseEntity.ok(createErrorResponse(id, -32603, "Internal error: " + e.getMessage()));
        }
    }

    private JsonNode createResponse(String id, Object result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.set("result", objectMapper.valueToTree(result));
        return response;
    }

    private JsonNode createErrorResponse(String id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        
        return response;
    }
}

package com.example.a2aspring.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.MethodNotFoundJsonMappingException;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class A2AServerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2AServerController.class);

    private final JSONRPCHandler jsonRpcHandler;
    private final ObjectMapper objectMapper;
    private final Executor a2aExecutor;

    public A2AServerController(JSONRPCHandler jsonRpcHandler, 
                               ObjectMapper objectMapper,
                               Executor a2aExecutor) {
        this.jsonRpcHandler = jsonRpcHandler;
        this.objectMapper = objectMapper;
        this.a2aExecutor = a2aExecutor;
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleRequest(@RequestBody String body, HttpServletRequest request) {
        LOGGER.debug("Received request: {}", body);
        
        boolean streaming = false;
        
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode methodNode = node.get("method");
            String method = methodNode != null ? methodNode.asText() : null;
            
            streaming = isStreamingMethod(method);
            ServerCallContext context = createCallContext(request);
            
            if (streaming) {
                return handleStreamingRequest(node, context);
            } else {
                JSONRPCResponse<?> nonStreamingResponse = handleNonStreamingRequest(node, context);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(nonStreamingResponse);
            }
            
        } catch (JsonProcessingException e) {
            LOGGER.error("Error parsing JSON request", e);
            JSONRPCErrorResponse error = handleJsonError(e);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
        } catch (Exception e) {
            LOGGER.error("Error handling request", e);
            JSONRPCErrorResponse error = new JSONRPCErrorResponse(
                    new io.a2a.spec.InternalError(e.getMessage()));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
        }
    }

    @GetMapping("/.well-known/agent-card.json")
    public ResponseEntity<AgentCard> getAgentCard() {
        return ResponseEntity.ok(jsonRpcHandler.getAgentCard());
    }

    private boolean isStreamingMethod(String method) {
        return SendStreamingMessageRequest.METHOD.equals(method) ||
               TaskResubscriptionRequest.METHOD.equals(method);
    }

    private ResponseEntity<SseEmitter> handleStreamingRequest(JsonNode node, ServerCallContext context) 
            throws JsonProcessingException {
        
        JSONRPCRequest<?> request = objectMapper.treeToValue(node, JSONRPCRequest.class);
        SseEmitter emitter = new SseEmitter(0L);
        
        a2aExecutor.execute(() -> {
            try {
                Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
                
                if (request instanceof SendStreamingMessageRequest req) {
                    publisher = jsonRpcHandler.onMessageSendStream(req, context);
                } else if (request instanceof TaskResubscriptionRequest req) {
                    publisher = jsonRpcHandler.onResubscribeToTask(req, context);
                } else {
                    emitter.complete();
                    return;
                }
                
                publisher.subscribe(new Flow.Subscriber<>() {
                    private Flow.Subscription subscription;
                    private final AtomicBoolean completed = new AtomicBoolean(false);
                    
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(1);
                    }
                    
                    @Override
                    public void onNext(JSONRPCResponse<?> item) {
                        if (!completed.get()) {
                            try {
                                emitter.send(item);
                                subscription.request(1);
                            } catch (IOException e) {
                                LOGGER.error("Error sending SSE event", e);
                                onError(e);
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.error("Stream error", throwable);
                        if (completed.compareAndSet(false, true)) {
                            emitter.completeWithError(throwable);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        if (completed.compareAndSet(false, true)) {
                            emitter.complete();
                        }
                    }
                });
                
            } catch (Exception e) {
                LOGGER.error("Error in streaming handler", e);
                emitter.completeWithError(e);
            }
        });
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    private JSONRPCResponse<?> handleNonStreamingRequest(JsonNode node, ServerCallContext context) 
            throws JsonProcessingException {
        
        NonStreamingJSONRPCRequest<?> request = objectMapper.treeToValue(node, NonStreamingJSONRPCRequest.class);
        
        if (request instanceof GetTaskRequest req) {
            return jsonRpcHandler.onGetTask(req, context);
        } else if (request instanceof CancelTaskRequest req) {
            return jsonRpcHandler.onCancelTask(req, context);
        } else if (request instanceof SetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.setPushNotificationConfig(req, context);
        } else if (request instanceof GetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.getPushNotificationConfig(req, context);
        } else if (request instanceof SendMessageRequest req) {
            return jsonRpcHandler.onMessageSend(req, context);
        } else if (request instanceof ListTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.listPushNotificationConfig(req, context);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.deletePushNotificationConfig(req, context);
        } else if (request instanceof GetAuthenticatedExtendedCardRequest req) {
            return jsonRpcHandler.onGetAuthenticatedExtendedCardRequest(req, context);
        } else {
            return new JSONRPCErrorResponse(request.getId(), 
                    new MethodNotFoundError("Unknown method"));
        }
    }

    private ServerCallContext createCallContext(HttpServletRequest request) {
        User user = createUser(request);
        Map<String, Object> state = new HashMap<>();
        
        Map<String, String> headers = extractHeaders(request);
        state.put("headers", headers);
        
        return new ServerCallContext(user, state, Collections.emptySet());
    }

    private User createUser(HttpServletRequest request) {
        String username = request.getRemoteUser();
        if (username == null) {
            return UnauthenticatedUser.INSTANCE;
        }
        
        return new AuthenticatedUser(username);
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private JSONRPCErrorResponse handleJsonError(JsonProcessingException e) {
        if (e.getCause() instanceof MethodNotFoundJsonMappingException ex) {
            return new JSONRPCErrorResponse(ex.getId(), new MethodNotFoundError());
        } else if (e.getCause() instanceof InvalidParamsJsonMappingException ex) {
            return new JSONRPCErrorResponse(ex.getId(), new InvalidParamsError());
        } else if (e.getCause() instanceof IdJsonMappingException ex) {
            return new JSONRPCErrorResponse(ex.getId(), new InvalidRequestError());
        } else {
            return new JSONRPCErrorResponse(new io.a2a.spec.JSONParseError(e.getMessage()));
        }
    }
}

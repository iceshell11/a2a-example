# Refactoring: Switch-Case → jsonrpc4j

## Summary

Replaced the ugly switch-case in `MessageController` with proper **jsonrpc4j** annotation-based dispatching.

## Before (Ugly Switch-Case)

```java
@PostMapping("/")
public ResponseEntity<JsonNode> handleJsonRpc(@RequestBody JsonNode request) {
    String method = request.path("method").asText();
    
    return switch (method) {
        case "tasks/send" -> handleTasksSend(id, params);
        case "tasks/sendSubscribe" -> handleTasksSendSubscribe(id, params);
        case "tasks/get" -> handleTasksGet(id, params);
        case "tasks/cancel" -> handleTasksCancel(id, params);
        default -> errorResponse(id, -32601, "Method not found: " + method);
    };
}
```

**Problems:**
- ❌ Manual method dispatching
- ❌ Verbose error handling
- ❌ No type safety
- ❌ Hard to maintain
- ❌ No validation

## After (Clean jsonrpc4j)

### 1. Interface with Annotations

```java
@JsonRpcService("/")
public interface A2AJsonRpcService {
    
    @JsonRpcMethod("tasks/send")
    Map<String, Object> sendTask(
        @JsonRpcParam("id") String taskId,
        @JsonRpcParam("message") Map<String, Object> message,
        @JsonRpcParam(value = "sessionId", optional = true) String sessionId
    );
    
    @JsonRpcMethod("tasks/get")
    Map<String, Object> getTask(@JsonRpcParam("id") String taskId);
    
    @JsonRpcMethod("tasks/cancel")
    Map<String, Object> cancelTask(@JsonRpcParam("id") String taskId);
}
```

### 2. Configuration

```java
@Bean
public AutoJsonRpcServiceImplExporter autoJsonRpcServiceImplExporter() {
    AutoJsonRpcServiceImplExporter exporter = new AutoJsonRpcServiceImplExporter();
    exporter.setErrorResolver(new A2AErrorResolver());
    return exporter;
}
```

### 3. Clean Implementation

```java
@Service
public class A2AJsonRpcServiceImpl implements A2AJsonRpcService {
    
    @Override
    public Map<String, Object> sendTask(String taskId, Map<String, Object> message, String sessionId) {
        // Business logic only - no dispatching!
        ChatResponse response = weatherService.processMessage(extractText(message));
        return createResult(taskId, response);
    }
}
```

**Benefits:**
- ✅ Automatic method dispatching
- ✅ Type-safe parameter binding
- ✅ Built-in error handling
- ✅ Clean, maintainable code
- ✅ Proper JSON-RPC compliance

## Architecture Flow

```
HTTP POST /
    ↓
AutoJsonRpcServiceImplExporter (jsonrpc4j)
    ↓
@JsonRpcService Dispatcher
    ↓
@JsonRpcMethod Routing
    ↓
A2AJsonRpcServiceImpl
    ↓
Business Logic
```

## Files Changed

### New Files:
- `jsonrpc/A2AJsonRpcService.java` - Interface with annotations
- `jsonrpc/A2AJsonRpcServiceImpl.java` - Implementation
- `jsonrpc/JsonRpcConfiguration.java` - jsonrpc4j configuration
- `config/WebConfiguration.java` - CORS configuration

### Modified Files:
- `pom.xml` - Added jsonrpc4j dependency
- `controller/MessageController.java` - Removed JSON-RPC handling (now just discovery & SSE)
- `README.md` - Updated documentation

## Why jsonrpc4j?

| Library | Stars | Spring Boot | Annotations | Type Safety |
|---------|-------|-------------|-------------|-------------|
| **jsonrpc4j** | 1.1k+ | ✅ Excellent | ✅ Full | ✅ Yes |
| json-rpc (sebastian-toepfer) | ~50 | ✅ Good | ✅ Partial | ✅ Yes |
| simple-json-rpc | ~100 | ⚠️ Manual | ⚠️ Partial | ⚠️ Limited |
| spring-boot-starter-jsonrpc | ~20 | ✅ Good | ✅ Full | ✅ Yes |

**jsonrpc4j** is the clear winner - mature, well-maintained, excellent Spring Boot integration.

## Testing

All existing tests pass with the new architecture. The JSON-RPC protocol handling is now:
- More reliable
- Better error messages
- Properly validated
- Standards-compliant

## Migration Notes

If you have existing code using the old switch-case approach:

1. Extract interface from your service
2. Add `@JsonRpcService` and `@JsonRpcMethod` annotations
3. Configure `AutoJsonRpcServiceImplExporter`
4. Remove manual dispatching code
5. Done!

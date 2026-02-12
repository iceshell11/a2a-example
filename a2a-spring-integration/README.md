# Spring Boot Starter for A2A

A Spring Boot starter for building A2A (Agent-to-Agent) protocol compatible agents. This starter provides auto-configuration for the A2A Java SDK, making it easy to create A2A agents with minimal configuration.

## Features

- **Auto-configuration**: Zero-config setup - just add the dependency
- **Full SDK access**: Implement `AgentExecutor` with complete access to `RequestContext` and `EventQueue`
- **JSON-RPC support**: Full A2A protocol implementation
- **Streaming support**: Server-Sent Events for real-time responses
- **Production-ready**: Thread pool management, error handling, logging

## Requirements

- Java 17+
- Spring Boot 3.5.8+
- A2A Java SDK 0.3.3.Final

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>spring-boot-starter-a2a</artifactId>
    <version>0.3.3-SNAPSHOT</version>
</dependency>
```

### 2. Run Your Application

```bash
mvn spring-boot:run
```

Your Spring Boot application now has an A2A agent running on port 8080:

- **JSON-RPC endpoint**: `POST /`
- **Agent card**: `GET /.well-known/agent-card.json`

### 3. Test It

```bash
# Get agent card
curl http://localhost:8080/.well-known/agent-card.json

# Send a message
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tasks/send",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"text": "Hello!"}]
      }
    }
  }'
```

## Customization

### Custom Agent Logic

Implement `AgentExecutor` to define your agent's behavior. You have full access to `RequestContext` (task metadata, message, session) and `EventQueue` (streaming updates, status changes):

```java
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyAgentExecutor implements AgentExecutor {
    
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        
        // Submit and start work
        updater.submit();
        updater.startWork();
        
        // Access task information
        String taskId = context.getTaskId();
        Message message = context.getMessage();
        String userText = extractText(message);
        
        // Send streaming updates (if streaming is enabled)
        updater.updateStatus(TaskState.WORKING, "Processing your request...");
        
        // Your custom logic here
        String result = processUserMessage(userText);
        
        // Add result as artifact
        List<TextPart> parts = List.of(new TextPart(result, null));
        updater.addArtifact(parts, "response", "Agent Response", null);
        
        // Complete the task
        updater.complete();
    }
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.cancel();
    }
    
    private String extractText(Message message) {
        // Extract text from message parts
        StringBuilder sb = new StringBuilder();
        for (var part : message.getParts()) {
            if (part instanceof TextPart textPart) {
                sb.append(textPart.getText()).append(" ");
            }
        }
        return sb.toString().trim();
    }
    
    private String processUserMessage(String message) {
        // Your business logic here
        return "Processed: " + message;
    }
}
```

The starter will automatically detect and use your implementation. For simpler use cases, extend `DefaultAgentExecutor` and override `processMessage()`:

```java
@Component
public class SimpleExecutor extends DefaultAgentExecutor {
    @Override
    protected String processMessage(String message) {
        return "Custom response: " + message;
    }
}
```

### Configuration

Configure via `application.yml`:

```yaml
a2a:
  enabled: true
  agent:
    name: "My Custom Agent"
    description: "Does amazing things"
    url: "https://myagent.example.com"
    capabilities:
      streaming: true
      push-notifications: false
      state-transition-history: false
    input-modes:
      - text
      - file
    output-modes:
      - text
  executor:
    core-pool-size: 10
    max-pool-size: 100
    keep-alive-seconds: 60
  timeouts:
    blocking-agent: 30
    blocking-consumption: 5
```

Or via `application.properties`:

```properties
# Enable/disable
a2a.enabled=true

# Agent configuration
a2a.agent.name=My Custom Agent
a2a.agent.description=Does amazing things
a2a.agent.url=https://myagent.example.com
a2a.agent.version=1.0.0

# Capabilities
a2a.agent.capabilities.streaming=true
a2a.agent.capabilities.push-notifications=false

# Thread pool
a2a.executor.core-pool-size=10
a2a.executor.max-pool-size=100
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `a2a.enabled` | `true` | Enable/disable auto-configuration |
| `a2a.agent.name` | `"Spring A2A Agent"` | Agent name |
| `a2a.agent.description` | `"A Spring Boot A2A agent"` | Agent description |
| `a2a.agent.version` | `"1.0.0"` | Agent version |
| `a2a.agent.url` | `"http://localhost:8080"` | Agent URL |
| `a2a.agent.capabilities.streaming` | `true` | Support streaming |
| `a2a.agent.capabilities.push-notifications` | `false` | Support push notifications |
| `a2a.agent.capabilities.state-transition-history` | `false` | Support state transition history |
| `a2a.agent.input-modes` | `["text"]` | Supported input modes |
| `a2a.agent.output-modes` | `["text"]` | Supported output modes |
| `a2a.executor.core-pool-size` | `5` | Thread pool core size |
| `a2a.executor.max-pool-size` | `50` | Thread pool max size |
| `a2a.executor.keep-alive-seconds` | `60` | Thread keep-alive time |
| `a2a.timeouts.blocking-agent` | `30` | Timeout for blocking agent ops |
| `a2a.timeouts.blocking-consumption` | `5` | Timeout for blocking consumption |

## Advanced Usage

### Custom Beans

You can override any auto-configured bean by providing your own:

```java
@Configuration
public class CustomA2AConfig {
    
    @Bean
    @Primary
    public TaskStore customTaskStore() {
        return new MyPersistentTaskStore(); // e.g., database-backed
    }
    
    @Bean
    public PushNotificationSender pushNotificationSender() {
        return new WebhookNotificationSender();
    }
}
```

Available beans for override:
- `AgentExecutor` - Custom agent logic (full access to RequestContext and EventQueue)
- `TaskStore` - Task persistence
- `PushNotificationSender` - Push notifications
- `PushNotificationConfigStore` - Notification configuration storage
- `RequestHandler` - Request processing
- `JSONRPCHandler` - JSON-RPC transport

### Disable Auto-Configuration

```yaml
a2a:
  enabled: false
```

## API Endpoints

### JSON-RPC Endpoint

**POST** `/`

Handles all A2A protocol methods:
- `tasks/send` - Send a message (blocking)
- `tasks/sendSubscribe` - Send a message (streaming/SSE)
- `tasks/get` - Get task status
- `tasks/cancel` - Cancel a task
- `tasks/pushNotification/set` - Configure push notifications
- `tasks/pushNotification/get` - Get push notification config
- `tasks/pushNotification/list` - List push notification configs
- `tasks/pushNotification/delete` - Delete push notification config

### Agent Card Endpoint

**GET** `/.well-known/agent-card.json`

Returns the agent's capabilities and metadata.

## Examples

### Blocking Request

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tasks/send",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"text": "analyze my data"}]
      }
    }
  }'
```

### Streaming Request (SSE)

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tasks/sendSubscribe",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"text": "multi-step workflow"}]
      }
    }
  }'
```

### Get Task Status

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tasks/get",
    "params": {
      "id": "task-id-here"
    }
  }'
```

## Testing

Run the test suite:

```bash
mvn test
```

## Troubleshooting

### Port Already in Use

```properties
server.port=8081
```

### Timeout Issues

```properties
a2a.timeouts.blocking-agent=60
```

### Thread Pool Exhaustion

```properties
a2a.executor.max-pool-size=100
```

### Debug Logging

```properties
logging.level.io.github.a2asdk.spring.boot.starter.a2a=DEBUG
logging.level.io.a2a=DEBUG
```

## Migration from Manual Configuration

If you were using the old manual `@Configuration` approach:

**Before:**
```java
@Configuration
public class A2AConfiguration {
    // Manual bean definitions
}
```

**After:**
```java
// Remove the configuration class
// Just implement AgentExecutor:
@Component
public class MyExecutor implements AgentExecutor {
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.submit();
        updater.startWork();
        
        // Your logic with full access to context and event queue
        String result = process(context.getMessage());
        
        updater.addArtifact(List.of(new TextPart(result, null)), "result", "Result", null);
        updater.complete();
    }
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        new TaskUpdater(context, eventQueue).cancel();
    }
}
```

## Next Steps

1. **Add Authentication**: Implement security in `A2AController` by providing a custom `User` factory
2. **Persistent Storage**: Implement custom `TaskStore` with database backend
3. **Push Notifications**: Implement `PushNotificationSender` for webhooks
4. **Custom Skills**: Define agent skills in configuration

## Architecture

```
Client Request → A2AController → JSONRPCHandler → RequestHandler
                                            ↓
                                     AgentExecutor (your implementation)
                                            ↓
                                      Response
```

Your `AgentExecutor` implementation receives:
- **RequestContext**: Task ID, message, session state, user info, metadata
- **EventQueue**: Send status updates, streaming events, artifacts

This gives you full control over the task lifecycle and supports advanced features like streaming updates and multi-step workflows.

## License

Apache License 2.0

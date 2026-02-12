# Spring Boot Starter for A2A

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![A2A SDK](https://img.shields.io/badge/A2A%20SDK-0.3.3-blue.svg)](https://github.com/a2asdk/a2a-java-sdk)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)

A Spring Boot starter for building A2A (Agent-to-Agent) protocol compatible agents. This starter provides auto-configuration for the A2A Java SDK, making it easy to create A2A agents with minimal configuration while maintaining full access to the SDK's capabilities.

## What is A2A?

A2A (Agent-to-Agent) is an open protocol that enables AI agents to communicate and collaborate with each other. This starter implements the A2A protocol, allowing your Spring Boot application to:

- Receive and process agent tasks via JSON-RPC
- Send streaming responses in real-time
- Support multi-step workflows
- Integrate with other A2A-compatible agents

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Agent Development](#agent-development)
- [Advanced Usage](#advanced-usage)
- [API Reference](#api-reference)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)
- [Migration Guide](#migration-guide)

## Features

- **Auto-configuration**: Zero-config setup - just add the dependency
- **Full SDK access**: Direct use of `AgentExecutor` with complete access to `RequestContext` and `EventQueue`
- **JSON-RPC support**: Full A2A protocol implementation
- **Streaming support**: Server-Sent Events (SSE) for real-time responses
- **Production-ready**: Thread pool management, error handling, structured logging
- **Spring-native**: Works seamlessly with Spring Boot dependency injection

## Requirements

- Java 17 or higher
- Spring Boot 3.5.8 or higher
- A2A Java SDK 0.3.3.Final

## Quick Start

### 1. Add Dependency

Add the starter to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>spring-boot-starter-a2a</artifactId>
    <version>0.3.3-SNAPSHOT</version>
</dependency>
```

Or for Gradle:

```groovy
implementation 'io.github.a2asdk:spring-boot-starter-a2a:0.3.3-SNAPSHOT'
```

### 2. Run Your Application

```bash
mvn spring-boot:run
```

Your Spring Boot application now has an A2A agent running on port 8080:

- **JSON-RPC endpoint**: `POST /` - Main A2A protocol endpoint
- **Agent card**: `GET /.well-known/agent-card.json` - Agent capabilities discovery

### 3. Test It

Get the agent card:

```bash
curl http://localhost:8080/.well-known/agent-card.json
```

Send a message:

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
        "parts": [{"type": "text", "text": "Hello!"}]
      }
    }
  }'
```

## Configuration

### Basic Configuration

Configure your agent via `application.yml`:

```yaml
a2a:
  enabled: true
  agent:
    name: "My Custom Agent"
    description: "An AI agent that processes data"
    url: "https://myagent.example.com"
    version: "1.0.0"
    capabilities:
      streaming: true
      push-notifications: false
      state-transition-history: false
    skills:
      - id: "data_processor"
        name: "Data Processor"
        description: "Processes and analyzes data"
        tags: ["data", "analysis"]
  executor:
    core-pool-size: 10
    max-pool-size: 100
    keep-alive-seconds: 60
```

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `a2a.enabled` | `true` | Enable/disable auto-configuration |
| `a2a.agent.name` | `"Spring A2A Agent"` | Agent name displayed in agent card |
| `a2a.agent.description` | `"A Spring Boot A2A agent"` | Agent description |
| `a2a.agent.version` | `"1.0.0"` | Agent version |
| `a2a.agent.url` | `"http://localhost:8080"` | Agent URL endpoint |
| `a2a.agent.capabilities.streaming` | `true` | Support streaming responses |
| `a2a.agent.capabilities.push-notifications` | `false` | Support push notifications |
| `a2a.agent.capabilities.state-transition-history` | `false` | Support state transition history |
| `a2a.agent.input-modes` | `["text"]` | Supported input modes (text, file) |
| `a2a.agent.output-modes` | `["text"]` | Supported output modes (text, file) |
| `a2a.executor.core-pool-size` | `5` | Thread pool core size |
| `a2a.executor.max-pool-size` | `50` | Thread pool max size |
| `a2a.executor.keep-alive-seconds` | `60` | Thread keep-alive time (seconds) |
| `a2a.timeouts.blocking-agent` | `30` | Timeout for blocking agent operations |
| `a2a.timeouts.blocking-consumption` | `5` | Timeout for blocking consumption |

## Agent Development

### Creating a Custom Agent

Implement the `AgentExecutor` interface to define your agent's behavior. You have full access to:

- **`RequestContext`**: Task metadata, message, session state, user info
- **`EventQueue`**: Send streaming updates, status changes, artifacts
- **`TaskUpdater`**: Helper for managing task lifecycle

#### Basic Implementation

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
        
        // Initialize task
        updater.submit();
        updater.startWork();
        
        try {
            // Access request information
            String taskId = context.getTaskId();
            Message message = context.getMessage();
            String userText = extractText(message);
            
            // Process the request
            String result = processUserMessage(userText);
            
            // Add result as artifact
            List<TextPart> parts = List.of(new TextPart(result, null));
            updater.addArtifact(parts, "response", "Agent Response", null);
            
            // Complete successfully
            updater.complete();
            
        } catch (Exception e) {
            // Handle errors
            updater.completeWithError("Processing failed: " + e.getMessage());
        }
    }
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.cancel();
    }
    
    private String extractText(Message message) {
        if (message == null || message.getParts() == null) {
            return "";
        }
        
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

#### Simple Implementation (For Basic Use Cases)

For simpler use cases, extend `DefaultAgentExecutor` and override `processMessage()`:

```java
import io.github.a2asdk.spring.boot.starter.a2a.executor.DefaultAgentExecutor;
import org.springframework.stereotype.Component;

@Component
public class SimpleExecutor extends DefaultAgentExecutor {
    
    @Override
    protected String processMessage(String message) {
        // Simple text processing
        return "Custom response: " + message.toUpperCase();
    }
}
```

### Streaming Responses

To support streaming, enable `streaming: true` in your agent capabilities and send events during processing:

```java
@Component
public class StreamingAgentExecutor implements AgentExecutor {
    
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.submit();
        updater.startWork();
        
        String message = extractText(context.getMessage());
        
        // Simulate streaming chunks
        String[] chunks = message.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < chunks.length; i++) {
            result.append(chunks[i]).append(" ");
            
            // Send progress update every few words
            if (i % 3 == 0) {
                updater.addArtifact(
                    List.of(new TextPart(result.toString().trim(), null)),
                    "partial-result",
                    "Partial Result",
                    null
                );
            }
        }
        
        // Final result
        updater.addArtifact(
            List.of(new TextPart(result.toString().trim(), null)),
            "final-result",
            "Final Result",
            null
        );
        updater.complete();
    }
    
    // ... cancel method
}
```

### Accessing Request Information

The `RequestContext` provides access to:

```java
// Task information
String taskId = context.getTaskId();
String contextId = context.getContextId();
Task task = context.getTask();

// User message
Message message = context.getMessage();

// Session state
Object sessionState = context.getSessionState();

// User information
User user = context.getUser();
String userId = user.id();
Set<String> roles = user.roles();

// Custom metadata
Map<String, Object> metadata = context.getMetadata();
```

## Advanced Usage

### Custom Beans

Override any auto-configured bean by providing your own:

```java
@Configuration
public class CustomA2AConfig {
    
    @Bean
    @Primary
    public TaskStore customTaskStore() {
        // Example: Database-backed task storage
        return new MyPersistentTaskStore();
    }
    
    @Bean
    public PushNotificationSender pushNotificationSender() {
        // Example: Webhook-based notifications
        return new WebhookNotificationSender();
    }
    
    @Bean
    public AgentExecutor agentExecutor(MyService service) {
        // Custom executor with dependency injection
        return new MyServiceAgentExecutor(service);
    }
}
```

### Available Beans for Override

| Bean | Interface | Purpose |
|------|-----------|---------|
| `AgentExecutor` | `AgentExecutor` | Task processing logic |
| `TaskStore` | `TaskStore` | Task persistence |
| `PushNotificationSender` | `PushNotificationSender` | Push notification delivery |
| `PushNotificationConfigStore` | `PushNotificationConfigStore` | Notification configuration |
| `RequestHandler` | `RequestHandler` | Request processing |
| `JSONRPCHandler` | `JSONRPCHandler` | JSON-RPC transport |
| `QueueManager` | `QueueManager` | Event queue management |

### Disable Auto-Configuration

To completely disable A2A auto-configuration:

```yaml
a2a:
  enabled: false
```

## API Reference

### JSON-RPC Endpoint

**POST** `/`

Handles all A2A protocol methods:

#### Methods

| Method | Description |
|--------|-------------|
| `tasks/send` | Send a message (blocking) |
| `tasks/sendSubscribe` | Send a message (streaming/SSE) |
| `tasks/get` | Get task status and history |
| `tasks/cancel` | Cancel a running task |
| `tasks/pushNotification/set` | Configure push notifications |
| `tasks/pushNotification/get` | Get push notification config |
| `tasks/pushNotification/list` | List push notification configs |
| `tasks/pushNotification/delete` | Delete push notification config |

#### Request Format

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tasks/send",
  "params": {
    "message": {
      "role": "user",
      "parts": [
        {
          "type": "text",
          "text": "Hello, agent!"
        }
      ]
    },
    "configuration": {
      "blocking": true
    }
  }
}
```

### Agent Card Endpoint

**GET** `/.well-known/agent-card.json`

Returns the agent's capabilities and metadata:

```json
{
  "name": "My Custom Agent",
  "description": "An AI agent that processes data",
  "url": "https://myagent.example.com",
  "version": "1.0.0",
  "capabilities": {
    "streaming": true,
    "pushNotifications": false,
    "stateTransitionHistory": false
  },
  "skills": [
    {
      "id": "data_processor",
      "name": "Data Processor",
      "description": "Processes and analyzes data",
      "tags": ["data", "analysis"]
    }
  ]
}
```

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
        "parts": [{"type": "text", "text": "analyze my data"}]
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
        "parts": [{"type": "text", "text": "generate a long report"}]
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
      "id": "task-id-from-previous-response"
    }
  }'
```

### Cancel Task

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tasks/cancel",
    "params": {
      "id": "task-id-here"
    }
  }'
```

## Troubleshooting

### Common Issues

#### Port Already in Use

```yaml
server:
  port: 8081
```

#### Timeout Issues

Increase timeout values:

```yaml
a2a:
  timeouts:
    blocking-agent: 60
    blocking-consumption: 10
```

#### Thread Pool Exhaustion

Increase thread pool size:

```yaml
a2a:
  executor:
    core-pool-size: 10
    max-pool-size: 100
```

#### Debug Logging

Enable detailed logging:

```yaml
logging:
  level:
    io.github.a2asdk.spring.boot.starter.a2a: DEBUG
    io.a2a: DEBUG
```

### Testing

Run the test suite:

```bash
# Run all tests
mvn test

# Run with debug output
mvn test -X

# Run specific test
mvn test -Dtest=A2AControllerTest
```

## Migration Guide

### From Manual Configuration

If you were using manual `@Configuration` before:

**Before:**
```java
@Configuration
public class A2AConfiguration {
    
    @Bean
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }
    
    @Bean
    public RequestHandler requestHandler(...) {
        return new DefaultRequestHandler(...);
    }
    
    // ... many more beans
}
```

**After:**
```java
// Remove the configuration class entirely!
// Just implement AgentExecutor:

@Component
public class MyExecutor implements AgentExecutor {
    
    @Autowired
    private MyService service;
    
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.submit();
        updater.startWork();
        
        // Your logic with full access to context and event queue
        String result = service.process(context.getMessage());
        
        updater.addArtifact(List.of(new TextPart(result, null)), "result", "Result", null);
        updater.complete();
    }
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        new TaskUpdater(context, eventQueue).cancel();
    }
}
```

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌────────────────┐
│   Client    │────▶│ A2AController│────▶│ JSONRPCHandler │
└─────────────┘     └──────────────┘     └────────────────┘
                                                  │
                                                  ▼
                                         ┌────────────────┐
                                         │ RequestHandler │
                                         └────────────────┘
                                                  │
                                                  ▼
                                         ┌────────────────┐
                                         │ AgentExecutor  │
                                         │  (Your Impl)   │
                                         └────────────────┘
                                                  │
                                                  ▼
                                            ┌──────────┐
                                            │ Response │
                                            └──────────┘
```

Your `AgentExecutor` implementation receives:

- **RequestContext**: Task ID, message, session state, user info, metadata
- **EventQueue**: Send status updates, streaming events, artifacts

This gives you full control over the task lifecycle and supports advanced features like streaming updates and multi-step workflows.

## Next Steps

1. **Add Authentication**: Implement custom `User` creation in `A2AController`
2. **Persistent Storage**: Implement `TaskStore` with JPA/JDBC for database persistence
3. **Push Notifications**: Implement `PushNotificationSender` for webhook notifications
4. **Custom Skills**: Define agent capabilities in configuration
5. **Monitoring**: Add Micrometer metrics for task processing

## Resources

- [A2A Protocol Specification](https://github.com/a2asdk/a2a-spec)
- [A2A Java SDK Documentation](https://github.com/a2asdk/a2a-java-sdk)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)

## License

Apache License 2.0

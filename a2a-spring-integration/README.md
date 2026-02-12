# A2A Spring Boot Integration

A complete Spring Boot integration with the A2A (Agent2Agent) Java SDK, enabling you to build A2A-compatible agents using Spring's dependency injection and configuration management.

## Features

- **Unified JSON-RPC Endpoint**: Single endpoint handling all A2A protocol methods
- **Event-Driven Architecture**: Send status updates and artifacts via EventQueue
- **Sub-Agent Support**: Delegate work to multiple sub-agents with progress tracking
- **Streaming Support**: Real-time event streaming via SSE (Server-Sent Events)
- **Spring Configuration**: Full Spring dependency injection and configuration
- **No LangChain Required**: Pure Java/Spring implementation

## Quick Start

### 1. Build the Project

```bash
cd a2a-spring-integration
mvn clean install
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

### 3. Test the Agent Card

```bash
curl http://localhost:8080/.well-known/agent-card.json
```

### 4. Send a Message

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
        "parts": [{"text": "Hello, please analyze this message"}]
      }
    }
  }'
```

## Project Structure

```
com.example.a2aspring/
├── A2ASpringApplication.java          # Spring Boot entry point
├── config/
│   └── A2AConfiguration.java          # A2A SDK bean configuration
├── controller/
│   └── A2AServerController.java       # JSON-RPC REST endpoint
├── executor/
│   ├── SpringAgentExecutor.java       # Base executor implementation
│   ├── CustomBusinessAgentExecutor.java  # Example business logic executor
│   └── MultiStepAgentExecutor.java    # Multi-step/sub-agent executor
└── service/
    └── SubAgentService.java           # Sub-agent delegation service
```

## Architecture

### Components

1. **A2AServerController**
   - Handles HTTP requests at `/`
   - Routes JSON-RPC requests to appropriate handlers
   - Supports both blocking and streaming (SSE) responses

2. **SpringAgentExecutor**
   - Implements `AgentExecutor` interface
   - Processes user messages
   - Sends events via `TaskUpdater`
   - Can be extended for custom logic

3. **A2AConfiguration**
   - Configures all A2A SDK components as Spring beans
   - Sets up `TaskStore`, `QueueManager`, `RequestHandler`, etc.
   - Configures thread pool for async operations

### Event Flow

```
Client Request → A2AServerController → JSONRPCHandler → DefaultRequestHandler
                                                          ↓
                                                    AgentExecutor.execute()
                                                          ↓
                         ┌────────────────────────────────┼────────────────────────────────┐
                         ↓                                ↓                                ↓
                   TaskUpdater.submit()         TaskUpdater.addArtifact()        TaskUpdater.complete()
                         ↓                                ↓                                ↓
                   EventQueue (status)           EventQueue (artifact)          EventQueue (final)
                         ↓                                ↓                                ↓
                         └────────────────────────────────┴────────────────────────────────┘
                                                          ↓
                                                    Client Response
```

## Customizing the Agent

### Basic Customization

Extend `SpringAgentExecutor` and override `processMessage()`:

```java
@Component
public class MyAgentExecutor extends SpringAgentExecutor {
    
    @Override
    protected String processMessage(String message) {
        // Your custom logic here
        return "Custom response for: " + message;
    }
}
```

### Advanced: Sub-Agent Workflow

Use `MultiStepAgentExecutor` to delegate to sub-agents:

```java
@Component
public class MyMultiStepExecutor extends SpringAgentExecutor {
    
    @Autowired
    private SubAgentService subAgentService;
    
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        
        // Initialize task
        updater.submit();
        updater.startWork();
        
        // Delegate to sub-agents
        subAgentService.processWithSubAgents(context, eventQueue);
        
        // Complete
        updater.complete();
    }
}
```

### Sending Custom Events

```java
@Override
public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    TaskUpdater updater = new TaskUpdater(context, eventQueue);
    
    // Update status
    updater.startWork();
    
    // Add artifact
    List<Part<?>> parts = List.of(new TextPart("Result", null));
    updater.addArtifact(parts, "result-id", "My Result", null);
    
    // Custom status update with metadata
    TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
        .taskId(context.getTaskId())
        .contextId(context.getContextId())
        .status(new TaskStatus(TaskState.WORKING, message, null))
        .isFinal(false)
        .metadata(Map.of("progress", "50%"))
        .build();
    
    eventQueue.enqueueEvent(event);
    
    // Complete
    updater.complete();
}
```

## Configuration

### Application Properties

```properties
# Timeouts
a2a.blocking.agent.timeout.seconds=30
a2a.blocking.consumption.timeout.seconds=5

# Thread Pool
a2a.executor.core-pool-size=5
a2a.executor.max-pool-size=50
a2a.executor.keep-alive-seconds=60
```

### Agent Card Configuration

Modify `A2AConfiguration.agentCard()` to customize:

- **Agent Name**: Display name of your agent
- **Capabilities**: Streaming, push notifications, state history
- **Skills**: What your agent can do
- **Input/Output Modes**: Supported formats (text, file, etc.)

## API Endpoints

### JSON-RPC Endpoint

**POST** `/`

Handles all A2A protocol methods:
- `tasks/send` - Send a message (blocking)
- `tasks/sendSubscribe` - Send a message (streaming)
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

### Streaming Request

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

## Dependencies

Key dependencies:
- Spring Boot 3.2.x
- A2A Java SDK 0.3.2.Final
- Jackson for JSON processing

## Troubleshooting

### Common Issues

1. **Port already in use**
   ```properties
   server.port=8081
   ```

2. **Timeout issues**
   ```properties
   a2a.blocking.agent.timeout.seconds=60
   ```

3. **Thread pool exhaustion**
   ```properties
   a2a.executor.max-pool-size=100
   ```

### Debug Logging

```properties
logging.level.io.a2a=DEBUG
logging.level.com.example.a2aspring=TRACE
```

## Next Steps

1. **Add Authentication**: Implement JWT or OAuth2 in `A2AServerController.createUser()`
2. **Persistent Storage**: Replace `InMemoryTaskStore` with database-backed store
3. **Push Notifications**: Implement `PushNotificationSender` for webhooks
4. **Custom Transports**: Add REST or gRPC endpoints alongside JSON-RPC

## License

Apache License 2.0

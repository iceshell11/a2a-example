# A2A Spring Demo

A complete working example of **Spring Boot 3.5.8** + **Spring AI 1.1.2** + **A2A SDK 0.3.3.Final** with JSON-RPC and streaming support.

## Features

- **Agent2Agent (A2A) Protocol**: Full A2A server implementation
- **JSON-RPC 2.0**: Standard A2A communication protocol using **jsonrpc4j** library
- **Annotation-based Dispatching**: No switch-case, proper `@JsonRpcMethod` annotations
- **Streaming (SSE)**: Server-Sent Events for real-time responses
- **Agent Discovery**: Agent card at `/.well-known/agent-card.json`
- **Task Management**: Send, query, and cancel tasks
- **Weather Agent**: Example agent with tools (using stubs for LLM)

## Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) OpenAI API key for real LLM integration

## Quick Start

### 1. Build the Project

```bash
mvn clean install -DskipTests
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

Or with custom port:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### 3. Test the Endpoints

#### Using HTTP Test Script (Bash)
```bash
chmod +x test-a2a.sh
./test-a2a.sh
```

#### Using HTTP Test Script (Windows)
```cmd
test-a2a.bat
```

#### Using HTTP Client (VS Code REST Client)
Open `http-tests.http` in VS Code with REST Client extension installed.

#### Manual curl Commands

**Health Check:**
```bash
curl http://localhost:8080/health
```

**Get Agent Card:**
```bash
curl http://localhost:8080/.well-known/agent-card.json
```

**Send Weather Task:**
```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "req-001",
    "method": "tasks/send",
    "params": {
      "id": "task-001",
      "message": {
        "role": "user",
        "parts": [
          {
            "kind": "text",
            "text": "What'"'"'s the weather in London?"
          }
        ]
      }
    }
  }'
```

**Streaming Request:**
```bash
curl -N -X POST http://localhost:8080/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "id": "stream-1",
    "method": "tasks/send",
    "params": {
      "id": "task-stream-1",
      "message": {
        "role": "user",
        "parts": [
          {
            "kind": "text",
            "text": "What'"'"'s the weather in Tokyo?"
          }
        ]
      }
    }
  }'
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/.well-known/agent-card.json` | GET | A2A Agent Card (discovery) |
| `/card` | GET | Alternative Agent Card endpoint |
| `/` | POST | JSON-RPC A2A protocol endpoint |
| `/stream` | POST | Streaming endpoint (SSE) |

## JSON-RPC Methods

### `tasks/send`
Send a task to the agent and get immediate response.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": "request-id",
  "method": "tasks/send",
  "params": {
    "id": "task-id",
    "message": {
      "role": "user",
      "parts": [
        {
          "kind": "text",
          "text": "User message here"
        }
      ]
    }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "request-id",
  "result": {
    "id": "task-id",
    "status": {
      "state": "completed"
    },
    "artifacts": [
      {
        "name": "result",
        "parts": [
          {
            "kind": "text",
            "text": "Weather response here"
          }
        ]
      }
    ]
  }
}
```

### `tasks/get`
Query the status of an existing task.

### `tasks/cancel`
Cancel a running task.

## Streaming Events

When using the `/stream` endpoint, you'll receive SSE events:

```
event: message
data: {"type":"status","taskId":"task-1","state":"working","message":"Processing..."}

event: message
data: {"type":"progress","taskId":"task-1","progress":50}

event: message
data: {"type":"text","taskId":"task-1","text":"Partial response..."}

event: message
data: {"type":"status","taskId":"task-1","state":"completed","message":"Done"}
```

## JSON-RPC Architecture

This project uses **jsonrpc4j** for proper JSON-RPC 2.0 dispatching:

```
HTTP Request → AutoJsonRpcServiceImplExporter → A2AJsonRpcService
                                                  ↓
                                            @JsonRpcMethod dispatcher
                                                  ↓
                                       A2AJsonRpcServiceImpl
```

### Why jsonrpc4j?

- ✅ **No switch-case**: Annotation-based method dispatching (`@JsonRpcMethod`)
- ✅ **Type-safe**: Automatic parameter binding and validation
- ✅ **Error handling**: Built-in JSON-RPC error responses
- ✅ **Spring integration**: `AutoJsonRpcServiceImplExporter` auto-configures services
- ✅ **Well-maintained**: 1.1k+ stars, widely used

### How it works

1. **Interface** (`A2AJsonRpcService`): Defines methods with `@JsonRpcService("/")` and `@JsonRpcMethod` annotations
2. **Implementation** (`A2AJsonRpcServiceImpl`): Contains business logic
3. **Exporter** (`JsonRpcConfiguration`): Auto-exposes services using `AutoJsonRpcServiceImplExporter`
4. **Dispatching**: jsonrpc4j handles method routing automatically

## Project Structure

```
src/main/java/com/example/a2a/
├── A2ADemoApplication.java              # Spring Boot entry point
├── config/
│   ├── A2AConfiguration.java            # AgentCard bean
│   ├── JsonRpcConfiguration.java        # jsonrpc4j exporter config
│   └── WebConfiguration.java            # CORS config
├── controller/
│   └── MessageController.java           # Discovery & SSE endpoints
├── jsonrpc/
│   ├── A2AJsonRpcService.java           # JSON-RPC interface (annotations)
│   ├── A2AJsonRpcServiceImpl.java       # JSON-RPC implementation
│   └── A2AErrorResolver.java            # Custom error handling
├── executor/
│   └── WeatherAgentExecutor.java        # A2A AgentExecutor
├── model/
│   └── ChatResponse.java
└── service/
    └── WeatherService.java              # Weather tools (stubs)
```
src/main/java/com/example/a2a/
├── A2ADemoApplication.java          # Spring Boot entry point
├── config/
│   └── A2AConfiguration.java        # AgentCard bean configuration
├── controller/
│   └── MessageController.java       # JSON-RPC & SSE endpoints
├── executor/
│   └── WeatherAgentExecutor.java    # A2A AgentExecutor implementation
├── model/
│   └── ChatResponse.java            # Response model
└── service/
    └── WeatherService.java          # Weather agent logic with tools
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080  # Change port here

spring:
  ai:
    openai:
      api-key: your-api-key  # For real LLM integration
```

## A2A Protocol Compliance

This implementation follows the [A2A Protocol](https://a2a-protocol.org/) specification:

- ✓ Agent Card discovery (`/.well-known/agent-card.json`)
- ✓ JSON-RPC 2.0 transport
- ✓ Task lifecycle (submit, working, completed, canceled)
- ✓ Message parts (text)
- ✓ Streaming support (SSE)
- ✓ Task queries and cancellation

## Dependencies

| Dependency | Version |
|------------|---------|
| Spring Boot | 3.5.8 |
| Spring AI | 1.1.2 |
| A2A Java SDK | 0.3.3.Final |
| Java | 17+ |

## Development

### Running Tests
```bash
mvn test
```

### Build JAR
```bash
mvn clean package
java -jar target/a2a-spring-demo-1.0.0.jar
```

## License

MIT License - Feel free to use this as a starting point for your A2A agents!

## JSON-RPC Library

This project uses [**jsonrpc4j**](https://github.com/briandilley/jsonrpc4j) - the most popular JSON-RPC library for Java:

```xml
<dependency>
    <groupId>com.github.briandilley.jsonrpc4j</groupId>
    <artifactId>jsonrpc4j</artifactId>
    <version>1.6</version>
</dependency>
```

### Key Features:
- **1.1k+ stars** on GitHub
- **Spring Boot integration** via `AutoJsonRpcServiceImplExporter`
- **Annotation-based** (`@JsonRpcService`, `@JsonRpcMethod`, `@JsonRpcParam`)
- **Type-safe** parameter binding
- **Automatic error handling** with proper JSON-RPC error codes
- **No boilerplate** - clean service interface + implementation

### Example:
```java
@JsonRpcService("/")
public interface A2AJsonRpcService {
    @JsonRpcMethod("tasks/send")
    Map<String, Object> sendTask(
        @JsonRpcParam("id") String taskId,
        @JsonRpcParam("message") Map<String, Object> message
    );
}
```

## Resources

- [A2A Protocol Documentation](https://a2a-protocol.org/)
- [A2A Java SDK](https://github.com/a2aproject/a2a-java)
- [jsonrpc4j GitHub](https://github.com/briandilley/jsonrpc4j)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)

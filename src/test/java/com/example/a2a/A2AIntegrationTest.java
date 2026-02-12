package com.example.a2a;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A2A Integration Tests
 * Tests JSON-RPC endpoints handled by jsonrpc4j dispatcher
 */
@SpringBootTest
@AutoConfigureMockMvc
public class A2AIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheck_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.agent").value("Weather Agent"));
    }

    @Test
    void agentCard_shouldReturnValidCard() throws Exception {
        mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Weather Agent"))
                .andExpect(jsonPath("$.protocolVersion").value("0.3.0"))
                .andExpect(jsonPath("$.capabilities.streaming").value(true));
    }

    @Test
    void tasksSend_shouldProcessWeatherRequest() throws Exception {
        String request = """
            {
              "jsonrpc": "2.0",
              "id": "test-001",
              "method": "tasks/send",
              "params": {
                "id": "task-test-001",
                "message": {
                  "role": "user",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "What's the weather in London?"
                    }
                  ]
                }
              }
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("test-001"))
                .andExpect(jsonPath("$.result.id").value("task-test-001"))
                .andExpect(jsonPath("$.result.status.state").value("completed"))
                .andExpect(jsonPath("$.result.artifacts").exists());
    }

    @Test
    void tasksSend_shouldProcessForecastRequest() throws Exception {
        String request = """
            {
              "jsonrpc": "2.0",
              "id": "test-002",
              "method": "tasks/send",
              "params": {
                "id": "task-test-002",
                "message": {
                  "role": "user",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "Give me a 3-day forecast for New York"
                    }
                  ]
                }
              }
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result.status.state").value("completed"));
    }

    @Test
    void tasksGet_shouldReturnTaskStatus() throws Exception {
        // First create a task
        String sendRequest = """
            {
              "jsonrpc": "2.0",
              "id": "test-003",
              "method": "tasks/send",
              "params": {
                "id": "task-test-003",
                "message": {
                  "role": "user",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "Weather in Paris"
                    }
                  ]
                }
              }
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sendRequest));

        // Now get the task
        String getRequest = """
            {
              "jsonrpc": "2.0",
              "id": "test-004",
              "method": "tasks/get",
              "params": {
                "id": "task-test-003"
              }
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("task-test-003"));
    }

    @Test
    void tasksCancel_shouldCancelTask() throws Exception {
        // First create a task
        String sendRequest = """
            {
              "jsonrpc": "2.0",
              "id": "test-005",
              "method": "tasks/send",
              "params": {
                "id": "task-test-005",
                "message": {
                  "role": "user",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "Weather in Berlin"
                    }
                  ]
                }
              }
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sendRequest));

        // Now cancel the task
        String cancelRequest = """
            {
              "jsonrpc": "2.0",
              "id": "test-006",
              "method": "tasks/cancel",
              "params": {
                "id": "task-test-005"
              }
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancelRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32001))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("cannot be canceled")));
    }

    @Test
    void unknownMethod_shouldReturnError() throws Exception {
        // jsonrpc4j returns error for unknown methods
        String request = """
            {
              "jsonrpc": "2.0",
              "id": "test-error-002",
              "method": "tasks/unknown",
              "params": {}
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void greetingRequest_shouldReturnHelpMessage() throws Exception {
        String request = """
            {
              "jsonrpc": "2.0",
              "id": "test-help-001",
              "method": "tasks/send",
              "params": {
                "id": "task-help-001",
                "message": {
                  "role": "user",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "Hello, what can you do?"
                    }
                  ]
                }
              }
            }
            """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.artifacts[0].parts[0].text").value(org.hamcrest.Matchers.containsString("Weather Agent")));
    }
}

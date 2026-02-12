package com.example.a2aspring;

import com.example.a2aspring.controller.A2AServerController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class A2AServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testAgentCardEndpoint() throws Exception {
        mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Spring A2A Agent"))
                .andExpect(jsonPath("$.capabilities.streaming").value(true));
    }

    @Test
    public void testSendMessageBlocking() throws Exception {
        String requestBody = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "tasks/send",
                    "params": {
                        "message": {
                            "role": "user",
                            "parts": [{"text": "Hello, analyze this"}]
                        }
                    }
                }
                """;

        MvcResult result = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertNotNull(response.get("id"));
        assertNotNull(response.get("result"));
        
        JsonNode taskResult = response.get("result");
        assertNotNull(taskResult.get("id"));
        assertNotNull(taskResult.get("status"));
        assertEquals("completed", taskResult.get("status").get("state").asText());
    }

    @Test
    public void testSendMessageWithAnalysisKeyword() throws Exception {
        String requestBody = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "tasks/send",
                    "params": {
                        "message": {
                            "role": "user",
                            "parts": [{"text": "Please analyze my data for insights"}]
                        }
                    }
                }
                """;

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status.state").value("completed"))
                .andExpect(jsonPath("$.result.artifacts").isArray());
    }

    @Test
    public void testGetTask() throws Exception {
        // First create a task
        String sendRequest = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "tasks/send",
                    "params": {
                        "message": {
                            "role": "user",
                            "parts": [{"text": "Test message"}]
                        }
                    }
                }
                """;

        MvcResult sendResult = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendRequest))
                .andReturn();

        String sendResponse = sendResult.getResponse().getContentAsString();
        JsonNode taskResult = objectMapper.readTree(sendResponse).get("result");
        String taskId = taskResult.get("id").asText();

        // Then get the task
        String getRequest = String.format("""
                {
                    "jsonrpc": "2.0",
                    "id": 2,
                    "method": "tasks/get",
                    "params": {
                        "id": "%s"
                    }
                }
                """, taskId);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(taskId))
                .andExpect(jsonPath("$.result.status.state").value("completed"));
    }

    @Test
    public void testCancelTask() throws Exception {
        String sendRequest = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "tasks/send",
                    "params": {
                        "message": {
                            "role": "user",
                            "parts": [{"text": "Long running task"}]
                        },
                        "configuration": {
                            "blocking": false
                        }
                    }
                }
                """;

        MvcResult sendResult = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendRequest))
                .andReturn();

        String sendResponse = sendResult.getResponse().getContentAsString();
        JsonNode taskResult = objectMapper.readTree(sendResponse).get("result");
        String taskId = taskResult.get("id").asText();

        String cancelRequest = String.format("""
                {
                    "jsonrpc": "2.0",
                    "id": 2,
                    "method": "tasks/cancel",
                    "params": {
                        "id": "%s"
                    }
                }
                """, taskId);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isOk());
    }

    @Test
    public void testInvalidJson() throws Exception {
        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testUnknownMethod() throws Exception {
        String requestBody = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "unknown/method",
                    "params": {}
                }
                """;

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").exists());
    }
}

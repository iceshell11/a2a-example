package com.example.a2a.jsonrpc;

import com.example.a2a.model.Task;
import com.example.a2a.service.TaskService;
import com.example.a2a.service.WeatherService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class A2aJsonRpcService {

    private final TaskService taskService;
    private final WeatherService weatherService;

    public A2aJsonRpcService(TaskService taskService, WeatherService weatherService) {
        this.taskService = taskService;
        this.weatherService = weatherService;
    }

    @JsonRpcMethod("message/send")
    public Task sendMessage(
            @JsonRpcParam("id") String id,
            @JsonRpcParam("message") JsonNode message) {
        return doSendMessage(id, message);
    }

    @JsonRpcMethod("tasks/send")
    public Task tasksSend(
            @JsonRpcParam("id") String id,
            @JsonRpcParam("message") JsonNode message) {
        return doSendMessage(id, message);
    }

    private Task doSendMessage(String id, JsonNode message) {
        String text = extractText(message);
        
        Task task = taskService.createTask(id);
        taskService.setTaskWorking(id);
        
        String response = weatherService.processMessage(text).getContent();
        taskService.completeTask(id, response);
        
        return taskService.getTask(id);
    }

    @JsonRpcMethod("tasks/get")
    public Task getTask(@JsonRpcParam("id") String id) {
        Task task = taskService.getTask(id);
        if (task == null) {
            throw new TaskException(-32000, "Task not found: " + id);
        }
        return task;
    }

    @JsonRpcMethod("tasks/cancel")
    public Task cancelTask(@JsonRpcParam("id") String id) {
        Task task = taskService.getTask(id);
        if (task == null) {
            throw new TaskException(-32000, "Task not found: " + id);
        }
        
        if (task.getState() == Task.TaskState.COMPLETED || task.getState() == Task.TaskState.CANCELED) {
            throw new TaskException(-32001, "Task cannot be canceled in state: " + task.getState());
        }
        
        taskService.cancelTask(id);
        return taskService.getTask(id);
    }

    private String extractText(JsonNode message) {
        if (message == null || !message.has("parts")) {
            return "";
        }
        
        StringBuilder text = new StringBuilder();
        JsonNode parts = message.get("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if ("text".equals(part.path("type").asText()) || "text".equals(part.path("kind").asText())) {
                    text.append(part.path("text").asText());
                }
            }
        }
        return text.toString();
    }
}

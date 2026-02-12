package com.example.a2a.service;

import com.example.a2a.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

@Service
public class StreamingService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingService.class);
    private static final long SSE_TIMEOUT = 30000; // 30 seconds
    private static final long EMIT_DELAY_MS = 50; // Delay between events

    private final TaskService taskService;
    private final WeatherService weatherService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public StreamingService(TaskService taskService, WeatherService weatherService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.weatherService = weatherService;
        this.objectMapper = objectMapper;
    }

    public SseEmitter streamMessage(String taskId, String messageText) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        Task task = taskService.createTask(taskId);
        taskService.setTaskWorking(taskId);
        
        executorService.submit(() -> {
            try {
                // Send working status
                emitStatus(emitter, taskId, "working");
                Thread.sleep(EMIT_DELAY_MS);
                
                // Get weather response
                String response = weatherService.processMessage(messageText).getContent();
                
                // Emit response as text event
                emitText(emitter, taskId, response);
                Thread.sleep(EMIT_DELAY_MS);
                
                // Complete task
                taskService.completeTask(taskId, response);
                
                // Send completed status
                emitStatus(emitter, taskId, "completed");
                
                emitter.complete();
            } catch (Exception e) {
                logger.error("Streaming error", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    public SseEmitter subscribeToTask(String taskId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        if (!taskService.canSubscribe(taskId)) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Task not found or already has subscriber"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        Task task = taskService.getTask(taskId);
        if (task == null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Task not found: " + taskId));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        // Register listener for updates
        taskService.addListener(taskId, updatedTask -> {
            try {
                if (updatedTask.getState() == Task.TaskState.COMPLETED) {
                    emitStatus(emitter, taskId, "completed");
                    if (updatedTask.getResult() != null) {
                        emitText(emitter, taskId, updatedTask.getResult());
                    }
                    emitter.complete();
                    taskService.removeListener(taskId);
                } else if (updatedTask.getState() == Task.TaskState.CANCELED) {
                    emitStatus(emitter, taskId, "canceled");
                    emitter.complete();
                    taskService.removeListener(taskId);
                } else {
                    emitStatus(emitter, taskId, updatedTask.getState().name().toLowerCase());
                }
            } catch (Exception e) {
                logger.error("Error in subscribe listener", e);
                emitter.completeWithError(e);
                taskService.removeListener(taskId);
            }
        });
        
        // Send current state immediately
        try {
            emitStatus(emitter, taskId, task.getState().name().toLowerCase());
            if (task.getState() == Task.TaskState.COMPLETED && task.getResult() != null) {
                emitText(emitter, taskId, task.getResult());
                emitter.complete();
                taskService.removeListener(taskId);
            }
        } catch (Exception e) {
            logger.error("Error sending initial state", e);
            emitter.completeWithError(e);
            taskService.removeListener(taskId);
        }
        
        // Timeout cleanup
        emitter.onTimeout(() -> {
            logger.warn("SSE timeout for task: {}", taskId);
            taskService.removeListener(taskId);
        });
        
        emitter.onCompletion(() -> {
            taskService.removeListener(taskId);
        });
        
        return emitter;
    }

    private void emitStatus(SseEmitter emitter, String taskId, String state) throws IOException {
        SseEvent event = new SseEvent("task_status_update", taskId, state, null);
        emitter.send(SseEmitter.event()
            .name("message")
            .data(event));
    }

    private void emitText(SseEmitter emitter, String taskId, String text) throws IOException {
        SseEvent event = new SseEvent("task_artifact_update", taskId, null, text);
        emitter.send(SseEmitter.event()
            .name("message")
            .data(event));
    }

    public static class SseEvent {
        private final String type;
        private final String taskId;
        private final String state;
        private final String text;

        public SseEvent(String type, String taskId, String state, String text) {
            this.type = type;
            this.taskId = taskId;
            this.state = state;
            this.text = text;
        }

        public String getType() { return type; }
        public String getTaskId() { return taskId; }
        public String getState() { return state; }
        public String getText() { return text; }
    }
}

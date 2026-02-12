package com.example.a2a.service;

import com.example.a2a.model.Task;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TaskService {
    
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final Map<String, TaskUpdateListener> listeners = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public TaskService() {
        // Cleanup old tasks every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldTasks, 5, 5, TimeUnit.MINUTES);
    }

    public Task createTask(String id) {
        Task task = new Task(id);
        tasks.put(id, task);
        return task;
    }

    public Task getTask(String id) {
        return tasks.get(id);
    }

    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
        notifyListeners(task);
    }

    public void setTaskWorking(String id) {
        Task task = tasks.get(id);
        if (task != null) {
            task.setState(Task.TaskState.WORKING);
            updateTask(task);
        }
    }

    public void completeTask(String id, String result) {
        Task task = tasks.get(id);
        if (task != null) {
            task.setState(Task.TaskState.COMPLETED);
            task.setResult(result);
            
            // Create artifact for the result
            Task.Part part = new Task.Part("text", result);
            Task.Artifact artifact = new Task.Artifact("response", List.of(part));
            task.setArtifacts(List.of(artifact));
            
            updateTask(task);
        }
    }

    public void cancelTask(String id) {
        Task task = tasks.get(id);
        if (task != null) {
            task.setState(Task.TaskState.CANCELED);
            updateTask(task);
        }
    }

    public boolean canSubscribe(String id) {
        Task task = tasks.get(id);
        return task != null && !listeners.containsKey(id);
    }

    public void addListener(String taskId, TaskUpdateListener listener) {
        listeners.put(taskId, listener);
    }

    public void removeListener(String taskId) {
        listeners.remove(taskId);
    }

    private void notifyListeners(Task task) {
        TaskUpdateListener listener = listeners.get(task.getId());
        if (listener != null) {
            listener.onTaskUpdate(task);
        }
    }

    private void cleanupOldTasks() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour old
        tasks.entrySet().removeIf(entry -> {
            Task task = entry.getValue();
            boolean isOld = task.getUpdatedAt().isBefore(cutoff);
            boolean isTerminal = task.getState() == Task.TaskState.COMPLETED 
                || task.getState() == Task.TaskState.CANCELED;
            if (isOld && isTerminal) {
                listeners.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    @FunctionalInterface
    public interface TaskUpdateListener {
        void onTaskUpdate(Task task);
    }
}

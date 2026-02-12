package com.example.a2a.model;

import java.time.Instant;
import java.util.List;

public class Task {
    private final String id;
    private TaskState state;
    private String result;
    private final Instant createdAt;
    private Instant updatedAt;
    private List<Artifact> artifacts;

    public Task(String id) {
        this.id = id;
        this.state = TaskState.SUBMITTED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public TaskState getState() { return state; }
    public void setState(TaskState state) { 
        this.state = state; 
        this.updatedAt = Instant.now();
    }
    public String getResult() { return result; }
    public void setResult(String result) { 
        this.result = result; 
        this.updatedAt = Instant.now();
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Artifact> getArtifacts() { return artifacts; }
    public void setArtifacts(List<Artifact> artifacts) { 
        this.artifacts = artifacts;
        this.updatedAt = Instant.now();
    }

    public Status getStatus() {
        return new Status(state.name().toLowerCase());
    }

    public static class Status {
        private final String state;

        public Status(String state) {
            this.state = state;
        }

        public String getState() { return state; }
    }

    public enum TaskState {
        SUBMITTED, WORKING, INPUT_REQUIRED, COMPLETED, CANCELED
    }

    public static class Artifact {
        private final String name;
        private final List<Part> parts;

        public Artifact(String name, List<Part> parts) {
            this.name = name;
            this.parts = parts;
        }

        public String getName() { return name; }
        public List<Part> getParts() { return parts; }
    }

    public static class Part {
        private final String type;
        private final String text;

        public Part(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() { return type; }
        public String getText() { return text; }
    }
}

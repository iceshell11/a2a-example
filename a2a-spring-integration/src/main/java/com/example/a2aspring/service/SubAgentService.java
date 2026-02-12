package com.example.a2aspring.service;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SubAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubAgentService.class);

    public void processWithSubAgents(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        String taskId = context.getTaskId();
        String contextId = context.getContextId();
        
        try {
            LOGGER.info("Delegating to sub-agents for task: {}", taskId);
            
            sendProgressEvent(eventQueue, taskId, contextId, "Initializing sub-agent workflow");
            
            String step1Result = executeSubAgent1(context, eventQueue);
            updater.addArtifact(
                    List.of(new TextPart(step1Result, null)),
                    "step-1-" + UUID.randomUUID(),
                    "Step 1: Data Extraction",
                    Map.of("step", 1, "agent", "extractor")
            );
            
            sendProgressEvent(eventQueue, taskId, contextId, "Data extraction completed");
            
            String step2Result = executeSubAgent2(context, eventQueue, step1Result);
            updater.addArtifact(
                    List.of(new TextPart(step2Result, null)),
                    "step-2-" + UUID.randomUUID(),
                    "Step 2: Processing",
                    Map.of("step", 2, "agent", "processor")
            );
            
            sendProgressEvent(eventQueue, taskId, contextId, "Processing completed");
            
            String step3Result = executeSubAgent3(context, eventQueue, step2Result);
            updater.addArtifact(
                    List.of(new TextPart(step3Result, null)),
                    "step-3-" + UUID.randomUUID(),
                    "Step 3: Finalization",
                    Map.of("step", 3, "agent", "finalizer")
            );
            
            sendProgressEvent(eventQueue, taskId, contextId, "All sub-agents completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Error in sub-agent workflow", e);
            throw new InternalError("Sub-agent processing failed: " + e.getMessage());
        }
    }

    private String executeSubAgent1(RequestContext context, EventQueue eventQueue) {
        LOGGER.debug("Executing Sub-Agent 1: Data Extractor");
        
        String userInput = context.getUserInput(" ");
        
        simulateWork(500);
        
        return """
                **Sub-Agent 1 (Data Extractor) Results:**
                
                - Extracted entities: %d
                - Key terms identified: %s
                - Data quality score: %.1f%%
                - Processing time: 500ms
                
                Raw input captured successfully.
                """.formatted(
                    Math.min(userInput.split("\\s+").length, 10),
                    userInput.length() > 20 ? userInput.substring(0, 20) + "..." : userInput,
                    95.5
                );
    }

    private String executeSubAgent2(RequestContext context, EventQueue eventQueue, String previousResult) {
        LOGGER.debug("Executing Sub-Agent 2: Processor");
        
        simulateWork(800);
        
        return """
                **Sub-Agent 2 (Processor) Results:**
                
                - Previous output analyzed
                - Transformation applied: Content enrichment
                - Confidence score: 0.92
                - Processing time: 800ms
                
                **Enhanced Output:**
                %s
                
                [Content has been processed and enriched with metadata]
                """.formatted(previousResult.substring(0, Math.min(100, previousResult.length())));
    }

    private String executeSubAgent3(RequestContext context, EventQueue eventQueue, String previousResult) {
        LOGGER.debug("Executing Sub-Agent 3: Finalizer");
        
        simulateWork(300);
        
        return """
                **Sub-Agent 3 (Finalizer) Results:**
                
                - Final validation: PASSED
                - Format consistency: VERIFIED
                - Output optimization: COMPLETED
                - Processing time: 300ms
                
                **Final Summary:**
                All sub-agents have successfully completed their tasks.
                Total artifacts produced: 3
                Workflow status: COMPLETE
                
                **Final Result Preview:**
                %s
                """.formatted(previousResult.substring(0, Math.min(50, previousResult.length())) + "...");
    }

    private void sendProgressEvent(EventQueue eventQueue, String taskId, String contextId, String message) {
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(taskId)
                .contextId(contextId)
                .status(new TaskStatus(
                        TaskState.WORKING,
                        new Message.Builder()
                                .role(Message.Role.AGENT)
                                .parts(List.of(new TextPart(message, null)))
                                .messageId(UUID.randomUUID().toString())
                                .taskId(taskId)
                                .contextId(contextId)
                                .build(),
                        null
                ))
                .isFinal(false)
                .metadata(Map.of("progress", message))
                .build();
        
        eventQueue.enqueueEvent(event);
        LOGGER.debug("Progress event sent: {}", message);
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Work simulation interrupted");
        }
    }
}

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
    
    // Processing delays in milliseconds
    private static final long EXTRACTION_DELAY_MS = 500;
    private static final long PROCESSING_DELAY_MS = 800;
    private static final long FINALIZATION_DELAY_MS = 300;
    
    // Content limits
    private static final int MAX_PREVIEW_LENGTH = 100;
    private static final int MAX_FINAL_PREVIEW_LENGTH = 50;
    private static final int MAX_ENTITIES_COUNT = 10;
    private static final int PREVIEW_TRUNCATE_LENGTH = 20;

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
            
        } catch (JSONRPCError e) {
            LOGGER.error("JSONRPC error in sub-agent workflow: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in sub-agent workflow", e);
            throw new InternalError("Sub-agent processing failed: " + e.getMessage());
        }
    }

    private String executeSubAgent1(RequestContext context, EventQueue eventQueue) {
        LOGGER.debug("Executing Sub-Agent 1: Data Extractor");
        
        String userInput = context.getUserInput(" ");
        
        simulateWork(EXTRACTION_DELAY_MS);
        
        return """
                **Sub-Agent 1 (Data Extractor) Results:**
                
                - Extracted entities: %d
                - Key terms identified: %s
                - Data quality score: %.1f%%
                - Processing time: %dms
                
                Raw input captured successfully.
                """.formatted(
                    EXTRACTION_DELAY_MS,
                    Math.min(userInput.split("\\s+").length, MAX_ENTITIES_COUNT),
                    userInput.length() > PREVIEW_TRUNCATE_LENGTH 
                            ? userInput.substring(0, PREVIEW_TRUNCATE_LENGTH) + "..." 
                            : userInput,
                    95.5
                );
    }

    private String executeSubAgent2(RequestContext context, EventQueue eventQueue, String previousResult) {
        LOGGER.debug("Executing Sub-Agent 2: Processor");
        
        simulateWork(PROCESSING_DELAY_MS);
        
        return """
                **Sub-Agent 2 (Processor) Results:**
                
                - Previous output analyzed
                - Transformation applied: Content enrichment
                - Confidence score: 0.92
                - Processing time: %dms
                
                **Enhanced Output:**
                %s
                
                [Content has been processed and enriched with metadata]
                """.formatted(
                    PROCESSING_DELAY_MS,
                    previousResult.substring(0, Math.min(MAX_PREVIEW_LENGTH, previousResult.length()))
                );
    }

    private String executeSubAgent3(RequestContext context, EventQueue eventQueue, String previousResult) {
        LOGGER.debug("Executing Sub-Agent 3: Finalizer");
        
        simulateWork(FINALIZATION_DELAY_MS);
        
        return """
                **Sub-Agent 3 (Finalizer) Results:**
                
                - Final validation: PASSED
                - Format consistency: VERIFIED
                - Output optimization: COMPLETED
                - Processing time: %dms
                
                **Final Summary:**
                All sub-agents have successfully completed their tasks.
                Total artifacts produced: 3
                Workflow status: COMPLETE
                
                **Final Result Preview:**
                %s
                """.formatted(
                    FINALIZATION_DELAY_MS,
                    previousResult.substring(0, Math.min(MAX_FINAL_PREVIEW_LENGTH, previousResult.length())) + "..."
                );
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

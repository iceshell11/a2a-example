package io.github.a2asdk.spring.boot.starter.a2a.executor.internal;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import io.github.a2asdk.spring.boot.starter.a2a.executor.A2AExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Adapter that bridges {@link A2AExecutor} to the SDK's {@link AgentExecutor} interface.
 * 
 * This adapter handles the task lifecycle and delegates message processing to the user-provided executor.
 */
public class AgentExecutorAdapter implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentExecutorAdapter.class);

    private final A2AExecutor executor;

    public AgentExecutorAdapter(A2AExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        
        try {
            LOGGER.info("Starting task execution - taskId: {}, contextId: {}", 
                    context.getTaskId(), context.getContextId());
            
            if (context.getTask() == null) {
                updater.submit();
                LOGGER.debug("Task submitted");
            }
            
            updater.startWork();
            LOGGER.debug("Task status changed to WORKING");
            
            // Delegate to user executor
            String result = executor.execute(context.getTaskId(), context.getMessage());
            
            List<?> parts = List.of(new TextPart(result, null));
            updater.addArtifact(parts, "response", "Agent Response", null);
            LOGGER.debug("Artifact added");
            
            updater.complete();
            LOGGER.info("Task completed successfully");
            
        } catch (JSONRPCError e) {
            LOGGER.error("JSONRPC error during task execution: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during task execution", e);
            throw new InternalError("Task execution failed: " + e.getMessage());
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        Task task = context.getTask();
        
        if (task == null) {
            throw new TaskNotCancelableError("Task not found");
        }
        
        TaskState currentState = task.getStatus().state();
        if (currentState.isFinal()) {
            throw new TaskNotCancelableError(
                    "Task cannot be canceled - current state: " + currentState.asString());
        }
        
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.cancel();
        LOGGER.info("Task canceled - taskId: {}", context.getTaskId());
    }
}

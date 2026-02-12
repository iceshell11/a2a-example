package com.example.a2aspring.executor;

import com.example.a2aspring.service.SubAgentService;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MultiStepAgentExecutor extends SpringAgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiStepAgentExecutor.class);

    @Autowired
    private SubAgentService subAgentService;

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        
        try {
            LOGGER.info("Starting multi-step execution for task: {}", context.getTaskId());
            
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();
            
            String userMessage = context.getUserInput(" ");
            
            if (userMessage.toLowerCase().contains("multi-step") || 
                userMessage.toLowerCase().contains("workflow") ||
                userMessage.toLowerCase().contains("complex")) {
                
                LOGGER.info("Using sub-agent workflow");
                subAgentService.processWithSubAgents(context, eventQueue);
                
            } else {
                String result = processMessage(userMessage);
                List<Part<?>> parts = List.of(new TextPart(result, null));
                updater.addArtifact(parts, "response", "Agent Response", null);
            }
            
            updater.complete();
            LOGGER.info("Multi-step execution completed");
            
        } catch (Exception e) {
            LOGGER.error("Error in multi-step execution", e);
            throw new InternalError("Execution failed: " + e.getMessage());
        }
    }
}

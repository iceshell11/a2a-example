package io.github.a2asdk.spring.boot.starter.a2a.executor;

import io.a2a.spec.Message;

/**
 * Interface for implementing A2A agent execution logic.
 * 
 * Implement this interface to define how your agent processes messages.
 * The starter will automatically pick up your implementation.
 * 
 * Example:
 * <pre>{@code
 * @Component
 * public class MyA2AExecutor implements A2AExecutor {
 *     @Override
 *     public String execute(String taskId, Message message) {
 *         // Your custom logic here
 *         return "Processed: " + extractText(message);
 *     }
 * }
 * }</pre>
 */
public interface A2AExecutor {

    /**
     * Execute the agent logic for a given task and message.
     *
     * @param taskId the task ID
     * @param message the message to process
     * @return the response text
     */
    String execute(String taskId, Message message);

    /**
     * Extract text content from a message.
     * Utility method for implementations.
     *
     * @param message the message
     * @return concatenated text content
     */
    default String extractText(Message message) {
        if (message == null || message.getParts() == null) {
            return "";
        }
        
        StringBuilder textBuilder = new StringBuilder();
        for (var part : message.getParts()) {
            if (part instanceof io.a2a.spec.TextPart textPart) {
                if (textBuilder.length() > 0) {
                    textBuilder.append(" ");
                }
                textBuilder.append(textPart.getText());
            }
        }
        return textBuilder.toString();
    }
}

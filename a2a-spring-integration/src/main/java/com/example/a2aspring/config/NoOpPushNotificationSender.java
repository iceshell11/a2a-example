package com.example.a2aspring.config;

import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.spec.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpPushNotificationSender implements PushNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpPushNotificationSender.class);

    @Override
    public void sendNotification(Task task) {
        LOGGER.debug("Push notification suppressed for task: {} (no-op implementation)", 
                task != null ? task.getId() : "null");
    }
}

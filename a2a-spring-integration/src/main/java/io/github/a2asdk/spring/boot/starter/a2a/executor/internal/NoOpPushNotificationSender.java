package io.github.a2asdk.spring.boot.starter.a2a.executor.internal;

import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op implementation of {@link PushNotificationSender}.
 * 
 * This implementation logs notifications but does not send them.
 * Replace with a real implementation if push notifications are needed.
 */
public class NoOpPushNotificationSender implements PushNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpPushNotificationSender.class);

    @Override
    public void send(Task task, PushNotificationConfig config) {
        LOGGER.debug("NoOp: Would send notification for task {} to {}", 
                task.getId(), config.getUrl());
    }
}

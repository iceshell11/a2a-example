package com.example.a2aspring.config;

import io.a2a.server.AgentCardValidator;
import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.config.DefaultValuesConfigProvider;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class A2AConfiguration {

    @Value("${a2a.executor.core-pool-size:5}")
    private int corePoolSize;

    @Value("${a2a.executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${a2a.executor.keep-alive-seconds:60}")
    private long keepAliveSeconds;

    @Bean
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("Spring A2A Agent")
                .description("A Spring Boot integrated A2A agent")
                .url("http://localhost:8080")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("spring_agent")
                        .name("Spring A2A Agent")
                        .description("Handles tasks via Spring Boot integration")
                        .tags(Collections.singletonList("spring"))
                        .examples(List.of("process data", "analyze text"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    @Bean
    @ExtendedAgentCard
    public Optional<AgentCard> extendedAgentCard() {
        return Optional.empty();
    }

    @Bean
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    public QueueManager queueManager() {
        return new InMemoryQueueManager();
    }

    @Bean
    public PushNotificationConfigStore pushNotificationConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    @Bean
    public PushNotificationSender pushNotificationSender(NoOpPushNotificationSender noOpSender) {
        return noOpSender;
    }

    @Bean
    @Internal
    public Executor a2aExecutor() {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory()
        );
    }

    @Bean
    @Primary
    public RequestHandler requestHandler(
            AgentExecutor agentExecutor,
            TaskStore taskStore,
            QueueManager queueManager,
            PushNotificationConfigStore pushConfigStore,
            PushNotificationSender pushSender,
            @Internal Executor executor) {
        return new DefaultRequestHandler(
                agentExecutor,
                taskStore,
                queueManager,
                pushConfigStore,
                pushSender,
                executor
        );
    }

    @Bean
    public JSONRPCHandler jsonRpcHandler(
            @PublicAgentCard AgentCard agentCard,
            @ExtendedAgentCard Optional<AgentCard> extendedAgentCard,
            RequestHandler requestHandler,
            @Internal Executor executor) {
        AgentCardValidator.validateTransportConfiguration(agentCard);
        return extendedAgentCard
                .map(card -> new JSONRPCHandler(agentCard, card, requestHandler, executor))
                .orElseGet(() -> new JSONRPCHandler(agentCard, requestHandler, executor));
    }

    @Bean
    public A2AConfigProvider a2aConfigProvider() {
        return new DefaultValuesConfigProvider();
    }
}

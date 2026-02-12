package io.github.a2asdk.spring.boot.starter.a2a;

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
import io.github.a2asdk.spring.boot.starter.a2a.executor.A2AExecutor;
import io.github.a2asdk.spring.boot.starter.a2a.executor.SimpleA2AExecutor;
import io.github.a2asdk.spring.boot.starter.a2a.executor.internal.AgentExecutorAdapter;
import io.github.a2asdk.spring.boot.starter.a2a.executor.internal.NoOpPushNotificationSender;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Auto-configuration for A2A Spring Boot starter.
 * 
 * This class configures all the necessary beans for A2A protocol support.
 * Beans can be overridden by providing your own @Bean definitions.
 */
@AutoConfiguration
@ConditionalOnClass({AgentExecutor.class, JSONRPCHandler.class})
@EnableConfigurationProperties(A2AProperties.class)
@ConditionalOnProperty(prefix = "a2a", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
public class A2AAutoConfiguration {

    private final A2AProperties properties;

    public A2AAutoConfiguration(A2AProperties properties) {
        this.properties = properties;
    }

    /**
     * User-provided executor for custom agent logic.
     * Falls back to SimpleA2AExecutor if not provided.
     */
    @Bean
    @ConditionalOnMissingBean(A2AExecutor.class)
    public A2AExecutor a2aExecutor() {
        return new SimpleA2AExecutor();
    }

    /**
     * Adapter that bridges A2AExecutor to AgentExecutor interface.
     */
    @Bean
    @Primary
    public AgentExecutor agentExecutor(A2AExecutor a2aExecutor) {
        return new AgentExecutorAdapter(a2aExecutor);
    }

    /**
     * Agent card describing the agent's capabilities.
     */
    @Bean
    @PublicAgentCard
    public AgentCard agentCard() {
        A2AProperties.AgentProperties agent = properties.getAgent();
        A2AProperties.CapabilitiesProperties caps = agent.getCapabilities();
        
        return new AgentCard.Builder()
                .name(agent.getName())
                .description(agent.getDescription())
                .url(agent.getUrl())
                .version(agent.getVersion())
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(caps.isStreaming())
                        .pushNotifications(caps.isPushNotifications())
                        .stateTransitionHistory(caps.isStateTransitionHistory())
                        .build())
                .defaultInputModes(agent.getInputModes())
                .defaultOutputModes(agent.getOutputModes())
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("spring_agent")
                        .name(agent.getName())
                        .description(agent.getDescription())
                        .tags(Collections.singletonList("spring"))
                        .examples(List.of("process data", "analyze text"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    /**
     * Extended agent card (optional).
     */
    @Bean
    @ExtendedAgentCard
    @ConditionalOnMissingBean(name = "extendedAgentCard")
    public Optional<AgentCard> extendedAgentCard() {
        return Optional.empty();
    }

    /**
     * Task store for persisting task state.
     */
    @Bean
    @ConditionalOnMissingBean(TaskStore.class)
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    /**
     * Queue manager for event handling.
     */
    @Bean
    @ConditionalOnMissingBean(QueueManager.class)
    public QueueManager queueManager() {
        return new InMemoryQueueManager();
    }

    /**
     * Push notification config store.
     */
    @Bean
    @ConditionalOnMissingBean(PushNotificationConfigStore.class)
    public PushNotificationConfigStore pushNotificationConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    /**
     * Push notification sender.
     */
    @Bean
    @ConditionalOnMissingBean(PushNotificationSender.class)
    public PushNotificationSender pushNotificationSender() {
        return new NoOpPushNotificationSender();
    }

    /**
     * Executor for async operations.
     */
    @Bean
    @Internal
    @ConditionalOnMissingBean(name = "a2aExecutor")
    public Executor a2aExecutor() {
        A2AProperties.ExecutorProperties exec = properties.getExecutor();
        return new ThreadPoolExecutor(
                exec.getCorePoolSize(),
                exec.getMaxPoolSize(),
                exec.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory()
        );
    }

    /**
     * Request handler for processing A2A requests.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(RequestHandler.class)
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

    /**
     * JSON-RPC handler for transport.
     */
    @Bean
    @ConditionalOnMissingBean(JSONRPCHandler.class)
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

    /**
     * A2A configuration provider.
     */
    @Bean
    @ConditionalOnMissingBean(A2AConfigProvider.class)
    public A2AConfigProvider a2aConfigProvider() {
        return new DefaultValuesConfigProvider();
    }
}

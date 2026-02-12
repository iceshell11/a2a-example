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
import io.github.a2asdk.spring.boot.starter.a2a.executor.DefaultAgentExecutor;
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
     * Agent executor for processing A2A tasks.
     * Provide your own {@link AgentExecutor} bean to customize task processing.
     * 
     * <p>The executor has full access to:
     * <ul>
     *   <li>{@link RequestContext} - task metadata, message, session state</li>
     *   <li>{@link EventQueue} - send events, streaming updates, status changes</li>
     * </ul>
     * 
     * <p>Example custom implementation:
     * <pre>{@code
     * @Component
     * public class MyAgentExecutor implements AgentExecutor {
     *     @Override
     *     public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
     *         TaskUpdater updater = new TaskUpdater(context, eventQueue);
     *         updater.submit();
     *         updater.startWork();
     *         
     *         // Access task info
     *         String taskId = context.getTaskId();
     *         Message message = context.getMessage();
     *         
     *         // Send streaming updates
     *         updater.updateStatus(TaskState.WORKING, "Processing...");
     *         
     *         // Process and complete
     *         String result = myService.process(message);
     *         updater.addArtifact(List.of(new TextPart(result, null)), "result", "Result", null);
     *         updater.complete();
     *     }
     *     
     *     @Override
     *     public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
     *         TaskUpdater updater = new TaskUpdater(context, eventQueue);
     *         updater.cancel();
     *     }
     * }
     * }</pre>
     * 
     * @see AgentExecutor
     * @see RequestContext
     * @see EventQueue
     * @see TaskUpdater
     */
    @Bean
    @ConditionalOnMissingBean(AgentExecutor.class)
    public AgentExecutor agentExecutor() {
        return new DefaultAgentExecutor();
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

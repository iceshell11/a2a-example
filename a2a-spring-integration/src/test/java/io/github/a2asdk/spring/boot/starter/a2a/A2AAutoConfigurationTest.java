package io.github.a2asdk.spring.boot.starter.a2a;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.JSONRPCError;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import io.github.a2asdk.spring.boot.starter.a2a.executor.DefaultAgentExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for A2A auto-configuration.
 */
class A2AAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A2AAutoConfiguration.class));

    @Test
    void shouldAutoConfigureBeansWhenEnabled() {
        this.contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentExecutor.class);
                    assertThat(context).hasSingleBean(TaskStore.class);
                    assertThat(context).hasSingleBean(RequestHandler.class);
                    assertThat(context).hasSingleBean(JSONRPCHandler.class);
                    assertThat(context).hasSingleBean(PushNotificationConfigStore.class);
                    assertThat(context).hasSingleBean(PushNotificationSender.class);
                });
    }

    @Test
    void shouldNotAutoConfigureWhenDisabled() {
        this.contextRunner
                .withPropertyValues("a2a.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AgentExecutor.class);
                    assertThat(context).doesNotHaveBean(JSONRPCHandler.class);
                });
    }

    @Test
    void shouldUseDefaultExecutorWhenNoneProvided() {
        this.contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentExecutor.class);
                    assertThat(context.getBean(AgentExecutor.class)).isInstanceOf(DefaultAgentExecutor.class);
                });
    }

    @Test
    void shouldUseCustomExecutorWhenProvided() {
        this.contextRunner
                .withBean(AgentExecutor.class, () -> new AgentExecutor() {
                    @Override
                    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                        // Custom implementation
                    }
                    
                    @Override
                    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                        // Custom implementation
                    }
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentExecutor.class);
                    assertThat(context.getBean(AgentExecutor.class)).isNotInstanceOf(DefaultAgentExecutor.class);
                });
    }
}

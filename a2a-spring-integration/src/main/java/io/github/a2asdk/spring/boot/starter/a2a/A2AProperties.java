package io.github.a2asdk.spring.boot.starter.a2a;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for A2A Spring Boot starter.
 */
@ConfigurationProperties(prefix = "a2a")
@Validated
public class A2AProperties {

    /**
     * Whether A2A auto-configuration is enabled.
     */
    private boolean enabled = true;

    /**
     * Agent configuration.
     */
    private AgentProperties agent = new AgentProperties();

    /**
     * Executor thread pool configuration.
     */
    private ExecutorProperties executor = new ExecutorProperties();

    /**
     * Timeout configuration.
     */
    private TimeoutProperties timeouts = new TimeoutProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AgentProperties getAgent() {
        return agent;
    }

    public void setAgent(AgentProperties agent) {
        this.agent = agent;
    }

    public ExecutorProperties getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorProperties executor) {
        this.executor = executor;
    }

    public TimeoutProperties getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(TimeoutProperties timeouts) {
        this.timeouts = timeouts;
    }

    /**
     * Agent configuration properties.
     */
    public static class AgentProperties {

        /**
         * Agent name.
         */
        private String name = "Spring A2A Agent";

        /**
         * Agent description.
         */
        private String description = "A Spring Boot A2A agent";

        /**
         * Agent version.
         */
        private String version = "1.0.0";

        /**
         * Agent URL.
         */
        private String url = "http://localhost:8080";

        /**
         * Agent capabilities.
         */
        private CapabilitiesProperties capabilities = new CapabilitiesProperties();

        /**
         * Default input modes.
         */
        private List<String> inputModes = Collections.singletonList("text");

        /**
         * Default output modes.
         */
        private List<String> outputModes = Collections.singletonList("text");

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public CapabilitiesProperties getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(CapabilitiesProperties capabilities) {
            this.capabilities = capabilities;
        }

        public List<String> getInputModes() {
            return inputModes;
        }

        public void setInputModes(List<String> inputModes) {
            this.inputModes = inputModes;
        }

        public List<String> getOutputModes() {
            return outputModes;
        }

        public void setOutputModes(List<String> outputModes) {
            this.outputModes = outputModes;
        }
    }

    /**
     * Agent capabilities configuration.
     */
    public static class CapabilitiesProperties {

        /**
         * Whether streaming is supported.
         */
        private boolean streaming = true;

        /**
         * Whether push notifications are supported.
         */
        private boolean pushNotifications = false;

        /**
         * Whether state transition history is supported.
         */
        private boolean stateTransitionHistory = false;

        public boolean isStreaming() {
            return streaming;
        }

        public void setStreaming(boolean streaming) {
            this.streaming = streaming;
        }

        public boolean isPushNotifications() {
            return pushNotifications;
        }

        public void setPushNotifications(boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
        }

        public boolean isStateTransitionHistory() {
            return stateTransitionHistory;
        }

        public void setStateTransitionHistory(boolean stateTransitionHistory) {
            this.stateTransitionHistory = stateTransitionHistory;
        }
    }

    /**
     * Executor thread pool configuration.
     */
    public static class ExecutorProperties {

        /**
         * Core pool size for the executor.
         */
        private int corePoolSize = 5;

        /**
         * Maximum pool size for the executor.
         */
        private int maxPoolSize = 50;

        /**
         * Keep alive time in seconds for idle threads.
         */
        private long keepAliveSeconds = 60;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public long getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(long keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }

    /**
     * Timeout configuration.
     */
    public static class TimeoutProperties {

        /**
         * Timeout in seconds for blocking agent operations.
         */
        private int blockingAgent = 30;

        /**
         * Timeout in seconds for blocking consumption operations.
         */
        private int blockingConsumption = 5;

        public int getBlockingAgent() {
            return blockingAgent;
        }

        public void setBlockingAgent(int blockingAgent) {
            this.blockingAgent = blockingAgent;
        }

        public int getBlockingConsumption() {
            return blockingConsumption;
        }

        public void setBlockingConsumption(int blockingConsumption) {
            this.blockingConsumption = blockingConsumption;
        }
    }
}

package com.example.a2a.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class A2AConfiguration {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public AgentCard agentCard() {
        String baseUrl = "http://localhost:" + serverPort + contextPath;
        
        return new AgentCard.Builder()
                .name("Weather Agent")
                .description("Provides weather information for cities")
                .url(baseUrl)
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("weather_search")
                                .name("Search Weather")
                                .description("Get current weather information for any city")
                                .tags(List.of("weather", "forecast", "temperature"))
                                .examples(List.of(
                                        "What's the weather in London?",
                                        "Weather forecast for New York",
                                        "Temperature in Tokyo today"
                                ))
                                .build()
                ))
                .protocolVersion("0.3.0")
                .build();
    }
}

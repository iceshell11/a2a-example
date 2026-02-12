package com.example.a2a.service;

import com.example.a2a.model.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        CityExtractor cityExtractor = new CityExtractor();
        weatherService = new WeatherService(cityExtractor);
    }

    @Test
    void getWeather_shouldReturnWeatherInfo() {
        String result = weatherService.getWeather("London");
        
        assertNotNull(result);
        assertTrue(result.contains("London"));
        assertTrue(result.contains("°C"));
        assertTrue(result.contains("humidity"));
    }

    @Test
    void getForecast_shouldReturnForecastInfo() {
        String result = weatherService.getForecast("Paris", 3);
        
        assertNotNull(result);
        assertTrue(result.contains("Paris"));
        assertTrue(result.contains("Day 1"));
        assertTrue(result.contains("Day 2"));
        assertTrue(result.contains("Day 3"));
    }

    @Test
    void processMessage_weatherQuery_shouldUseWeatherTool() {
        ChatResponse response = weatherService.processMessage("What's the weather in Tokyo?");
        
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("Tokyo"));
        assertFalse(response.getToolCalls().isEmpty());
        assertTrue(response.getToolCalls().get(0).contains("get_weather"));
    }

    @Test
    void processMessage_forecastQuery_shouldUseForecastTool() {
        ChatResponse response = weatherService.processMessage("Give me a 5-day forecast for New York");
        
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("New York"));
        assertFalse(response.getToolCalls().isEmpty());
        assertTrue(response.getToolCalls().get(0).contains("get_forecast"));
    }

    @Test
    void processMessage_greeting_shouldReturnHelpMessage() {
        ChatResponse response = weatherService.processMessage("Hello there!");
        
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("Weather Agent"));
        assertTrue(response.getContent().contains("weather"));
        assertTrue(response.getToolCalls().isEmpty());
    }

    @Test
    void processMessage_noCity_shouldAskForCity() {
        ChatResponse response = weatherService.processMessage("weather today");

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().toLowerCase().contains("city"),
                   "Expected response to contain 'city' but got: " + response.getContent());
    }
}

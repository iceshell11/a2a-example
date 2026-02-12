package com.example.a2a.service;

import com.example.a2a.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weather Service providing current weather and forecast information.
 * Uses CityExtractor for parsing user messages.
 */
@Service
public class WeatherService {

    private static final int MIN_TEMP = 15;
    private static final int TEMP_RANGE = 20;
    private static final int MIN_HUMIDITY = 40;
    private static final int HUMIDITY_RANGE = 50;
    private static final int MAX_FORECAST_DAYS = 5;

    private final Random random;
    private final CityExtractor cityExtractor;

    public WeatherService(CityExtractor cityExtractor) {
        this.cityExtractor = cityExtractor;
        this.random = new Random();
    }

    public String getWeather(String city) {
        
        int temp = MIN_TEMP + random.nextInt(TEMP_RANGE);
        String condition = getRandomCondition();
        int humidity = MIN_HUMIDITY + random.nextInt(HUMIDITY_RANGE);
        
        return String.format(
            "Weather in %s: %d°C, %s, humidity %d%%. " +
            "Perfect weather for outdoor activities!",
            city, temp, condition, humidity
        );
    }

    public String getForecast(String city, int days) {
        
        StringBuilder forecast = new StringBuilder();
        forecast.append(String.format("Weather forecast for %s:%n", city));
        
        int actualDays = Math.min(days, MAX_FORECAST_DAYS);
        for (int i = 1; i <= actualDays; i++) {
            int temp = 10 + random.nextInt(25);
            String condition = getRandomForecastCondition();
            forecast.append(String.format("Day %d: %d°C, %s%n", i, temp, condition));
        }
        
        return forecast.toString();
    }

    public ChatResponse processMessage(String message) {
        if (message == null || message.isBlank()) {
            return createWelcomeResponse();
        }

        List<String> toolCalls = new ArrayList<>();
        String lowerMessage = message.toLowerCase();
        
        if (isWeatherQuery(lowerMessage)) {
            return handleWeatherQuery(message, toolCalls);
        }
        
        if (isForecastQuery(lowerMessage)) {
            return handleForecastQuery(message, toolCalls);
        }
        
        return createWelcomeResponse();
    }

    private ChatResponse handleWeatherQuery(String message, List<String> toolCalls) {
        String city = cityExtractor.extractCity(message);
        
        if (hasCity(city)) {
            toolCalls.add("get_weather: " + city);
            return new ChatResponse(getWeather(city), toolCalls);
        }
        
        return new ChatResponse(
            "I'd be happy to help with weather information! Which city would you like to know about?",
            toolCalls
        );
    }

    private ChatResponse handleForecastQuery(String message, List<String> toolCalls) {
        String city = cityExtractor.extractCity(message);
        
        if (hasCity(city)) {
            int days = cityExtractor.extractDays(message);
            toolCalls.add("get_forecast: " + city + " (" + days + " days)");
            return new ChatResponse(getForecast(city, days), toolCalls);
        }
        
        return new ChatResponse(
            "I can provide a weather forecast! Which city are you interested in?",
            toolCalls
        );
    }

    private ChatResponse createWelcomeResponse() {
        String welcomeMessage = "Hello! I'm a Weather Agent. I can help you with:\n" +
            "- Current weather conditions\n" +
            "- Weather forecasts\n" +
            "- Temperature information\n\n" +
            "Just ask me about the weather in any city!";
        return new ChatResponse(welcomeMessage, List.of());
    }

    private boolean isWeatherQuery(String lowerMessage) {
        return lowerMessage.contains("weather") || lowerMessage.contains("temperature");
    }

    private boolean isForecastQuery(String lowerMessage) {
        return lowerMessage.contains("forecast");
    }

    private boolean hasCity(String city) {
        return city != null && !city.isEmpty();
    }

    private String getRandomCondition() {
        String[] conditions = {"sunny", "cloudy", "rainy", "partly cloudy"};
        return conditions[random.nextInt(conditions.length)];
    }

    private String getRandomForecastCondition() {
        String[] conditions = {"sunny", "cloudy", "rainy", "partly cloudy", "stormy"};
        return conditions[random.nextInt(conditions.length)];
    }
}

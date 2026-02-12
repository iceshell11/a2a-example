package com.example.a2a.service;

import com.example.a2a.common.A2AConstants;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts city names and forecast days from user messages.
 * Separates parsing logic from business logic.
 */
@Component
public class CityExtractor {

    private static final String[] CITY_PATTERNS = {"in ", "for ", "at "};
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[?.,!]$");
    private static final Pattern DAY_PATTERN = Pattern.compile("\\b(\\d+)\\s*day", Pattern.CASE_INSENSITIVE);
    private static final String[] NUMBER_WORDS = {"two", "three", "four", "five"};

    /**
     * Extract city name from a message.
     * 
     * @param message the user message
     * @return extracted city name or null if not found
     */
    public String extractCity(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String city = extractCityFromPatterns(message);
        if (city != null) {
            return city;
        }

        return extractCityFromCapitalizedWords(message);
    }

    /**
     * Extract number of forecast days from a message.
     * 
     * @param message the user message
     * @return number of days (defaults to 3)
     */
    public int extractDays(String message) {
        if (message == null || message.isBlank()) {
            return A2AConstants.Defaults.FORECAST_DAYS_DEFAULT;
        }

        String lower = message.toLowerCase();

        if (lower.contains("next week")) {
            return 7;
        }
        if (lower.contains("tomorrow")) {
            return 2;
        }

        int daysFromWords = extractDaysFromWords(lower);
        if (daysFromWords > 0) {
            return daysFromWords;
        }

        return extractDaysFromNumbers(message);
    }

    private String extractCityFromPatterns(String message) {
        String lower = message.toLowerCase();

        for (String pattern : CITY_PATTERNS) {
            int idx = lower.lastIndexOf(pattern);
            if (idx != -1) {
                String after = message.substring(idx + pattern.length()).trim();
                after = removePunctuation(after);

                if (!after.isEmpty()) {
                    return extractFirstWordBeforeModifiers(after);
                }
            }
        }
        return null;
    }

    private String extractCityFromCapitalizedWords(String message) {
        String[] words = message.split("\\s+");

        for (String word : words) {
            if (isPotentialCityName(word)) {
                return removePunctuation(word);
            }
        }
        return null;
    }

    private boolean isPotentialCityName(String word) {
        if (word.length() <= 1) {
            return false;
        }
        if (!Character.isUpperCase(word.charAt(0))) {
            return false;
        }
        String cleaned = removePunctuation(word);
        return !cleaned.equals("I") && !cleaned.equals("A");
    }

    private String extractFirstWordBeforeModifiers(String text) {
        return text.split("\\s+(today|now|currently|tomorrow)")[0].trim();
    }

    private String removePunctuation(String text) {
        return PUNCTUATION_PATTERN.matcher(text).replaceAll("").trim();
    }

    private int extractDaysFromWords(String lower) {
        for (int i = 0; i < NUMBER_WORDS.length; i++) {
            if (lower.contains(NUMBER_WORDS[i] + " day")) {
                return i + 2;
            }
        }
        return 0;
    }

    private int extractDaysFromNumbers(String message) {
        Matcher matcher = DAY_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                int days = Integer.parseInt(matcher.group(1));
                return Math.min(Math.max(days, 1), 7);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return A2AConstants.Defaults.FORECAST_DAYS_DEFAULT;
    }
}

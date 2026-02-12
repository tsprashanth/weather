package com.weather.model;

/**
 * Response DTO returned by our forecast endpoint.
 */
public record ForecastResponse(
        double latitude,
        double longitude,
        String shortForecast,
        int temperatureF,
        String temperatureCharacterization
) {}

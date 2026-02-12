package com.weather.service;

import com.weather.model.ForecastResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Calls the National Weather Service API to retrieve forecasts.
 *
 * NWS API flow:
 *   1. GET /points/{lat},{lon}  -> returns metadata including a forecast URL
 *   2. GET {forecastUrl}        -> returns period-based forecasts (Today, Tonight, etc.)
 *
 * Shortcuts / trade-offs:
 *   - No caching: The /points response is essentially static for a given coordinate and
 *     could be cached indefinitely. The forecast itself updates roughly hourly. In production
 *     we'd add a cache (e.g. Caffeine or Redis) to reduce latency and NWS load.
 *   - We parse the JSON with raw Maps instead of full POJOs. This keeps the code compact
 *     but is fragile if the NWS schema changes. A production service should use typed DTOs.
 *   - No retry/circuit-breaker logic. NWS can be intermittent; we'd add resilience4j or
 *     similar in production.
 *   - Temperature thresholds are subjective: >=85°F = hot, <=50°F = cold, else moderate.
 *     A more sophisticated approach might vary by region or season.
 */
@Service
public class NwsForecastService {

    private static final String NWS_BASE = "https://api.weather.gov";
    private static final int HOT_THRESHOLD_F = 85;
    private static final int COLD_THRESHOLD_F = 50;

    private final RestClient restClient;

    public NwsForecastService() {
        // NWS requires a User-Agent identifying the application
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "(weather-service, contact@example.com)")
                .defaultHeader("Accept", "application/geo+json")
                .build();
    }

    /**
     * Fetches the forecast for today at the given coordinates.
     *
     * @throws WeatherServiceException if the NWS API returns an error or unexpected data
     */
    public ForecastResponse getForecast(double latitude, double longitude) {
        // Step 1: Resolve the grid point / forecast URL from coordinates
        String forecastUrl = resolveForecastUrl(latitude, longitude);

        // Step 2: Fetch the full forecast
        Map<String, Object> forecastResponse = restClient.get()
                .uri(forecastUrl)
                .retrieve()
                .body(Map.class);

        // Step 3: Find "Today" (or the first daytime period) in the forecast
        Map<String, Object> todayPeriod = extractTodayPeriod(forecastResponse);

        String shortForecast = (String) todayPeriod.get("shortForecast");
        int temperature = (int) todayPeriod.get("temperature");
        String characterization = characterizeTemperature(temperature);

        return new ForecastResponse(latitude, longitude, shortForecast, temperature, characterization);
    }

    /**
     * Calls GET /points/{lat},{lon} and extracts the forecast URL.
     */
    @SuppressWarnings("unchecked")
    private String resolveForecastUrl(double latitude, double longitude) {
        String pointsUrl = String.format("%s/points/%.4f,%.4f", NWS_BASE, latitude, longitude);

        Map<String, Object> response;
        try {
            response = restClient.get()
                    .uri(pointsUrl)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new WeatherServiceException(
                    "Failed to resolve grid point from NWS. The coordinates may be outside US coverage. " +
                    "Detail: " + e.getMessage());
        }

        Map<String, Object> properties = (Map<String, Object>) response.get("properties");
        if (properties == null || !properties.containsKey("forecast")) {
            throw new WeatherServiceException("NWS did not return a forecast URL for the given coordinates.");
        }
        return (String) properties.get("forecast");
    }

    /**
     * Extracts the first daytime forecast period (typically "Today" or "This Afternoon").
     * If it's evening, the first period might be "Tonight" which is nighttime — we still
     * return it as the most current forecast rather than skipping to tomorrow.
     *
     * Shortcut: In production we might want smarter logic (e.g. always prefer daytime,
     * or return both day and night forecasts).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTodayPeriod(Map<String, Object> forecastResponse) {
        Map<String, Object> properties = (Map<String, Object>) forecastResponse.get("properties");
        List<Map<String, Object>> periods = (List<Map<String, Object>>) properties.get("periods");

        if (periods == null || periods.isEmpty()) {
            throw new WeatherServiceException("NWS returned no forecast periods.");
        }

        // The first period is always the current/next period (Today, This Afternoon, Tonight, etc.)
        return periods.get(0);
    }

    /**
     * Maps a temperature in Fahrenheit to a human-readable characterization.
     *
     * Thresholds:
     *   - Hot:      >= 85°F  (feels like a warm summer day)
     *   - Cold:     <= 50°F  (you'd want a jacket)
     *   - Moderate: everything in between
     */
    private String characterizeTemperature(int temperatureF) {
        if (temperatureF >= HOT_THRESHOLD_F) {
            return "hot";
        } else if (temperatureF <= COLD_THRESHOLD_F) {
            return "cold";
        } else {
            return "moderate";
        }
    }
}

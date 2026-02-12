package com.weather.controller;

import com.weather.model.ForecastResponse;
import com.weather.service.NwsForecastService;
import com.weather.service.WeatherServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ForecastController {

    private final NwsForecastService forecastService;

    public ForecastController(NwsForecastService forecastService) {
        this.forecastService = forecastService;
    }

    /**
     * GET /forecast?latitude={lat}&longitude={lon}
     *
     * Returns the short forecast and temperature characterization for today.
     *
     * Example:
     *   GET /forecast?latitude=39.7456&longitude=-97.0892
     *
     *   {
     *     "latitude": 39.7456,
     *     "longitude": -97.0892,
     *     "shortForecast": "Mostly Sunny",
     *     "temperatureF": 72,
     *     "temperatureCharacterization": "moderate"
     *   }
     *
     * Note: The NWS API only covers US locations. Coordinates outside the US will return an error.
     */
    @GetMapping("/forecast")
    public ResponseEntity<?> getForecast(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        // Basic coordinate validation
        if (latitude < -90 || latitude > 90) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Latitude must be between -90 and 90."));
        }
        if (longitude < -180 || longitude > 180) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Longitude must be between -180 and 180."));
        }

        try {
            ForecastResponse forecast = forecastService.getForecast(latitude, longitude);
            return ResponseEntity.ok(forecast);
        } catch (WeatherServiceException e) {
            return ResponseEntity.badGateway().body(
                    Map.of("error", e.getMessage()));
        }
    }
}

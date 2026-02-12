package com.weather.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the temperature characterization logic in isolation.
 *
 * Uses reflection to test the private method directly. In production code,
 * we might extract this into a utility class to make it more testable.
 */
class TemperatureCharacterizationTest {

    private final NwsForecastService service = new NwsForecastService();

    @ParameterizedTest
    @CsvSource({
            "100, hot",
            "85,  hot",
            "84,  moderate",
            "65,  moderate",
            "51,  moderate",
            "50,  cold",
            "30,  cold",
            "0,   cold",
            "-10, cold"
    })
    void characterizeTemperature(int tempF, String expected) throws Exception {
        Method method = NwsForecastService.class.getDeclaredMethod("characterizeTemperature", int.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, tempF);
        assertEquals(expected, result);
    }
}

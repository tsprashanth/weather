package com.weather.controller;

import com.weather.model.ForecastResponse;
import com.weather.service.NwsForecastService;
import com.weather.service.WeatherServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ForecastController.class)
class ForecastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NwsForecastService forecastService;

    @Test
    void getForecast_returnsOk() throws Exception {
        var response = new ForecastResponse(39.7456, -97.0892, "Mostly Sunny", 72, "moderate");
        when(forecastService.getForecast(39.7456, -97.0892)).thenReturn(response);

        mockMvc.perform(get("/forecast")
                        .param("latitude", "39.7456")
                        .param("longitude", "-97.0892"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortForecast").value("Mostly Sunny"))
                .andExpect(jsonPath("$.temperatureF").value(72))
                .andExpect(jsonPath("$.temperatureCharacterization").value("moderate"));
    }

    @Test
    void getForecast_invalidLatitude_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/forecast")
                        .param("latitude", "999")
                        .param("longitude", "-97.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getForecast_nwsError_returnsBadGateway() throws Exception {
        when(forecastService.getForecast(0.0, 0.0))
                .thenThrow(new WeatherServiceException("NWS error"));

        mockMvc.perform(get("/forecast")
                        .param("latitude", "0.0")
                        .param("longitude", "0.0"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("NWS error"));
    }
}

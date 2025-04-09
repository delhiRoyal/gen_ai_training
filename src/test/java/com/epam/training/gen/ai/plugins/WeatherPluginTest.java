package com.epam.training.gen.ai.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class WeatherPluginTest {

    @InjectMocks
    private WeatherPlugin weatherPlugin;

    @Test
    void getCurrentWeather_London() {
        String result = weatherPlugin.getCurrentWeather("London");
        assertEquals("The current weather in London is: Cloudy, 15°C.", result);
    }

    @Test
    void getCurrentWeather_Paris() {
        String result = weatherPlugin.getCurrentWeather("Paris");
        assertEquals("The current weather in Paris is: Sunny, 22°C.", result);
    }

    @Test
    void getCurrentWeather_NewYork() {
        String result = weatherPlugin.getCurrentWeather("New York");
        assertEquals("The current weather in New York is: Partly Cloudy, 18°C.", result);
    }

    @Test
    void getCurrentWeather_UnknownCity() {
        String result = weatherPlugin.getCurrentWeather("UnknownCity");
        assertEquals("Weather information not available for UnknownCity.", result);
    }

    @Test
    void getWeatherForecast_London() {
        String result = weatherPlugin.getWeatherForecast("London", "3");
        assertEquals("The weather forecast for London for the next 3 days is: Rain expected.", result);
    }

    @Test
    void getWeatherForecast_Paris() {
        String result = weatherPlugin.getWeatherForecast("Paris", "7");
        assertEquals("The weather forecast for Paris for the next 7 days is: Sunny.", result);
    }

    @Test
    void getWeatherForecast_NewYork() {
        String result = weatherPlugin.getWeatherForecast("New York", "1");
        assertEquals("The weather forecast for New York for the next 1 days is: Variable.", result);
    }

    @Test
    void getWeatherForecast_UnknownCity() {
        String result = weatherPlugin.getWeatherForecast("UnknownCity", "5");
        assertEquals("Weather forecast not available for UnknownCity.", result);
    }
}
package com.epam.training.gen.ai.plugins;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WeatherPlugin {

    @DefineKernelFunction(
            name = "GetCurrentWeather",
            description = "Gets the current weather for a specified city."
    )
    public String getCurrentWeather(
            @KernelFunctionParameter(name = "city", description = "The city to get the weather for.") String city
    ) {
        log.info("Getting current weather for: {}", city);
        // In a real implementation, you would call an external weather API here.
        // For this example, we're mocking the response.
        if (city.equalsIgnoreCase("London")) {
            return "The current weather in London is: Cloudy, 15°C.";
        } else if (city.equalsIgnoreCase("Paris")) {
            return "The current weather in Paris is: Sunny, 22°C.";
        } else if (city.equalsIgnoreCase("New York")) {
            return "The current weather in New York is: Partly Cloudy, 18°C.";
        } else {
            return "Weather information not available for " + city + ".";
        }
    }

    @DefineKernelFunction(
            name = "GetWeatherForecast",
            description = "Gets the weather forecast for a specified city."
    )
    public String getWeatherForecast(
            @KernelFunctionParameter(name = "city", description = "The city to get the weather forecast for.") String city,
            @KernelFunctionParameter(name = "days", description = "Number of days for the forecast") String days
    ) {
        log.info("Getting weather forecast for: {} for {} days", city, days);
        // Mocking the forecast response.
        if (city.equalsIgnoreCase("London")) {
            return "The weather forecast for London for the next " + days + " days is: Rain expected.";
        } else if (city.equalsIgnoreCase("Paris")) {
            return "The weather forecast for Paris for the next " + days + " days is: Sunny.";
        } else if (city.equalsIgnoreCase("New York")) {
            return "The weather forecast for New York for the next " + days + " days is: Variable.";
        } else {
            return "Weather forecast not available for " + city + ".";
        }
    }
}
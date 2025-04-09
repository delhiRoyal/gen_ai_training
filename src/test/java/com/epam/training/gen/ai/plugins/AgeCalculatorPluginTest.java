package com.epam.training.gen.ai.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AgeCalculatorPluginTest {

    @InjectMocks
    private AgeCalculatorPlugin ageCalculatorPlugin;

    @Test
    void calculateAge_ValidDate_ReturnsCorrectAge() {
        // Arrange
        String dateOfBirth = "1990-05-15";
        int expectedAge = java.time.Period.between(java.time.LocalDate.parse(dateOfBirth), java.time.LocalDate.now()).getYears();

        // Act
        String result = ageCalculatorPlugin.calculateAge(dateOfBirth);

        // Assert
        assertEquals(String.valueOf(expectedAge), result);
    }

    @Test
    void calculateAge_InvalidDateFormat_ReturnsErrorMessage() {
        // Arrange
        String dateOfBirth = "1990/05/15";
        String expectedErrorMessage = "Invalid date format. Please use YYYY-MM-DD.";

        // Act
        String result = ageCalculatorPlugin.calculateAge(dateOfBirth);

        // Assert
        assertEquals(expectedErrorMessage, result);
    }

    @Test
    void calculateAge_InvalidDate_ReturnsErrorMessage() {
        // Arrange
        String dateOfBirth = "invalid-date";
        String expectedErrorMessage = "Invalid date format. Please use YYYY-MM-DD.";

        // Act
        String result = ageCalculatorPlugin.calculateAge(dateOfBirth);

        // Assert
        assertEquals(expectedErrorMessage, result);
    }
    
    @Test
    void calculateAge_FutureDate_ReturnsNegativeAge() {
        // Arrange
        String dateOfBirth = "2025-05-15";
        int expectedAge = java.time.Period.between(java.time.LocalDate.parse(dateOfBirth), java.time.LocalDate.now()).getYears();

        // Act
        String result = ageCalculatorPlugin.calculateAge(dateOfBirth);

        // Assert
        assertEquals(String.valueOf(expectedAge), result);
    }
}
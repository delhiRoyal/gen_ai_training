package com.epam.training.gen.ai.plugins;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@Slf4j
public class AgeCalculatorPlugin {

    @DefineKernelFunction(
            name = "CalculateAge",
            description = "Calculates the age based on the provided date of birth."
    )
    public String calculateAge(
            @KernelFunctionParameter(name = "dateOfBirth",description = "Date of birth in YYYY-MM-DD format") String dateOfBirth
    ) {
        log.info("Calculating age for date of birth: {}", dateOfBirth);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate birthDate = LocalDate.parse(dateOfBirth, formatter);
            LocalDate currentDate = LocalDate.now();
            Period period = Period.between(birthDate, currentDate);
            int age = period.getYears();
            log.info("Calculated age: {}", age);
            return String.valueOf(age);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", dateOfBirth, e);
            return "Invalid date format. Please use YYYY-MM-DD.";
        } catch (Exception e) {
            log.error("Error calculating age: {}", e.getMessage(), e);
            return "Error calculating age.";
        }
    }
}
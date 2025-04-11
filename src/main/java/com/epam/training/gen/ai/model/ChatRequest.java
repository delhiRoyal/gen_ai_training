package com.epam.training.gen.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String input;
    private Double temperature;
    private String deployment;
    private String sourceFilename;


    public ChatRequest(String input) {
        this.input = input;
    }

    public ChatRequest(String input, Double temperature){
        this.input = input;
        this.temperature = temperature;
    }

    public ChatRequest(String input, Double temperature, String deployment) {
        this.input = input;
        this.temperature = temperature;
        this.deployment = deployment;
    }
}
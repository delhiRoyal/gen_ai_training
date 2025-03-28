package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.model.ChatRequest;
import com.epam.training.gen.ai.model.ChatResponse;
import com.epam.training.gen.ai.service.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
public class ChatBotController {

    private final ChatBotService chatBotService;

    @Autowired
    public ChatBotController(ChatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }

    @GetMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestParam String prompt,
            @RequestParam(required = false) Double temperature,
            @RequestParam(required = false, defaultValue = "openAI") String deployment) { // Added deployment parameter
        try {
            String response = chatBotService.getChatBotResponse(prompt, temperature, deployment);
            return ResponseEntity.ok(new ChatResponse(response, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ChatResponse(null, "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatPost(@RequestBody ChatRequest request) {
        try {
            if (request.getInput() == null || request.getInput().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ChatResponse(null, "Input prompt cannot be empty."));
            }
            // Default to "openAI" if deployment is not provided
            String deployment = request.getDeployment() == null ? "openAI" : request.getDeployment();
            String response = chatBotService.getChatBotResponse(request.getInput(), request.getTemperature(), deployment);
            return ResponseEntity.ok(new ChatResponse(response, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ChatResponse(null, "An error occurred: " + e.getMessage()));
        }
    }
}
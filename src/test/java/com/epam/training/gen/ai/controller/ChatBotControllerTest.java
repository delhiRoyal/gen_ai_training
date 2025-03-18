package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.service.ChatBotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatBotControllerTest {

    @Mock
    private ChatBotService chatBotService;

    @InjectMocks
    private ChatBotController chatBotController;

    @Test
    void chat_ValidPrompt_ReturnsOkResponse() throws Exception {
        // Arrange
        String prompt = "Hello";
        String expectedResponse = "Hi there!";
        when(chatBotService.getChatBotResponse(prompt)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, String>> responseEntity = chatBotController.chat(prompt);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(expectedResponse, responseEntity.getBody().get("response"));
        verify(chatBotService, times(1)).getChatBotResponse(prompt);
    }

    @Test
    void chat_ExceptionThrown_ReturnsInternalServerError() throws Exception {
        // Arrange
        String prompt = "Error";
        when(chatBotService.getChatBotResponse(prompt)).thenThrow(new RuntimeException("Test exception"));

        // Act
        ResponseEntity<Map<String, String>> responseEntity = chatBotController.chat(prompt);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().get("error").contains("An error occurred"));
        verify(chatBotService, times(1)).getChatBotResponse(prompt);
    }

    @Test
    void chatPost_ValidRequest_ReturnsOkResponse() throws Exception {
        // Arrange
        Map<String, String> requestBody = Map.of("input", "How are you?");
        String expectedResponse = "I'm good, thank you!";
        when(chatBotService.getChatBotResponse(requestBody.get("input"))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, String>> responseEntity = chatBotController.chatPost(requestBody);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(expectedResponse, responseEntity.getBody().get("response"));
        verify(chatBotService, times(1)).getChatBotResponse(requestBody.get("input"));
    }

    @Test
    void chatPost_EmptyPrompt_ReturnsBadRequest() throws Exception {
        // Arrange
        Map<String, String> requestBody = Map.of("input", "");

        // Act
        ResponseEntity<Map<String, String>> responseEntity = chatBotController.chatPost(requestBody);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().get("error").contains("Input prompt cannot be empty"));
        verify(chatBotService, never()).getChatBotResponse(anyString());
    }

    @Test
    void chatPost_NullPrompt_ReturnsBadRequest() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("input", null);

        // Act
        ResponseEntity<Map<String, String>> responseEntity = chatBotController.chatPost(requestBody);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().get("error").contains("Input prompt cannot be empty"));
        verify(chatBotService, never()).getChatBotResponse(anyString());
    }

    @Test
    void chatPost_ExceptionThrown_ReturnsInternalServerError() throws Exception {
        // Arrange
        Map<String, String> requestBody = Map.of("input", "Throw Exception");
        when(chatBotService.getChatBotResponse(requestBody.get("input"))).thenThrow(new RuntimeException("Test Exception"));

        // Act
        ResponseEntity<Map<String, String>> responseEntity = chatBotController.chatPost(requestBody);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().get("error").contains("An error occurred"));
        verify(chatBotService, times(1)).getChatBotResponse(requestBody.get("input"));
    }
}
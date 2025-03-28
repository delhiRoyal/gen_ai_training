package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.model.ChatRequest;
import com.epam.training.gen.ai.model.ChatResponse;
import com.epam.training.gen.ai.service.ChatBotService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebMvcTest(ChatBotController.class)
public class ChatBotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatBotService chatBotService;

    @Test
    public void testChatGetEndpoint() throws Exception {
        String expectedResponse = "Test response";
        when(chatBotService.getChatBotResponse(anyString(), anyDouble(), anyString())).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/chat")
                        .param("prompt", "Test prompt")
                        .param("temperature", "0.5")
                        .param("deployment", "mistral")) // Added deployment parameter
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ChatResponse response = new ChatResponse(expectedResponse, null);
        assertEquals(content, "{\"response\":\"Test response\",\"error\":null}");
    }

    @Test
    public void testChatPostEndpoint() throws Exception {
        String expectedResponse = "Test response";
        when(chatBotService.getChatBotResponse(anyString(), anyDouble(), anyString())).thenReturn(expectedResponse);

        ChatRequest request = new ChatRequest("Test prompt", 0.5, "deepseek"); // Added deployment in request
        String requestJson = "{\"input\":\"Test prompt\",\"temperature\":0.5,\"deployment\":\"deepseek\"}";

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(content, "{\"response\":\"Test response\",\"error\":null}");
    }

    @Test
    public void testChatPostEndpoint_emptyInput() throws Exception {
        ChatRequest request = new ChatRequest("", 0.5, "deepseek"); // Added deployment in request
        String requestJson = "{\"input\":\"\",\"temperature\":0.5,\"deployment\":\"deepseek\"}";

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(content, "{\"response\":null,\"error\":\"Input prompt cannot be empty.\"}");
    }

    @Test
    public void testChatGetEndpoint_defaultDeployment() throws Exception {
        String expectedResponse = "Test response";
        when(chatBotService.getChatBotResponse(anyString(), anyDouble(), anyString())).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/chat")
                        .param("prompt", "Test prompt")
                        .param("temperature", "0.5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ChatResponse response = new ChatResponse(expectedResponse, null);
        assertEquals(content, "{\"response\":\"Test response\",\"error\":null}");
    }

    @Test
    public void testChatPostEndpoint_defaultDeployment() throws Exception {
        String expectedResponse = "Test response";
        when(chatBotService.getChatBotResponse(anyString(), anyDouble(), anyString())).thenReturn(expectedResponse);

        ChatRequest request = new ChatRequest("Test prompt", 0.5, null); // Added deployment in request
        String requestJson = "{\"input\":\"Test prompt\",\"temperature\":0.5}";

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(content, "{\"response\":\"Test response\",\"error\":null}");
    }
}
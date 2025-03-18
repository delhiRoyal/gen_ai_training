package com.epam.training.gen.ai.service;


import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatBotService {


    public static final String NO_RESPONSE_ERROR_HANDLING = "Sorry the AI agent is not available at the moment, Try Again later!";
    @Value("${client-openai-deployment-name}")
    private String openAiDeploymentName;

    @Autowired
    private Kernel kernel;
    @Autowired
    private ChatCompletionService chatCompletionService;
    @Autowired
    private ChatHistory chatHistory;
    @Autowired
    private InvocationContext invocationContext;



    public String getChatBotResponse(String prompt) {
        chatHistory.addUserMessage(prompt);
        try {
            log.info("getChatBotResponse  prompt {} " , prompt);
            List<ChatMessageContent<?>> results = chatCompletionService
                    .getChatMessageContentsAsync(chatHistory, kernel, invocationContext)
                    .block();

            if (results == null) {
                log.trace("Could NOT get AI response on user input");
                return NO_RESPONSE_ERROR_HANDLING;

            }

            var response = results.stream()
                    .filter(result -> result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null)
                    .map(ChatMessageContent::getContent)
                    .collect(Collectors.joining(" "));

            log.info("Assistant > {} " , response);
            chatHistory.addAssistantMessage(response);
            return response;
        } catch (Exception e){
            log.error("Error while creating chatbot message: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
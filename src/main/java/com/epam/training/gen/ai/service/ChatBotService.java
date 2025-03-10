package com.epam.training.gen.ai.service;


import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ChatBotService {




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
            for (ChatMessageContent<?> result : results) {
                // Print the results
                if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
                    System.out.println("Assistant > " + result);
                }
                // Add the message from the agent to the chat history
                chatHistory.addMessage(result);
            }
            return results.toString();
        } catch (Exception e){
            log.error("Error while creating chatbot message: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
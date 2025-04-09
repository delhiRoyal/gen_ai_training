package com.epam.training.gen.ai.service;


import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatBotService {


    public static final String NO_RESPONSE_ERROR_HANDLING = "Sorry the AI agent is not available at the moment, Try Again later!";

    @Value("${DEFAULT_TEMPERATURE}")
    private Double defaultTemperature;


    private final Map<String, ChatCompletionService> chatCompletionServices = new HashMap<>();

    @Autowired
    private ChatHistory chatHistory;

    @Autowired
    @Qualifier("ageCalculator")
    private KernelPlugin ageCalculatorPlugin;

    @Autowired
    @Qualifier("weather")
    private KernelPlugin weatherPlugin;


    @Autowired
    public ChatBotService(
            @Qualifier("openAI") ChatCompletionService openAIChatCompletionService,
            @Qualifier("mistral") ChatCompletionService mistralChatCompletionService,
            @Qualifier("deepseek") ChatCompletionService deepSeekChatCompletionService) {
        chatCompletionServices.put("openAI", openAIChatCompletionService);
        chatCompletionServices.put("mistral", mistralChatCompletionService);
        chatCompletionServices.put("deepseek", deepSeekChatCompletionService);
    }


    public String getChatBotResponse(String prompt, Double temperature, String deployment) {
        chatHistory.addUserMessage(prompt);
        log.info("Creating InvocationContext with temperature: {}, deployment: {}", temperature, deployment);
        InvocationContext invocationContext = invocationContext(temperature == null ? defaultTemperature : temperature);
        ChatCompletionService chatCompletionService = chatCompletionServices.get(deployment);
        Kernel kernel = kernel(chatCompletionService);

        try {
            log.info("getChatBotResponse  prompt {} ", prompt);
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

            if(deployment.equals("deepseek")){
                int thinkEndIndex = response.indexOf("</think>") + 9;
                response = response.substring(thinkEndIndex);
            }

            log.info("Assistant > {} ", response);

            chatHistory.addAssistantMessage(response);
            return response;
        } catch (Exception e) {
            log.error("Error while creating chatbot message: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }


    /**
     * Creates an {@link InvocationContext} bean with default prompt
     * execution settings and provided temperature.
     *
     * @return an instance of {@link InvocationContext}
     */
    public InvocationContext invocationContext(Double temperature) {
        return InvocationContext.builder()
                .withPromptExecutionSettings(PromptExecutionSettings.builder()
                        .withTemperature(temperature)
                        .build())
                .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
                .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
                .build();
    }

    /**
     * Creates a {@link Kernel} bean to manage AI services and plugins.
     *
     * @param chatCompletionService the {@link ChatCompletionService} for handling completions
     * @return an instance of {@link Kernel}
     */
    public Kernel kernel(ChatCompletionService chatCompletionService) {
        return Kernel.builder()
                .withAIService(ChatCompletionService.class, chatCompletionService)
                .withPlugin(ageCalculatorPlugin)
                .withPlugin(weatherPlugin)
                .build();
    }
}
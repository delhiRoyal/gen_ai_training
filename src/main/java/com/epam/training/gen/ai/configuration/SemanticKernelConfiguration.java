package com.epam.training.gen.ai.configuration;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.epam.training.gen.ai.plugins.AgeCalculatorPlugin;
import com.epam.training.gen.ai.plugins.SimplePlugin;
import com.epam.training.gen.ai.plugins.WeatherPlugin;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up Semantic Kernel components.
 * <p>
 * This configuration provides several beans necessary for the interaction with
 * Azure OpenAI services and the creation of kernel plugins. It defines beans for
 * chat completion services, kernel plugins, kernel instance, invocation context,
 * and prompt execution settings.
 */
@Configuration
public class SemanticKernelConfiguration {

    /**
     * Creates a {@link KernelPlugin} bean using a simple plugin.
     *
     * @return an instance of {@link KernelPlugin}
     */
    @Bean
    public KernelPlugin kernelPlugin() {
        return KernelPluginFactory.createFromObject(
                new SimplePlugin(), "Simple Plugin");
    }

    /**
     * Creates a {@link ChatCompletionService} bean for handling chat completions using Azure OpenAI.
     *
     * @param deploymentOrModelName the Azure OpenAI deployment or model name
     * @param openAIAsyncClient the {@link OpenAIAsyncClient} to communicate with Azure OpenAI
     * @return an instance of {@link ChatCompletionService}
     */
    @Bean
    @Qualifier("openAI")
    public ChatCompletionService openAIChatCompletionService(@Value("${client-openai-deployment-name}") String deploymentOrModelName,
                                                       OpenAIAsyncClient openAIAsyncClient) {
        return OpenAIChatCompletion.builder()
                .withModelId(deploymentOrModelName)
                .withOpenAIAsyncClient(openAIAsyncClient)
                .build();
    }

    @Bean
    @Qualifier("ageCalculator")
    public KernelPlugin ageCalculatorKernelPlugin(AgeCalculatorPlugin ageCalculatorPlugin) {
        return KernelPluginFactory.createFromObject(ageCalculatorPlugin, "AgeCalculatorPlugin");
    }

    @Bean
    @Qualifier("weather")
    public KernelPlugin weatherKernelPlugin(WeatherPlugin weatherPlugin) {
        return KernelPluginFactory.createFromObject(weatherPlugin, "WeatherPlugin");
    }

    @Bean
    @Qualifier("mistral")
    public ChatCompletionService mistralCchatCompletionService(@Value("${client-mistral-deployment-name}") String deploymentOrModelName,
                                                       OpenAIAsyncClient openAIAsyncClient) {
        return OpenAIChatCompletion.builder()
                .withModelId(deploymentOrModelName)
                .withOpenAIAsyncClient(openAIAsyncClient)
                .build();
    }

    @Bean
    @Qualifier("deepseek")
    public ChatCompletionService deepSeekChatCompletionService(@Value("${client-deepseek-deployment-name}") String deploymentOrModelName,
                                                               OpenAIAsyncClient openAIAsyncClient) {
        return OpenAIChatCompletion.builder()
                .withModelId(deploymentOrModelName)
                .withOpenAIAsyncClient(openAIAsyncClient)
                .build();
    }


    @Bean
    public ChatHistory chatHistory(){
        return new ChatHistory();
    }

}


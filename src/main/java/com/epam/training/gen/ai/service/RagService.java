package com.epam.training.gen.ai.service;

import com.epam.training.gen.ai.model.ChatResponse;
import com.epam.training.gen.ai.model.SearchResult;
import com.epam.training.gen.ai.util.DataExtraction;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagService {

    public static final String FAILED_TO_GET_RESPONSE = "FAILED_TO_GET_RESPONSE";

    private final EmbeddingService embeddingService;

    private final DataExtraction dataExtraction;

    private final ChatBotService chatBotService;

    @Value("${rag.search.limit}")
    private int searchResultLimit;

    @Value("${rag.prompt.template}")
    private String ragPromptTemplate;

    @Value("${rag.enhance-query.template}")
    private String enhanceQueryTemplate;

    @Value("${rag.hyde.template}")
    private String ragHydeTemplate;

    @Value("${embedding.chunk.size}")
    private int chunkSize;

    @Autowired
    private ChatHistory chatHistory;

    @Autowired
    public RagService(EmbeddingService embeddingService, DataExtraction dataExtraction, ChatBotService chatBotService) {
        this.embeddingService = embeddingService;
        this.dataExtraction = dataExtraction;
        this.chatBotService = chatBotService;
    }

    public ChatResponse answerQuestion(String question, String deployment, double temperature, String sourceFilename) {
        if(sourceFilename==null){
            return new ChatResponse(chatBotService.getChatBotResponse(question, temperature, deployment), null);
        }

        log.info("Received RAG question: '{}' using deployment: {}, temp: {}, sourceFile: {}",
                question, deployment, temperature, sourceFilename);

        try {

            // 1. Rewrite the query for better retrieval
            String rewrittenQuery = rewriteQuery(question, deployment);
            log.info("Original query: '{}', Rewritten query: '{}'", question, rewrittenQuery);

            //2. Get Hypothetical document to query relevant document
            String hypotheticalDocument = createHypotheticalDocument(rewrittenQuery, deployment);
            log.info("Hypothetical document created : {}", hypotheticalDocument);

            // 3. Search for relevant documents using hypothetical document
            log.info("Searching embeddings with limit {} for file: {}", searchResultLimit, sourceFilename);
            List<SearchResult> searchResults = embeddingService.searchSimilarText(hypotheticalDocument, searchResultLimit, sourceFilename);

            if (searchResults.isEmpty()) {
                log.warn("No relevant documents found for question: {}, asking directly to llm.", question);
                 return new ChatResponse(chatBotService.getChatBotResponse(question, temperature, deployment), null);
            }

            // 4. Build Context String
            String context = searchResults.stream()
                    .map(sr -> StringUtils.hasText(sr.getSourceFilename())
                            ? String.format("Source: %s\nContent: %s", sr.getSourceFilename(), sr.getText())
                            : sr.getText())
                    .collect(Collectors.joining("\n---\n"));

            // 5. Augment Prompt
            String augmentedPrompt = String.format(ragPromptTemplate, context, question);
            log.debug("Augmented prompt for LLM (first 500 chars): {}", augmentedPrompt.substring(0, Math.min(augmentedPrompt.length(), 500)));

            return new ChatResponse(chatBotService.getChatBotResponse(augmentedPrompt, temperature, deployment), null);


        } catch (ExecutionException | InterruptedException e) {
            log.error("Error during RAG search for question '{}': {}", question, e.getMessage(), e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            return new ChatResponse(null, "Error retrieving relevant documents: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error during RAG processing for question '{}': {}", question, e.getMessage(), e);
            return new ChatResponse(null, "An error occurred while processing your request: " + e.getMessage());
        }
    }

    private String rewriteQuery(String query, String deployment){
        String rewriteQueryTemplate = String.format(enhanceQueryTemplate, query);
        String response = singleUseQuery(rewriteQueryTemplate, deployment, 0.0);
        return response.equals(FAILED_TO_GET_RESPONSE)?query:response;
    }

    private String createHypotheticalDocument(String query, String deployment){
        String hydeTemplate = String.format(ragHydeTemplate, query, chunkSize/4);
        String response = singleUseQuery(hydeTemplate, deployment, 0.0);
        return response.equals(FAILED_TO_GET_RESPONSE)?query:response;
    }

    /**
     * Runs the user query using an LLM always with a new ChatHistory instance.
     *
     * @param query The user query.
     * @param deployment    The LLM deployment to use for querying.
     * @param temperature   The temperature setting for the LLM call.
     * @return The response from the LLM.
     */
    private String singleUseQuery(String query, String deployment, double temperature) {
        ChatHistory newHistory = new ChatHistory(Collections.emptyList());
        newHistory.addUserMessage(query);
        return chatBotService.getChatBotResponse(query, temperature, deployment, newHistory);
    }


    /**
     * Processes an uploaded file (PDF or DOCX), extracts text, and stores its embedding.
     *
     * @param file The uploaded MultipartFile.
     * @return A status string indicating the outcome (e.g., "SUCCESS", "FAILED_...", "SKIPPED_...").
     * @throws IOException If there's an error reading the file stream or during text extraction.
     * @throws ExecutionException | InterruptedException If there's an error during embedding storage (Qdrant interaction).
     * @throws RuntimeException for other unexpected errors during embedding/storage.
     */
    public String processAndStoreDocument(MultipartFile file) throws IOException, ExecutionException, InterruptedException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String textContent = null;

        log.info("Processing uploaded document: {}", filename);

        try (InputStream inputStream = file.getInputStream()) {
            if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
                textContent = dataExtraction.extractTextFromPdf(inputStream, filename);
            } else if (contentType != null && contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                textContent = dataExtraction.extractTextFromDocx(inputStream, filename);
            } else {
                // This should ideally be caught by the controller, but double-check
                log.warn("Unsupported content type '{}' passed to service for file {}", contentType, filename);
                return "FAILED_UNSUPPORTED_TYPE";
            }
        }

        if (textContent == null || textContent.isBlank()) {
            log.warn("No text content extracted from {} or content is blank.", filename);
            return textContent == null ? "SKIPPED_EXTRACTION_FAILED" : "SKIPPED_BLANK_TEXT";
        }

        log.info("Extracted text from {}. Proceeding to embed and store.", filename);
        return embeddingService.chunkAndStoreEmbeddings(textContent, filename);
    }
}
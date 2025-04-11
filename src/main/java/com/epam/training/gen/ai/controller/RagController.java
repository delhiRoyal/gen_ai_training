package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.model.ChatRequest;
import com.epam.training.gen.ai.model.ChatResponse;
import com.epam.training.gen.ai.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/rag")
@Slf4j
public class RagController {

    private final RagService ragService;

    @Value("${DEFAULT_TEMPERATURE}")
    private Double defaultTemperature;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> queryKnowledgeBase(@RequestBody ChatRequest request) {
        if (request.getInput() == null || request.getInput().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse(null, "Input cannot be empty."));
        }
        String deployment = request.getDeployment() == null ? "openAI" : request.getDeployment();
        Double temperature = request.getTemperature()== null ? defaultTemperature : request.getTemperature();
        String sourceFilename = request.getSourceFilename();
        log.info("Received RAG query: '{}', Deployment: {}, Temp: {}, SourceFile: {}",
                request.getInput(), deployment, temperature, sourceFilename == null ? "N/A" : sourceFilename);

        try {
            ChatResponse response = ragService.answerQuestion(
                    request.getInput(),
                    deployment,
                    temperature,
                    sourceFilename
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ChatResponse(null, "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        log.info("Received file upload: Name='{}', Type='{}', Size={}", filename, contentType, file.getSize());

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Upload rejected: Invalid file type '{}' for file '{}'", contentType, filename);
            return ResponseEntity.badRequest().body("Invalid file type. Only PDF and DOCX files are allowed.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Upload rejected: File '{}' exceeds size limit of {} bytes", filename, MAX_FILE_SIZE);
            return ResponseEntity.badRequest().body("File exceeds maximum size limit of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB.");
        }


        try {
            String result = ragService.processAndStoreDocument(file);
            log.info("File processing result for '{}': {}", filename, result);
            return switch (result) {
                case "SUCCESS",
                     "SUCCESS_ALREADY_EXISTS" ->
                        ResponseEntity.ok("File processed and embedded successfully.");
                case "SKIPPED_BLANK_TEXT",
                     "SKIPPED_ENCRYPTED" ->
                        ResponseEntity.ok("File processed, but no content found or file is encrypted/unsupported.");
                case "FAILED_NO_EMBEDDING" ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File processed, but failed to generate embeddings.");
                default ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process file. Result: " + result);
            };
        } catch (IOException e) {
            log.error("IO Error processing uploaded file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading or processing file: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("Runtime Error processing uploaded file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error storing or embedding file content: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing uploaded file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during file processing.");
        }
    }

}
package com.epam.training.gen.ai.controller;

import com.azure.ai.openai.models.EmbeddingItem;
import com.epam.training.gen.ai.model.EmbeddingRequest;
import com.epam.training.gen.ai.model.EmbeddingResponse;
import com.epam.training.gen.ai.model.SearchResult;
import com.epam.training.gen.ai.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/embedding")
@Slf4j
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @Autowired
    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/build")
    public ResponseEntity<EmbeddingResponse> buildEmbedding(@RequestBody EmbeddingRequest request) {
        try {
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new EmbeddingResponse(null, "Input text cannot be empty."));
            }
            List<EmbeddingItem> embedding = embeddingService.buildEmbedding(request.getText());
            if (embedding.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmbeddingResponse(null, "Failed to generate embedding."));
            }
            return ResponseEntity.ok(new EmbeddingResponse(embedding, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmbeddingResponse(null, "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/build-and-store")
    public ResponseEntity<String> buildAndStoreEmbedding(@RequestBody EmbeddingRequest request) {
        try {
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Input text cannot be empty.");
            }
            String status = embeddingService.chunkAndStoreEmbeddings(request.getText());
            if ("FAILED_NO_EMBEDDING".equals(status)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate embedding, nothing stored.");
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body( "An error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchSimilarEmbeddings(@RequestBody EmbeddingRequest request) {
        try {
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Input text cannot be empty.");
            }
            // Use the new method that retrieves text
            List<SearchResult> similarTexts = embeddingService.searchSimilarText(request.getText(), request.getLimit());
            return ResponseEntity.ok(similarTexts); // Return the list directly
        } catch (ExecutionException | InterruptedException e) {
            log.error("Search failed: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during search: " + e.getMessage());
        }
        catch (Exception e) {
            log.error("Search failed unexpectedly: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body( "An unexpected error occurred: " + e.getMessage());
        }
    }



}

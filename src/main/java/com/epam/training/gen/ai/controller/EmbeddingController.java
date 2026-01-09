package com.epam.training.gen.ai.controller;

import com.azure.ai.openai.models.EmbeddingItem;
import com.epam.training.gen.ai.model.EmbeddingRequest;
import com.epam.training.gen.ai.model.EmbeddingResponse;
import com.epam.training.gen.ai.model.SearchResult;
import com.epam.training.gen.ai.service.EmbeddingService;
import io.qdrant.client.grpc.Points.ScoredPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/embedding")
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
            String status = embeddingService.buildAndStoreEmbedding(request.getText());
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
            List<ScoredPoint>  similarEmbeddingList= embeddingService.search(request.getText(), request.getLimit());
            return ResponseEntity.ok(getSearchResultFromScoredPoint(similarEmbeddingList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body( "An error occurred: " + e.getMessage());
        }
    }

    private List<SearchResult> getSearchResultFromScoredPoint( List<ScoredPoint> scoredPoints){
        return scoredPoints.stream().map(scoredPoint -> {
            SearchResult searchResult = new SearchResult();
            searchResult.setScore(scoredPoint.getScore());
            searchResult.setUuid(scoredPoint.getId().getUuid());
            searchResult.setEmbeddingPoints(scoredPoint.getVectors().getVector().getDataList());
            return searchResult;
        }).toList();
    }


}

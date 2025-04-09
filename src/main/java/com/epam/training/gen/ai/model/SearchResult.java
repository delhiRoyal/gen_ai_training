package com.epam.training.gen.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single search result item containing the score and associated text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private float score;
    private String uuid;
    private List<Float> embeddingPoints;

}
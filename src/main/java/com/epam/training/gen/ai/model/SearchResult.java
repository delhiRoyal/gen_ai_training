package com.epam.training.gen.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private float score;
    private String uuid;
    private String text;
    private List<Float> embeddingPoints;
    private String sourceFilename;
}
package com.epam.training.gen.ai.model;

import com.azure.ai.openai.models.EmbeddingItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    private List<EmbeddingItem> embedding;
    private String error;

}
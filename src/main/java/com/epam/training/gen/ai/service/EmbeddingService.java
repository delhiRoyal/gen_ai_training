package com.epam.training.gen.ai.service;


import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.WithVectorsSelectorFactory;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

@Service
@Slf4j
public class EmbeddingService {

    public static final int ADA_002_MODEL_DIMENSION_SIZE = 1536;
    private static final String COLLECTION_NAME = "embedding_collection";

    @Value("${openai.embedding.deployment}")
    private String embeddingDeployment;

    private final OpenAIAsyncClient openAIAsyncClient;
    private final QdrantClient qdrantClient;



    @Autowired
    public EmbeddingService(OpenAIAsyncClient openAIAsyncClient,
                            QdrantClient qdrantClient) {
        this.openAIAsyncClient = openAIAsyncClient;
        this.qdrantClient = qdrantClient;
    }


    public List<EmbeddingItem> buildEmbedding(String text) {
        log.info("Building embedding for text: {}", text);
        try {
            EmbeddingsOptions embeddingsOptions = new EmbeddingsOptions(List.of(text));

            Mono<Embeddings> embeddingsMono = openAIAsyncClient.getEmbeddings(embeddingDeployment,embeddingsOptions);

            Embeddings embeddings = embeddingsMono.block();

            if (embeddings == null || embeddings.getData() == null || embeddings.getData().isEmpty()) {
                throw new RuntimeException("No embeddings returned from OpenAI API.");
            }

            return embeddings.getData();

        } catch (Exception e) {
            log.error("Error while generating embedding: {}", e.getMessage());
            throw new RuntimeException("Error while generating embedding", e);
        }
    }

    public String buildAndStoreEmbedding(String text) {
        log.info("Building and storing embedding for text: {}", text);
        try {
            List<EmbeddingItem> embeddings = buildEmbedding(text);
            return storeEmbedding(embeddings, text);
        } catch (Exception e) {
            log.error("Error while generating and storing embedding: {}", e.getMessage());
            throw new RuntimeException("Error while generating and storing embedding", e);
        }
    }

    public List<ScoredPoint> search(String text, int limit) throws ExecutionException, InterruptedException {
        log.info("Searching for text: {}", text);
        List<EmbeddingItem> embeddings = buildEmbedding(text);
        if (embeddings.isEmpty()) {
            log.warn("Could not generate embedding for search query: {}", text);
            return java.util.Collections.emptyList();
        }
        List<ScoredPoint> similarEmbeddingList = searchEmbedding(embeddings, limit);
        log.info("Found {} similar embeddings", similarEmbeddingList.size());
        return similarEmbeddingList;

    }

    private String storeEmbedding(List<EmbeddingItem> embeddings, String text) throws ExecutionException, InterruptedException {
        createCollectionIfNotExists();
        UpdateResult updateResult;
        try {
            updateResult = qdrantClient.upsertAsync(COLLECTION_NAME, getPointStructList(embeddings)).get();
        } catch (Exception e) {
            log.error("Error while storing embedding: {}", e.getMessage());
            throw new RuntimeException("Error while storing embedding", e);
        }

        return updateResult.getStatus().name();
    }

    private static List<PointStruct> getPointStructList(List<EmbeddingItem> embeddings) {

        return embeddings.stream().map(embeddingItem -> {
            UUID id = UUID.randomUUID();
            return PointStruct.newBuilder()
                    .setId(id(id))
                    .setVectors(vectors(embeddingItem.getEmbedding()))
                    //.putAllPayload(Map.of("text", value(text)))
                    .build();
        }).collect(Collectors.toList());
    }

    private void createCollectionIfNotExists() throws ExecutionException, InterruptedException {
        if (qdrantClient.collectionExistsAsync(COLLECTION_NAME).get()){
            log.info("Collection already exists: {}", COLLECTION_NAME);
            return;
        }
        log.info("Creating collection: {}", COLLECTION_NAME);
        Collections.CollectionOperationResponse result = qdrantClient.createCollectionAsync(COLLECTION_NAME,
                            Collections.VectorParams.newBuilder()
                                    .setDistance(Collections.Distance.Cosine)
                                    .setSize(ADA_002_MODEL_DIMENSION_SIZE)
                                    .build())
                    .get();
        log.info("Collection was created: [{}]", result.getResult());
    }

    private List<ScoredPoint> searchEmbedding(List<EmbeddingItem> embeddings, int limit) throws ExecutionException, InterruptedException {
        if (!qdrantClient.collectionExistsAsync(COLLECTION_NAME).get()){
            log.info("Collection doesn't exists: {}", COLLECTION_NAME);
            return java.util.Collections.emptyList();
        }
        var queryEmbeddings = embeddings.stream().map(EmbeddingItem::getEmbedding).flatMap(List::stream).collect(Collectors.toList());
        return qdrantClient.queryAsync(QueryPoints.newBuilder()
                .setCollectionName(COLLECTION_NAME)
                .setQuery(nearest(queryEmbeddings))
                //.setWithPayload(enable(true))
                .setWithVectors(WithVectorsSelectorFactory.enable(true))
                .setLimit(limit)
                .build()

        ).get();
    }

}


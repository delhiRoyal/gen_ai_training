package com.epam.training.gen.ai.service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.epam.training.gen.ai.model.SearchResult;
import com.epam.training.gen.ai.util.DataExtraction;
import com.epam.training.gen.ai.util.IdGenerator;
import io.qdrant.client.*;
import io.qdrant.client.grpc.Collections.CollectionOperationResponse;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.ConditionFactory.filter;
import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

@Service
@Slf4j
public class EmbeddingService {

    public static final int ADA_002_MODEL_DIMENSION_SIZE = 1536;
    private static final String COLLECTION_NAME = "embedding_collection";
    private static final String PAYLOAD_TEXT_KEY = "text";
    private static final String PAYLOAD_SOURCE_FILENAME_KEY = "source_filename";


    private final OpenAIAsyncClient openAIAsyncClient;
    private final QdrantClient qdrantClient;
    private final IdGenerator idGenerator;
    private final DataExtraction dataExtraction;

    @Value("${embedding.openai.deployment}") // Updated path
    private String embeddingDeployment;

    @Value("${embedding.chunk.size}")
    private int chunkSize;

    @Value("${embedding.chunk.sentence_end_tolerance}")
    private int sentenceEndTolerance;

    @Autowired
    public EmbeddingService(OpenAIAsyncClient openAIAsyncClient, QdrantClient qdrantClient, IdGenerator idGenerator, DataExtraction dataExtraction) {
        this.openAIAsyncClient = openAIAsyncClient;
        this.qdrantClient = qdrantClient;
        this.idGenerator = idGenerator;
        this.dataExtraction = dataExtraction;
    }

    public String chunkAndStoreEmbeddings(String fullText) throws ExecutionException, InterruptedException {
        return chunkAndStoreEmbeddings(fullText, null);
    }

    /**
     * Chunks the given text, generates embeddings for each chunk, and stores them
     * along with the source filename.
     *
     * @param fullText       The complete text content extracted from a document.
     * @param sourceFilename The original filename of the document.
     * @return A status string indicating the overall outcome (e.g., "SUCCESS", "FAILED_PARTIAL", "SKIPPED_BLANK_TEXT").
     * @throws ExecutionException   If there's an error during Qdrant interaction.
     * @throws InterruptedException If the thread is interrupted during async operations.
     */
    public String chunkAndStoreEmbeddings(String fullText, String sourceFilename) throws ExecutionException, InterruptedException {
        if (fullText == null || fullText.isBlank()) {
            log.warn("Skipping embedding storage for null or blank text.");
            return "SKIPPED_BLANK_TEXT";
        }

        if (!StringUtils.hasText(sourceFilename)) {
            log.warn("Source filename is missing, proceeding without storing filename metadata.");
        }

        log.info("Starting chunking and embedding process for text starting with: '{}...'", fullText.substring(0, Math.min(fullText.length(), 100)));

        List<String> chunks = dataExtraction.chunkTextSimple(fullText, chunkSize, sentenceEndTolerance);
        log.info("Text divided into {} chunks.", chunks.size());

        int successfulEmbeddings = 0;
        int failedEmbeddings = 0;
        int skippedEmbeddings = 0;

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkId = idGenerator.generateConsistentId(chunk);

            try {
                if (embeddingExists(chunkId)) {
                    log.debug("Embedding for chunk {} (ID: {}) already exists, skipping.", i + 1, chunkId);
                    skippedEmbeddings++;
                    continue;
                }

                log.debug("Processing chunk {}/{} (ID: {}): '{}...'", i + 1, chunks.size(), chunkId, chunk.substring(0, Math.min(chunk.length(), 50)));
                List<EmbeddingItem> embeddings = buildEmbedding(chunk);

                if (embeddings.isEmpty()) {
                    log.warn("No embedding generated for chunk {} (ID: {}). Skipping storage.", i + 1, chunkId);
                    failedEmbeddings++;
                    continue;
                }

                EmbeddingItem embeddingItem = embeddings.get(0);
                String storeResult = storeEmbedding(embeddingItem, chunk, chunkId,sourceFilename);
                log.info("Stored embedding for chunk {} (ID: {}) with result: {}", i + 1, chunkId, storeResult);

                if (storeResult.startsWith("Completed") || storeResult.startsWith("Updated")) {
                    successfulEmbeddings++;
                } else {
                    log.warn("Failed to store embedding for chunk {} (ID: {}). Result: {}", i + 1, chunkId, storeResult);
                    failedEmbeddings++;
                }

            } catch (ExecutionException | InterruptedException e) {
                log.error("Error processing chunk {} (ID: {}): {}", i + 1, chunkId, e.getMessage(), e);
                failedEmbeddings++;
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            } catch (Exception e) {
                log.error("Unexpected error processing chunk {} (ID: {}): {}", i + 1, chunkId, e.getMessage(), e);
                failedEmbeddings++;
            }
        }

        log.info("Embedding process completed. Success: {}, Failed: {}, Skipped (Already Exists): {}",
                successfulEmbeddings, failedEmbeddings, skippedEmbeddings);

        if (failedEmbeddings > 0 && successfulEmbeddings == 0) {
            return "FAILED_ALL_CHUNKS";
        } else if (failedEmbeddings > 0) {
            return "SUCCESS_PARTIAL";
        } else if (successfulEmbeddings > 0 || skippedEmbeddings > 0) {
            return "SUCCESS";
        } else {
            return "NO_CHUNKS_PROCESSED";
        }
    }

    public List<EmbeddingItem> buildEmbedding(String textChunk) {
        String textChunksubstring = textChunk.substring(0, Math.min(textChunk.length(), 50));
        log.debug("Building embedding for text chunk starting with: '{}...'", textChunksubstring);
        try {
            EmbeddingsOptions embeddingsOptions = new EmbeddingsOptions(List.of(textChunk));
            Mono<Embeddings> embeddingsMono = openAIAsyncClient.getEmbeddings(embeddingDeployment, embeddingsOptions);
            Embeddings embeddings = embeddingsMono.block();

            if (embeddings == null || embeddings.getData() == null || embeddings.getData().isEmpty()) {
                log.warn("No embeddings returned from OpenAI API for text chunk starting with: '{}...'", textChunksubstring);
                return Collections.emptyList();
            }

            return embeddings.getData();

        } catch (Exception e) {
            log.error("Error while generating embedding for text chunk starting with '{}...': {}", textChunksubstring, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<SearchResult> searchSimilarText(String queryText, int limit) throws ExecutionException, InterruptedException {
        return searchSimilarText(queryText, limit, null);
    }

    public List<SearchResult> searchSimilarText(String queryText, int limit, String sourceFilename) throws ExecutionException, InterruptedException {
        log.info("Searching for {} similar text chunks for query (file filter: {})", limit, sourceFilename == null ? "None" : sourceFilename);
        List<EmbeddingItem> queryEmbeddings = buildEmbedding(queryText);
        if (queryEmbeddings.isEmpty()) {
            log.warn("Could not generate embedding for search query: {}", queryText);
            return Collections.emptyList();
        }

        List<ScoredPoint> scoredPoints = searchEmbeddingWithPayload(queryEmbeddings, limit, sourceFilename);
        log.info("Found {} potentially relevant text chunks", scoredPoints.size());

        return scoredPoints.stream()
                .map(this::mapScoredPointToSearchResultWithText)
                .filter(searchResult -> searchResult.getText() != null && !searchResult.getText().isEmpty())
                .collect(Collectors.toList());
    }

    private SearchResult mapScoredPointToSearchResultWithText(ScoredPoint scoredPoint) {
        SearchResult result = new SearchResult();
        result.setScore(scoredPoint.getScore());
        String pointIdStr = "N/A";
        if (scoredPoint.getId().hasUuid()) {
            pointIdStr = scoredPoint.getId().getUuid();
            result.setUuid(pointIdStr);
        } else if (scoredPoint.getId().hasNum()) {
            pointIdStr = String.valueOf(scoredPoint.getId().getNum());
            result.setUuid(pointIdStr);
        }

        JsonWithInt.Value textValue = scoredPoint.getPayloadMap().get(PAYLOAD_TEXT_KEY);
        if (textValue != null && textValue.hasStringValue()) {
            result.setText(textValue.getStringValue());
        } else {
            log.warn("Found point {} without expected text payload key '{}'", pointIdStr, PAYLOAD_TEXT_KEY);
            result.setText("");
        }

        JsonWithInt.Value filenameValue = scoredPoint.getPayloadMap().get(PAYLOAD_SOURCE_FILENAME_KEY);
        if (filenameValue != null && filenameValue.hasStringValue()) {
            result.setSourceFilename(filenameValue.getStringValue());
        } else {
            log.trace("Point {} does not have source filename payload key '{}'", pointIdStr, PAYLOAD_SOURCE_FILENAME_KEY);
            result.setSourceFilename(null);
        }

        return result;
    }

    /**
     * Stores a single embedding chunk.
     *
     * @param embeddingItem The embedding data.
     * @param textChunk     The text content of the chunk.
     * @param chunkId       The pre-generated consistent ID for this chunk.
     * @return The status name from Qdrant update result.
     * @throws ExecutionException   If Qdrant interaction fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    private String storeEmbedding(EmbeddingItem embeddingItem, String textChunk, String chunkId, String sourceFilename) throws ExecutionException, InterruptedException {
        createCollectionIfNotExists();
        UpdateResult updateResult;
        try {
            PointStruct point = createPointStruct(embeddingItem, textChunk, chunkId, sourceFilename);
            updateResult = qdrantClient.upsertAsync(COLLECTION_NAME, List.of(point)).get();
            log.debug("Stored embedding for chunk ID: {}", chunkId);
        } catch (Exception e) {
            log.error("Error while storing embedding for chunk ID {}: {}", chunkId, e.getMessage(), e);
            throw new RuntimeException("Error while storing embedding for chunk ID " + chunkId, e);
        }
        return updateResult.getStatus().name();
    }


    private void createCollectionIfNotExists() throws ExecutionException, InterruptedException {
        boolean exists = qdrantClient.collectionExistsAsync(COLLECTION_NAME).get();
        if (exists) {
            return;
        }
        log.info("Creating collection: {}", COLLECTION_NAME);
        try {
            CollectionOperationResponse result = qdrantClient.createCollectionAsync(COLLECTION_NAME, VectorParams.newBuilder().setDistance(Distance.Cosine)
                    .setSize(ADA_002_MODEL_DIMENSION_SIZE).build()).get();
            log.info("Collection creation result: [{}]", result.getResult());
            if (!result.getResult()) {
                log.error("Failed to create collection {}", COLLECTION_NAME);
                throw new RuntimeException("Failed to create Qdrant collection: " + COLLECTION_NAME);
            }
        } catch (ExecutionException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.warn("Collection {} already exists (detected during creation attempt).", COLLECTION_NAME);
            } else {
                log.error("Failed to create collection {} due to execution error: {}", COLLECTION_NAME, e.getMessage(), e);
                throw e;
            }
        }
    }

    private List<ScoredPoint> searchEmbeddingWithPayload(List<EmbeddingItem> embeddings, int limit, String sourceFilename) throws ExecutionException, InterruptedException {
        if (!qdrantClient.collectionExistsAsync(COLLECTION_NAME).get()) {
            log.warn("Collection doesn't exist during search: {}", COLLECTION_NAME);
            return Collections.emptyList();
        }
        var queryEmbeddings = embeddings.stream().map(EmbeddingItem::getEmbedding).flatMap(List::stream).collect(Collectors.toList());

        QueryPoints.Builder queryBuilder = QueryPoints.newBuilder()
                .setCollectionName(COLLECTION_NAME)
                .setQuery(nearest(queryEmbeddings))
                .setWithPayload(enable(true))
                .setWithVectors(WithVectorsSelectorFactory.enable(false))
                .setLimit(limit);

        if (StringUtils.hasText(sourceFilename)) {
            log.debug("Applying filter for source_filename: {}", sourceFilename);
            queryBuilder.setFilter(
                    Points.Filter.newBuilder()
                            .addMust(matchKeyword(PAYLOAD_SOURCE_FILENAME_KEY, sourceFilename))
                            .build()
            );
        } else {
            log.debug("No source_filename filter applied.");
        }


        return qdrantClient.queryAsync(queryBuilder.build()).get();
    }

    /**
     * Checks if an embedding with the given ID already exists in Qdrant.
     *
     * @param chunkId The consistent ID of the chunk.
     * @return true if the embedding exists, false otherwise.
     * @throws ExecutionException   If Qdrant interaction fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    private boolean embeddingExists(String chunkId) throws ExecutionException, InterruptedException {
        try {
            return !qdrantClient.retrieveAsync(COLLECTION_NAME, List.of(Points.PointId.newBuilder().setUuid(chunkId).build()), false, false, null)
                    .get()
                    .isEmpty();
        } catch (ExecutionException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Not found: Collection") || e.getMessage().contains("doesn't exist"))) {
                log.warn("Collection {} not found during existence check for ID {}. Assuming embedding does not exist.", COLLECTION_NAME, chunkId);
                return false;
            }

            log.error("Error checking embedding existence for ID {}: {}", chunkId, e.getMessage(), e);
            throw e;
        }
    }


    /**
     * Creates a Qdrant PointStruct for a text chunk.
     *
     * @param embeddingItem The embedding data.
     * @param textChunk     The text content of the chunk.
     * @param chunkId       The pre-generated consistent ID for this chunk.
     * @return The PointStruct object.
     */
    private PointStruct createPointStruct(EmbeddingItem embeddingItem, String textChunk, String chunkId, String sourceFilename) {
        Points.PointStruct.Builder builder = PointStruct.newBuilder()
                .setId(Points.PointId.newBuilder().setUuid(chunkId).build())
                .setVectors(vectors(embeddingItem.getEmbedding()))
                .putPayload(PAYLOAD_TEXT_KEY, value(textChunk));
        if (StringUtils.hasText(sourceFilename)) {
            builder.putPayload(PAYLOAD_SOURCE_FILENAME_KEY, value(sourceFilename));
        } else {
            log.trace("Skipping addition of null/empty source filename to payload for chunk ID: {}", chunkId);
        }

                return builder.build();
    }
}
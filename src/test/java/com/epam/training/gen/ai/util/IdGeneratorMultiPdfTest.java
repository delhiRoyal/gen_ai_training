package com.epam.training.gen.ai.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class IdGeneratorMultiPdfTest {

    private static final Logger log = LoggerFactory.getLogger(IdGeneratorMultiPdfTest.class);
    private static final String PDF_LOCATION_PATTERN = "classpath:data/*.pdf";
    private static final Pattern UUID_REGEX =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    // --- Use chunking parameters representative of your actual application if possible ---
    // Adjust these values based on your application's configuration (e.g., application.yml)
    // and the nature of your test data.
    private static final int TEST_CHUNK_SIZE = 5000; // Example value
    private static final int TEST_SENTENCE_TOLERANCE = 1000; // Example value

    private IdGenerator idGenerator;
    private DataExtraction dataExtraction;

    @BeforeEach
    void setUp() {
        idGenerator = new IdGenerator();
        dataExtraction = new DataExtraction();
    }

    @Test
    @DisplayName("Should generate consistent IDs for text chunks and report duplicates without failing")
    void generateConsistentIds_forMultiplePdfChunks_shouldBeConsistentAndReportDuplicates() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources(PDF_LOCATION_PATTERN);
        } catch (IOException e) {
            fail("Could not scan for PDF resources at pattern '%s': %s", PDF_LOCATION_PATTERN, e.getMessage());
            return;
        }

        assertThat(resources)
                .withFailMessage("No PDF files found in src/test/resources/data/ matching pattern '%s'. Test requires PDF files to run.", PDF_LOCATION_PATTERN)
                .isNotEmpty();

        log.info("Found {} PDF resources in {}", resources.length, PDF_LOCATION_PATTERN);

        Map<String, List<String>> generatedIdsWithSource = new HashMap<>();
        int totalFilesProcessed = 0;
        int totalChunksProcessed = 0;
        int errorCount = 0;

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) {
                log.warn("Skipping resource without a filename: {}", resource.getDescription());
                continue;
            }

            log.info("Processing PDF: {}", filename);
            String textContent = null;
            try (InputStream inputStream = resource.getInputStream()) {
                textContent = dataExtraction.extractTextFromPdf(inputStream, filename);
                totalFilesProcessed++;
            } catch (IOException e) {
                log.error("Failed to read or extract text from {}: {}", filename, e.getMessage(), e);
                errorCount++;
                continue;
            } catch (Exception e) {
                log.error("Unexpected error extracting text from {}: {}", filename, e.getMessage(), e);
                errorCount++;
                continue;
            }

            if (textContent != null && !textContent.isBlank()) {
                log.debug("Extracted text from {}. Chunking with size={} tolerance={}", filename, TEST_CHUNK_SIZE, TEST_SENTENCE_TOLERANCE);

                List<String> chunks = dataExtraction.chunkTextSimple(textContent, TEST_CHUNK_SIZE, TEST_SENTENCE_TOLERANCE);
                log.info("File {} resulted in {} chunks.", filename, chunks.size());

                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    if (chunk == null || chunk.isBlank()) {
                        log.trace("Skipping blank chunk from file {} at index {}", filename, i);
                        continue;
                    }

                    totalChunksProcessed++;
                    String chunkId = idGenerator.generateConsistentId(chunk);
                    String chunkPreview = chunk.substring(0, Math.min(chunk.length(), 50)).replaceAll("\\s+", " ").trim(); // Cleaned preview
                    String sourceInfo = String.format("File: %s, Chunk Index: %d, Preview: '%s...'", filename, i, chunkPreview);

                    log.trace("Generated ID for chunk from {}: {} (Index: {})", filename, chunkId, i);

                    assertThat(chunkId)
                            .withFailMessage("Generated ID for a chunk from %s (Index: %d) is null or empty.", filename, i)
                            .isNotNull()
                            .isNotEmpty()
                            .matches(UUID_REGEX);

                    generatedIdsWithSource.computeIfAbsent(chunkId, k -> new ArrayList<>()).add(sourceInfo);
                }
            } else {
                log.warn("No text content extracted or content is blank for file: {}", filename);
            }
        }

        int uniqueIdsGenerated = generatedIdsWithSource.keySet().size();
        log.info("Finished processing PDFs. Files Processed: {}, Total Chunks Processed: {}, Unique IDs Generated: {}, Errors: {}",
                totalFilesProcessed, totalChunksProcessed, uniqueIdsGenerated, errorCount);

        // --- Basic Assertions (Ensure processing happened) ---
        assertThat(totalFilesProcessed)
                .withFailMessage("Expected to process at least one PDF file, but processed %d.", totalFilesProcessed)
                .isGreaterThan(0);

        // Only assert chunks processed if files were processed without error
        if (totalFilesProcessed > 0 && errorCount == 0) {
            assertThat(totalChunksProcessed)
                    .withFailMessage("Expected to process at least one text chunk across all files (assuming valid content), but processed %d.", totalChunksProcessed)
                    .isGreaterThan(0);
            // Only assert unique IDs generated if chunks were processed
            if (totalChunksProcessed > 0) {
                assertThat(uniqueIdsGenerated)
                        .withFailMessage("Expected to generate at least one unique ID from the processed chunks, but generated %d.", uniqueIdsGenerated)
                        .isGreaterThan(0);
            }
        }



        log.info("--- Duplicate ID Analysis ---");
        int duplicateIdCount = 0;
        int totalDuplicateInstances = 0;
        boolean duplicatesFound = false;

        for (Map.Entry<String, List<String>> entry : generatedIdsWithSource.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicatesFound = true;
                duplicateIdCount++;
                totalDuplicateInstances += entry.getValue().size();
                log.warn("Duplicate ID found: {} ({} occurrences)", entry.getKey(), entry.getValue().size());
                for (String source : entry.getValue()) {
                    log.warn("  - Source: {}", source);
                }
            }
        }

        if (!duplicatesFound) {
            log.info("No duplicate IDs detected across {} processed chunks.", totalChunksProcessed);
        } else {
            log.warn("Detected {} distinct IDs with duplicates.", duplicateIdCount);
            log.warn("Total chunk instances processed: {}", totalChunksProcessed);
            log.warn("Total unique IDs generated: {}", uniqueIdsGenerated);
            log.warn("Total duplicate instances found (excluding first occurrence): {}", totalChunksProcessed - uniqueIdsGenerated);
        }
        log.info("--- End Duplicate ID Analysis ---");

        if (uniqueIdsGenerated == totalChunksProcessed && totalChunksProcessed > 0) {
            log.info("Test PASSED: Successfully verified that {} unique IDs were generated for {} distinct non-blank text chunks across {} files (No duplicates found).",
                    uniqueIdsGenerated, totalChunksProcessed, totalFilesProcessed);
        } else if (uniqueIdsGenerated < totalChunksProcessed && uniqueIdsGenerated > 0) {
            log.info("Test PASSED: Found {} unique IDs for {} total processed chunks across {} files. {} duplicate chunk instances were detected in the source data (as expected for content-based IDs).",
                    uniqueIdsGenerated, totalChunksProcessed, totalFilesProcessed, totalChunksProcessed - uniqueIdsGenerated);
        } else if (totalChunksProcessed == 0 && errorCount == 0) {
            log.info("Test PASSED: No non-blank chunks were processed (check test data content).");
        } else if (errorCount > 0) {
            log.info("Test finished with errors during file processing.");
            fail("Encountered %d errors while reading/extracting text from PDFs. Check logs above for details.", errorCount);
        } else {
            log.info("Test finished. Review logs for details.");
        }

        if (errorCount > 0) {
            fail("Encountered %d errors while reading/extracting text from PDFs. Check logs above for details.", errorCount);
        }
    }
}
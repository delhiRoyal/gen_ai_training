package com.epam.training.gen.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class DataExtraction {


    /**
     * Extracts text content from a PDF file provided as an InputStream.
     *
     * @param inputStream The InputStream of the PDF file.
     * @param filename    The name of the file (for logging purposes).
     * @return The extracted text content, or null if extraction fails.
     * @throws IOException if reading the stream or loading the PDF fails.
     */
    public String extractTextFromPdf(InputStream inputStream, String filename) throws IOException {
        log.debug("Attempting to extract text from PDF: {}", filename);
        byte[] pdfBytes = null;
        try {
            pdfBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Failed to read PDF input stream into byte array for file {}: {}", filename, e.getMessage(), e);
            throw e;
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                log.warn("Skipping encrypted PDF file: {}", filename);
                return null;
            }
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            log.info("Successfully extracted text from PDF: {}", filename);
            return text;
        } catch (IOException e) {
            log.error("Failed to load or extract text from PDF file {}: {}", filename, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error extracting text from PDF file {}: {}", filename, e.getMessage(), e);
            throw new IOException("Unexpected error during PDF text extraction: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from a DOCX file provided as an InputStream.
     *
     * @param inputStream The InputStream of the DOCX file.
     * @param filename    The name of the file (for logging purposes).
     * @return The extracted text content, or null if extraction fails.
     * @throws IOException if reading the stream or processing the DOCX fails.
     */
    public String extractTextFromDocx(InputStream inputStream, String filename) throws IOException {
        log.debug("Attempting to extract text from DOCX: {}", filename);
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.info("Successfully extracted text from DOCX: {}", filename);
            return text;
        } catch (IOException e) {
            log.error("Failed to extract text from DOCX file {}: {}", filename, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error extracting text from DOCX file {}: {}", filename, e.getMessage(), e);
            throw new IOException("Unexpected error during DOCX text extraction: " + e.getMessage(), e); // Wrap other exceptions
        }
    }

    /**
     * Chunks text primarily by sentence boundaries. If a sentence exceeds the
     * chunkSize, it checks if the sentence end falls within a tolerance window
     * (`chunkSize + sentenceEndTolerance`). If it does, the chunk includes the
     * entire sentence. Otherwise, it attempts to split at whitespace near the
     * chunkSize limit or performs a hard cut at chunkSize.
     *
     * @param text                 The text to chunk.
     * @param chunkSize            The target maximum size of each chunk (can be exceeded by sentenceEndTolerance).
     * @param sentenceEndTolerance tolerance window for sentence end, defaults to zero
     * @return A list of text chunks.
     */
    public List<String> chunkTextSimple(String text, int chunkSize, int sentenceEndTolerance) {
        if (text == null || text.isBlank() || chunkSize <= 0) {
            log.warn("chunkTextSimple called with invalid input. Text is null/blank or chunkSize <= 0.");
            return Collections.emptyList();
        }

        int effectiveTolerance = Math.max(0, sentenceEndTolerance);

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int start = 0;
        String sentenceTerminators = ".!?";
        // Define a lookbehind window for finding fallback whitespace (e.g., 20% of chunk size, min 10)
        int whitespaceLookbehind = Math.max(10, chunkSize / 5);

        log.info("Starting sentence-based chunking: textLength={}, chunkSize={}, tolerance={}",
                textLength, chunkSize, effectiveTolerance);

        while (start < textLength) {
            int currentChunkStart = start;
            int sentenceEnd = -1;
            int searchStart = start;
            int earliestTerminatorPos = textLength;

            for (char terminator : sentenceTerminators.toCharArray()) {
                int pos = text.indexOf(terminator, searchStart);
                if (pos != -1) {
                    boolean isPotentialEndContext = (pos + 1 >= textLength ||
                            Character.isWhitespace(text.charAt(pos + 1)) ||
                            text.charAt(pos + 1) == '\n' ||
                            text.charAt(pos + 1) == '\r' ||
                            sentenceTerminators.indexOf(text.charAt(pos + 1)) != -1);

                    if (isPotentialEndContext) {
                        boolean looksLikeMidAcronym = (terminator == '.' &&
                                pos > searchStart &&
                                Character.isLetter(text.charAt(pos - 1)) &&
                                pos + 1 < textLength &&
                                Character.isLetter(text.charAt(pos + 1)));

                        if (!looksLikeMidAcronym && pos < earliestTerminatorPos) {
                            earliestTerminatorPos = pos;
                        }
                    }
                }
            }

            if (earliestTerminatorPos < textLength) {
                int finalTerminatorPos = earliestTerminatorPos;
                while (finalTerminatorPos + 1 < textLength &&
                        sentenceTerminators.indexOf(text.charAt(finalTerminatorPos + 1)) != -1) {
                    finalTerminatorPos++;
                }
                sentenceEnd = finalTerminatorPos + 1;
            } else {
                sentenceEnd = textLength;
            }


            int potentialChunkLength = sentenceEnd - currentChunkStart;
            int actualEnd;

            if (potentialChunkLength <= chunkSize) {
                actualEnd = sentenceEnd;
                log.trace("Sentence fits within base chunk size: start={}, end={}, length={}", currentChunkStart, actualEnd, potentialChunkLength);
            } else if (potentialChunkLength <= chunkSize + effectiveTolerance) {
                actualEnd = sentenceEnd;
                log.trace("Sentence exceeds base chunk size but fits within tolerance: start={}, end={}, length={} (chunkSize={}, tolerance={})",
                        currentChunkStart, actualEnd, potentialChunkLength, chunkSize, effectiveTolerance);
            } else {
                log.trace("Sentence too long even with tolerance (length {} > {} + {}). Splitting required near chunk size.",
                        potentialChunkLength, chunkSize, effectiveTolerance);
                int idealSplitPoint = currentChunkStart + chunkSize;
                actualEnd = idealSplitPoint;

                boolean whitespaceFound = false;
                for (int i = idealSplitPoint - 1; i >= Math.max(currentChunkStart, idealSplitPoint - whitespaceLookbehind); i--) {
                    if (Character.isWhitespace(text.charAt(i))) {
                        actualEnd = i + 1;
                        whitespaceFound = true;
                        log.trace("Found fallback whitespace split point at index {}", i);
                        break;
                    }
                }
                if (!whitespaceFound) {
                    log.trace("No suitable whitespace found near index {}, using hard cut at {}.", idealSplitPoint, actualEnd);
                }
            }

            actualEnd = Math.min(actualEnd, textLength);

            if (actualEnd <= currentChunkStart) {
                log.warn("Chunk end calculation resulted in no progress (start={}, end={}). Attempting recovery or breaking.", currentChunkStart, actualEnd);
                actualEnd = currentChunkStart + 1;
                log.warn("Forcing minimal progress to avoid infinite loop. New end={}", actualEnd);
            }

            String chunk = text.substring(currentChunkStart, actualEnd).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
                log.debug("Added chunk: start={}, end={}, length={}, trimmedLength={}", currentChunkStart, actualEnd, actualEnd - currentChunkStart, chunk.length());
            } else {
                log.debug("Skipped adding blank chunk: start={}, end={}", currentChunkStart, actualEnd);
            }

            start = actualEnd;
            log.trace("Next chunk planned to start at: {}", start);
        }

        log.info("Chunking finished. Total chunks created: {}", chunks.size());
        return chunks;
    }
}

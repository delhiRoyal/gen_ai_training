package com.epam.training.gen.ai.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataExtractionTest {

    private DataExtraction dataExtraction;

    @BeforeEach
    void setUp() {
        dataExtraction = new DataExtraction();
    }

    @Nested
    @DisplayName("Input Validation and Edge Cases")
    class InputValidationTests {

        @Test
        @DisplayName("Should return empty list when text is null")
        void chunkTextSimple_whenTextIsNull_shouldReturnEmptyList() {
            List<String> chunks = dataExtraction.chunkTextSimple(null, 500, 50);
            assertThat(chunks).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when text is empty")
        void chunkTextSimple_whenTextIsEmpty_shouldReturnEmptyList() {
            List<String> chunks = dataExtraction.chunkTextSimple("", 500, 50);
            assertThat(chunks).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when text is blank")
        void chunkTextSimple_whenTextIsBlank_shouldReturnEmptyList() {
            List<String> chunks = dataExtraction.chunkTextSimple("   \n \t ", 500, 50);
            assertThat(chunks).isNotNull().isEmpty();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("Should return empty list when chunkSize is non-positive")
        void chunkTextSimple_whenChunkSizeIsNonPositive_shouldReturnEmptyList(int invalidChunkSize) {
            String text = "This is valid text.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, invalidChunkSize, 50);
            assertThat(chunks).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should treat negative tolerance as zero")
        void chunkTextSimple_whenToleranceIsNegative_shouldActAsZeroTolerance() {
            // Sentence length = 37. chunkSize = 30. Negative tolerance = -10 (acts as 0).
            // Expected: Split because 37 > 30 + 0.
            String text = "This sentence is exactly 37 chars long.";
            int chunkSize = 30;
            int negativeTolerance = -10;
            List<String> chunks = dataExtraction.chunkTextSimple(text, chunkSize, negativeTolerance);
            // Expecting a split near char 30 (likely whitespace before '37')
            assertThat(chunks).hasSize(2);
            assertThat(chunks.get(0)).isEqualTo("This sentence is exactly 37"); // Split at whitespace
            assertThat(chunks.get(1)).isEqualTo("chars long.");
        }
    }

    @Nested
    @DisplayName("Basic Chunking by Sentence")
    class BasicChunkingTests {

        @Test
        @DisplayName("Should return single chunk when text is shorter than chunkSize")
        void chunkTextSimple_whenTextShorterThanChunkSize_shouldReturnSingleChunk() {
            String text = "This is a short sentence.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 100, 10);
            assertThat(chunks).containsExactly("This is a short sentence.");
        }

        @Test
        @DisplayName("Should split text into sentences when all fit within chunkSize")
        void chunkTextSimple_whenSentencesFitChunkSize_shouldSplitBySentence() {
            String text = "First sentence. Second sentence! Third sentence?";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 50, 10);
            assertThat(chunks).containsExactly(
                    "First sentence.",
                    "Second sentence!",
                    "Third sentence?"
            );
        }

        @Test
        @DisplayName("Should handle different sentence terminators and whitespace")
        void chunkTextSimple_withVariousTerminatorsAndWhitespace_shouldSplitCorrectly() {
            String text = "Sentence one.\nSentence two!  Sentence three?\tSentence four.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 50, 10);
            assertThat(chunks).containsExactly(
                    "Sentence one.",
                    "Sentence two!",
                    "Sentence three?",
                    "Sentence four."
            );
        }

        @Test
        @DisplayName("Should handle text ending exactly at sentence boundary")
        void chunkTextSimple_whenTextEndsAtSentenceBoundary_shouldIncludeLastSentence() {
            String text = "Sentence one. Sentence two.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 50, 10);
            assertThat(chunks).containsExactly("Sentence one.", "Sentence two.");
        }

        @Test
        @DisplayName("Should handle text ending mid-sentence")
        void chunkTextSimple_whenTextEndsMidSentence_shouldIncludePartialSentence() {
            String text = "Sentence one. This is the start";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 50, 10);
            assertThat(chunks).containsExactly("Sentence one.", "This is the start");
        }

        @Test
        @DisplayName("Should trim whitespace from chunks")
        void chunkTextSimple_shouldTrimWhitespaceFromChunks() {
            String text = "  Sentence one.   Sentence two! \n ";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 50, 10);
            assertThat(chunks).containsExactly("Sentence one.", "Sentence two!");
        }

        @Test
        @DisplayName("Should not split mid-word like U.S.A. if sentence fits")
        void chunkTextSimple_shouldNotSplitMidAcronymIfSentenceFits() {
            String text = "Visit the U.S.A. It's a country.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 50, 10);
            assertThat(chunks).containsExactly("Visit the U.S.A. It's a country.");
        }

         @Test
         @DisplayName("Should handle multiple terminators together")
         void chunkTextSimple_withMultipleTerminators_shouldSplitCorrectly() {
             String text = "Is it good?! Yes. No...? Maybe.";
             List<String> chunks = dataExtraction.chunkTextSimple(text, 50, 10);
             assertThat(chunks).containsExactly(
                     "Is it good?!", // Splits after '!'
                     "Yes.",
                     "No...?", // Splits after '?'
                     "Maybe."
             );
         }
    }

    @Nested
    @DisplayName("Tolerance Handling")
    class ToleranceTests {

        @Test
        @DisplayName("Should include full sentence if length is within chunkSize + tolerance")
        void chunkTextSimple_whenSentenceLengthWithinTolerance_shouldKeepSentenceIntact() {
            // Sentence length = 37. chunkSize = 30. tolerance = 10. (37 <= 30 + 10)
            String text = "This sentence is exactly 37 chars long.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 30, 10);
            assertThat(chunks).containsExactly("This sentence is exactly 37 chars long.");
        }

        @Test
        @DisplayName("Should include full sentence if length equals chunkSize + tolerance")
        void chunkTextSimple_whenSentenceLengthEqualsToleranceLimit_shouldKeepSentenceIntact() {
            String text = "This sentece is xactly forty chars long.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 30, 10);
            assertThat(chunks).containsExactly("This sentece is xactly forty chars long.");
        }

        @Test
        @DisplayName("Should split sentence if length exceeds chunkSize + tolerance")
        void chunkTextSimple_whenSentenceLengthExceedsTolerance_shouldSplitSentence() {
            String text = "This very long sentence exceeds the limit+1.";
            List<String> chunks = dataExtraction.chunkTextSimple(text, 30, 10);
            assertThat(chunks).hasSize(2);
            assertThat(chunks.get(0)).isEqualTo("This very long sentence");
            assertThat(chunks.get(1)).isEqualTo("exceeds the limit+1.");
        }
    }

    @Nested
    @DisplayName("Long Sentence Splitting")
    class LongSentenceSplitTests {

        @Test
        @DisplayName("Should split long sentence at whitespace before chunkSize limit")
        void chunkTextSimple_whenLongSentence_shouldSplitAtWhitespace() {
            // No sentence terminators within the first ~60 chars
            String text = "This is a very very long sentence that definitely exceeds the chunk size and needs splitting at appropriate whitespace.";
            int chunkSize = 40;
            int tolerance = 5; // 40+5 = 45 max length for tolerance
            List<String> chunks = dataExtraction.chunkTextSimple(text, chunkSize, tolerance);

            assertThat(chunks).hasSize(3);
            // Split should occur before 'definitely' (index ~34)
            assertThat(chunks.get(0)).isEqualTo("This is a very very long sentence that");
            // Next split before 'appropriate' (index ~81 relative to start, ~47 relative to previous chunk start)
            assertThat(chunks.get(1)).isEqualTo("definitely exceeds the chunk size and");
            assertThat(chunks.get(2)).isEqualTo("needs splitting at appropriate whitespace.");
        }

        @Test
        @DisplayName("Should perform hard cut if no suitable whitespace found before chunkSize limit")
        void chunkTextSimple_whenLongSentenceAndNoWhitespace_shouldPerformHardCut() {
            // Long word forces hard cut
            String text = "Pneumonoultramicroscopicsilicovolcanoconiosis is a long word. Another sentence.";
            int chunkSize = 30;
            int tolerance = 5; // 30+5 = 35 max length
            List<String> chunks = dataExtraction.chunkTextSimple(text, chunkSize, tolerance);

            assertThat(chunks).hasSize(3);
            // Hard cut at index 30
            assertThat(chunks.get(0)).isEqualTo("Pneumonoultramicroscopicsilico");
            // Remainder of the word + ' is a long word.'
            assertThat(chunks.get(1)).isEqualTo("volcanoconiosis is a long word.");
            assertThat(chunks.get(2)).isEqualTo("Another sentence.");
        }

        @Test
        @DisplayName("Should handle multiple consecutive long sentences needing splits")
        void chunkTextSimple_withMultipleLongSentences_shouldSplitEachCorrectly() {
            String longSentence1 = "ThisIsTheFirstVeryLongSentenceWithoutSpacesThatNeedsToBeHardCutMultipleTimes"; // 71 chars
            String longSentence2 = "AndThisIsTheSecondVeryLongSentenceWhichAlsoNeedsToBeHardCutMaybeMoreThanOnce"; // 77 chars
            String text = longSentence1 + ". " + longSentence2 + ".";
            int chunkSize = 25;
            int tolerance = 5; // 25+5 = 30 max length

            List<String> chunks = dataExtraction.chunkTextSimple(text, chunkSize, tolerance);

            assertThat(chunks).containsExactly(
                    "ThisIsTheFirstVeryLongSen", // 0-25
                    "tenceWithoutSpacesThatNee", // 25-50
                    "dsToBeHardCutMultipleTimes.", // 71 + '.'
                    "AndThisIsTheSecondVeryLo", // 0-25 of second sentence
                    "ngSentenceWhichAlsoNeedsT", // 25-50
                    "oBeHardCutMaybeMoreThanOnce."
            );
        }
    }

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenariosTests {

        @Test
        @DisplayName("Should handle mix of short, tolerance-length, and long sentences")
        void chunkTextSimple_withMixedSentenceLengths_shouldChunkCorrectly() {
            String text = "Short sentence. "
                    + "This sentence fits exactly within the tolerance limit set. "
                    + "This next sentence is significantly longer and will definitely require a hard split because thereisnowhitespacehere. "
                    + "Final short one.";

            int chunkSize = 50;
            int tolerance = 10; // Max length 60

            List<String> chunks = dataExtraction.chunkTextSimple(text, chunkSize, tolerance);

            assertThat(chunks).containsExactly(
                    "Short sentence.",
                    "This sentence fits exactly within the tolerance limit set.",
                    "This next sentence is significantly longer and",
                    "will definitely require a hard split because",
                    "thereisnowhitespacehere.",
                    "Final short one."
            );
        }
    }
}
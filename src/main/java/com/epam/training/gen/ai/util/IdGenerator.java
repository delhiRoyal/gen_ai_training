package com.epam.training.gen.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Slf4j
@Component
public class IdGenerator {

    /**
     * Generates a consistent, name-based UUID (Version 5 using SHA-1 internally)
     * from the input text. This provides strong collision resistance while
     * maintaining the standard UUID format.
     *
     * @param text The input text to generate an ID for. Must not be null.
     * @return A standard UUID string derived consistently from the input text,
     *         or a random UUID if the input is null or an unexpected error occurs.
     */
    public String generateConsistentId(String text) {
        if (text == null) {
            log.warn("Input text for ID generation is null, falling back to random UUID.");
            return UUID.randomUUID().toString();
        }
        try {
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

            UUID generatedUuid = UUID.nameUUIDFromBytes(textBytes);

            return generatedUuid.toString();
        } catch (Exception e) {
            log.error("Failed to generate name-based UUID for text, falling back to random UUID. Text starts with: '{}...'",
                    text.substring(0, Math.min(text.length(), 50)), e);
            return UUID.randomUUID().toString();
        }
    }
}

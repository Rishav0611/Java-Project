package com.privacyshield;

/**
 * Makes fake values that keep the length and format of the real value,
 * so masked text still looks normal to an AI agent.
 */
public interface FakeValueGenerator {

    /**
     * Returns a fake value for the given detected type.
     */
    String generate(DataType type, String realValue);

    /**
     * Returns a fake word with the same length and character types.
     * Used for custom words that a user adds to the watch list.
     */
    String generateWord(String realValue);
}

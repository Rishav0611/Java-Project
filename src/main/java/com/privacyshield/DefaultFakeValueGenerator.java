package com.privacyshield;

import java.util.Random;

/**
 * Default fake value generator.
 *
 * Every fake keeps the same length and the same separators as the real value:
 * a phone number keeps its +91 prefix and spaces, an Aadhaar number keeps its
 * spaces or hyphens and a valid check digit, and a PAN keeps its holder type.
 */
public class DefaultFakeValueGenerator implements FakeValueGenerator {
    private static final String PAN_HOLDER_TYPES = "PCHFATBLJG";

    private final Random random;

    public DefaultFakeValueGenerator() {
        this(new Random());
    }

    public DefaultFakeValueGenerator(long seed) {
        this(new Random(seed));
    }

    DefaultFakeValueGenerator(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }
        this.random = random;
    }

    @Override
    public String generate(DataType type, String realValue) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (realValue == null || realValue.isEmpty()) {
            throw new IllegalArgumentException("realValue cannot be null or empty");
        }

        String fake = buildFake(type, realValue);
        int attempts = 0;

        // Very rarely a random fake can equal the real value, so try again.
        while (fake.equals(realValue) && attempts < 20) {
            fake = buildFake(type, realValue);
            attempts++;
        }

        return fake;
    }

    @Override
    public String generateWord(String realValue) {
        if (realValue == null || realValue.isEmpty()) {
            throw new IllegalArgumentException("realValue cannot be null or empty");
        }

        return replaceLettersAndDigits(realValue);
    }

    private String buildFake(DataType type, String realValue) {
        switch (type) {
            case AADHAAR:
                return fakeAadhaar(realValue);
            case PAN:
                return fakePan(realValue);
            case PHONE:
                return fakePhone(realValue);
            case EMAIL:
                return replaceLettersAndDigits(realValue);
            default:
                return replaceLettersAndDigits(realValue);
        }
    }

    /**
     * Keeps the +91 prefix and any spaces or hyphens. The ten mobile digits are
     * replaced, and the first mobile digit stays in the 6-9 range.
     */
    private String fakePhone(String realValue) {
        StringBuilder fake = new StringBuilder(realValue);
        int digitsLeft = 10;

        for (int i = realValue.length() - 1; i >= 0 && digitsLeft > 0; i--) {
            char ch = realValue.charAt(i);

            if (ch >= '0' && ch <= '9') {
                int digit;

                if (digitsLeft == 1) {
                    digit = 6 + random.nextInt(4);
                } else {
                    digit = random.nextInt(10);
                }

                fake.setCharAt(i, (char) ('0' + digit));
                digitsLeft--;
            }
        }

        return fake.toString();
    }

    /**
     * Builds 12 digits that start with 2-9 and have a valid Verhoeff check digit,
     * then puts them in the same places as the real digits.
     */
    private String fakeAadhaar(String realValue) {
        StringBuilder digits = new StringBuilder();
        digits.append(2 + random.nextInt(8));

        for (int i = 1; i < 11; i++) {
            digits.append(random.nextInt(10));
        }

        digits.append(Verhoeff.checkDigit(digits.toString()));

        StringBuilder fake = new StringBuilder(realValue);
        int index = 0;

        for (int i = 0; i < realValue.length(); i++) {
            char ch = realValue.charAt(i);

            if (ch >= '0' && ch <= '9') {
                fake.setCharAt(i, digits.charAt(index));
                index++;
            }
        }

        return fake.toString();
    }

    /**
     * Builds a PAN with a real holder-type character at the fourth position
     * and keeps the letter case of the real value.
     */
    private String fakePan(String realValue) {
        StringBuilder fake = new StringBuilder();

        fake.append(letterLike(realValue.charAt(0)));
        fake.append(letterLike(realValue.charAt(1)));
        fake.append(letterLike(realValue.charAt(2)));

        char holder = PAN_HOLDER_TYPES.charAt(random.nextInt(PAN_HOLDER_TYPES.length()));

        if (Character.isLowerCase(realValue.charAt(3))) {
            holder = Character.toLowerCase(holder);
        }

        fake.append(holder);
        fake.append(letterLike(realValue.charAt(4)));

        for (int i = 0; i < 4; i++) {
            fake.append(random.nextInt(10));
        }

        fake.append(letterLike(realValue.charAt(9)));

        return fake.toString();
    }

    /**
     * Replaces letters with letters and digits with digits, so the length,
     * dots, at-signs and other punctuation stay exactly the same.
     */
    private String replaceLettersAndDigits(String realValue) {
        StringBuilder fake = new StringBuilder(realValue.length());

        for (int i = 0; i < realValue.length(); i++) {
            char ch = realValue.charAt(i);

            if (Character.isLetter(ch)) {
                fake.append(letterLike(ch));
            } else if (Character.isDigit(ch)) {
                fake.append((char) ('0' + random.nextInt(10)));
            } else {
                fake.append(ch);
            }
        }

        return fake.toString();
    }

    private char letterLike(char original) {
        char letter = (char) ('A' + random.nextInt(26));

        if (Character.isLowerCase(original)) {
            return Character.toLowerCase(letter);
        }

        return letter;
    }
}

package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultFakeValueGenerator}.
 *
 * The checks look at the shape of the fake value, not at an exact random value.
 */
class FakeValueGeneratorTest {

    private static final String PAN_HOLDER_TYPES = "PCHFATBLJG";

    private final FakeValueGenerator generator = new DefaultFakeValueGenerator(12345L);

    @Test
    void phoneFakeKeepsTheCountryCodeAndSeparators() {
        String real = "+91 98765 43210";
        String fake = generator.generate(DataType.PHONE, real);

        assertEquals(real.length(), fake.length());
        assertTrue(fake.startsWith("+91 "));
        assertNotEquals(real, fake);
        assertEquals(12, countDigits(fake));
        assertEquals(3, fake.indexOf(' '));
        assertEquals(9, fake.lastIndexOf(' '));
        assertTrue(fake.charAt(4) >= '6' && fake.charAt(4) <= '9', fake);
        assertEquals(1, new PhoneDetector().detect(fake).size());
    }

    @Test
    void phoneFakeKeepsHyphens() {
        String real = "9876-543-210";
        String fake = generator.generate(DataType.PHONE, real);

        assertEquals(real.length(), fake.length());
        assertEquals('-', fake.charAt(4));
        assertEquals('-', fake.charAt(8));
        assertEquals(1, new PhoneDetector().detect(fake).size());
    }

    @Test
    void aadhaarFakeHasAValidCheckDigit() {
        String real = "2345 6789 0124";
        String fake = generator.generate(DataType.AADHAAR, real);

        assertEquals(real.length(), fake.length());
        assertEquals(' ', fake.charAt(4));
        assertEquals(' ', fake.charAt(9));
        assertNotEquals(real, fake);

        String digits = fake.replace(" ", "").replace("-", "");
        assertEquals(12, digits.length());
        assertTrue(digits.charAt(0) >= '2' && digits.charAt(0) <= '9', digits);
        assertTrue(Verhoeff.isValid(digits), digits);
        assertEquals(1, new AadhaarDetector().detect(fake).size());
    }

    @Test
    void panFakeKeepsLengthHolderTypeAndCase() {
        String real = "ABCPD1234E";
        String fake = generator.generate(DataType.PAN, real);

        assertEquals(10, fake.length());
        assertNotEquals(real, fake);
        assertTrue(PAN_HOLDER_TYPES.indexOf(fake.charAt(3)) >= 0, fake);
        assertTrue(Character.isUpperCase(fake.charAt(0)));
        assertEquals(4, countDigits(fake));
        assertEquals(1, new PanDetector().detect(fake).size());
    }

    @Test
    void panFakeKeepsLowercase() {
        String fake = generator.generate(DataType.PAN, "abcpd1234e");

        assertTrue(Character.isLowerCase(fake.charAt(0)));
        assertTrue(Character.isLowerCase(fake.charAt(3)));
        assertEquals(1, new PanDetector().detect(fake).size());
    }

    @Test
    void emailFakeKeepsLengthAndPunctuation() {
        String real = "person@example.com";
        String fake = generator.generate(DataType.EMAIL, real);

        assertEquals(real.length(), fake.length());
        assertNotEquals(real, fake);
        assertEquals(real.indexOf('@'), fake.indexOf('@'));
        assertEquals(real.lastIndexOf('.'), fake.lastIndexOf('.'));
        assertEquals(1, new EmailDetector().detect(fake).size());
    }

    @Test
    void wordFakeKeepsLengthAndCharacterTypes() {
        String fake = generator.generateWord("Rishav-2026");

        assertEquals("Rishav-2026".length(), fake.length());
        assertEquals('-', fake.charAt(6));
        assertTrue(Character.isLetter(fake.charAt(0)));
        assertEquals(4, countDigits(fake.substring(7)));
    }

    @Test
    void sameSeedGivesSameFake() {
        FakeValueGenerator first = new DefaultFakeValueGenerator(7L);
        FakeValueGenerator second = new DefaultFakeValueGenerator(7L);

        assertEquals(
                first.generate(DataType.PHONE, "9876543210"),
                second.generate(DataType.PHONE, "9876543210")
        );
    }

    @Test
    void fakeIsNeverTheRealValue() {
        for (int i = 0; i < 25; i++) {
            assertNotEquals("a@b.com", generator.generate(DataType.EMAIL, "a@b.com"));
        }
    }

    @Test
    void allPhoneFakesPassTheDetector() {
        for (int i = 0; i < 20; i++) {
            String fake = generator.generate(DataType.PHONE, "9876543210");
            List<Detection> found = new PhoneDetector().detect(fake);

            assertEquals(1, found.size(), fake);
        }
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null, "9876543210"));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(DataType.PHONE, null));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(DataType.PHONE, ""));
        assertThrows(IllegalArgumentException.class, () -> generator.generateWord(null));
        assertThrows(IllegalArgumentException.class, () -> generator.generateWord(""));
    }

    private int countDigits(String text) {
        int count = 0;

        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                count++;
            }
        }

        return count;
    }
}

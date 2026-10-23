package com.privacyshield.mask;

import java.util.Map;

public record MaskResult(String maskedText, Map<String, String> tokenToValue) {
}
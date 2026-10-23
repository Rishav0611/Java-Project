package com.privacyshield.detect;

public record Detection(int start, int end, DataType type, String value) {
}
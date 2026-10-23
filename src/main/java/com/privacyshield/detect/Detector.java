package com.privacyshield.detect;

import java.util.List;

public interface Detector {
    DataType type();
    List<Detection> detect(String text);
}
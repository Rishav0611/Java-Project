package com.privacyshield;

import java.util.List;

public interface Detector {
    List<Detection> detect(String text);
}


package com.privacyshield;

public class Detection {
    private final DataType type;
    private final String value;
    private final int start;
    private final int end;

    public Detection(DataType type, String value, int start, int end) {
        this.type = type;
        this.value = value;
        this.start = start;
        this.end = end;
    }

    public DataType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}


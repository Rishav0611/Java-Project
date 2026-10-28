package com.privacyshield;

import java.util.Locale;

/**
 * One piece of metadata found in a photo, such as the camera model, the date
 * the photo was taken, or a GPS coordinate.
 */
public class PhotoMetadata {
    private final String group;
    private final String tag;
    private final String value;

    public PhotoMetadata(String group, String tag, String value) {
        this.group = group == null ? "" : group;
        this.tag = tag == null ? "" : tag;
        this.value = value == null ? "" : value;
    }

    /**
     * The metadata group, for example "GPS", "Exif IFD0" or "XMP".
     */
    public String getGroup() {
        return group;
    }

    /**
     * The tag name, for example "Make" or "GPS Latitude".
     */
    public String getTag() {
        return tag;
    }

    public String getValue() {
        return value;
    }

    /**
     * True when this is a GPS tag, which can reveal where the photo was taken.
     */
    public boolean isLocation() {
        String groupText = group.toLowerCase(Locale.ROOT);
        String tagText = tag.toLowerCase(Locale.ROOT);

        return groupText.contains("gps")
                || tagText.contains("gps")
                || tagText.contains("latitude")
                || tagText.contains("longitude");
    }

    @Override
    public String toString() {
        return group + " | " + tag + " = " + value;
    }
}

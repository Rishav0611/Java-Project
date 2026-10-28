package com.privacyshield;

/**
 * What to do with the fake-to-real map after a text has been masked.
 *
 * KEEP   remembers the map in memory so the AI reply can be restored.
 * DELETE forgets the map at once and is the default.
 */
public enum KeepPolicy {
    KEEP("Keep"),
    DELETE("Delete");

    private final String label;

    KeepPolicy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * The safe choice: if the user closes the dialog without choosing,
     * the saved values are forgotten.
     */
    public static KeepPolicy getDefault() {
        return DELETE;
    }

    @Override
    public String toString() {
        return label;
    }
}

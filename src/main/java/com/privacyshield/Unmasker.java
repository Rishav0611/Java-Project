package com.privacyshield;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Puts the real values back into a text by replacing every fake value
 * from the fake-to-real map.
 */
public class Unmasker {

    public String unmask(String text, Map<String, String> fakeToReal) {
        if (text == null || text.isEmpty()
                || fakeToReal == null || fakeToReal.isEmpty()) {
            return text;
        }

        List<String> fakes = new ArrayList<String>(fakeToReal.keySet());

        // Try the longest fake first, so a short fake cannot cut a longer one.
        Collections.sort(fakes, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return Integer.compare(right.length(), left.length());
            }
        });

        StringBuilder restored = new StringBuilder(text.length());
        int index = 0;

        while (index < text.length()) {
            String match = null;

            for (String fake : fakes) {
                if (text.startsWith(fake, index)) {
                    match = fake;
                    break;
                }
            }

            if (match == null) {
                restored.append(text.charAt(index));
                index++;
            } else {
                restored.append(fakeToReal.get(match));
                index += match.length();
            }
        }

        return restored.toString();
    }
}

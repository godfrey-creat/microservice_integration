package com.ncba.countryinfo.util;

import java.util.Locale;

public final class TextUtils {

    private TextUtils() {
    }

    /**
     * Sentence case: trims, collapses repeated spaces, lower-cases everything,
     * then upper-cases the first letter. "kenya" -> "Kenya", "  TANZANIA " -> "Tanzania".
     */
    public static String toSentenceCase(String input) {
        if (input == null) {
            return null;
        }
        String normalised = input.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) {
            return normalised;
        }
        return normalised.substring(0, 1).toUpperCase(Locale.ROOT) + normalised.substring(1);
    }
}

package com.ncba.countryinfo.util;

import java.util.Locale;
import java.util.Set;

public final class TextUtils {

    /** Words kept lowercase in title case unless they are the first word. */
    private static final Set<String> MINOR_WORDS = Set.of("and", "of", "the", "da", "de", "du", "la");

    private TextUtils() {
    }

    /**
     * Sentence case (required by the brief): trims, collapses repeated spaces, lower-cases
     * everything, then upper-cases the first letter. "kenya" -> "Kenya", "SOUTH AFRICA" -> "South africa".
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

    /**
     * Title case, used as a fallback because the SOAP service matches names case-sensitively:
     * "south africa" -> "South Africa", "bosnia and herzegovina" -> "Bosnia and Herzegovina",
     * "guinea-bissau" -> "Guinea-Bissau".
     */
    public static String toTitleCase(String input) {
        String normalised = toSentenceCase(input);
        if (normalised == null || normalised.isEmpty()) {
            return normalised;
        }
        String[] words = normalised.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            String word = words[i];
            result.append(i > 0 && MINOR_WORDS.contains(word) ? word : capitaliseParts(word));
        }
        return result.toString();
    }

    /** Upper-cases the first letter and any letter that follows '-' or '('. */
    private static String capitaliseParts(String word) {
        char[] chars = word.toCharArray();
        boolean capitaliseNext = true;
        for (int i = 0; i < chars.length; i++) {
            if (capitaliseNext && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                capitaliseNext = false;
            } else if (chars[i] == '-' || chars[i] == '(') {
                capitaliseNext = true;
            }
        }
        return new String(chars);
    }
}

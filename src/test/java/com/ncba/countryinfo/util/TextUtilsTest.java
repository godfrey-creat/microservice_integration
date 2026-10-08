package com.ncba.countryinfo.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "kenya, Kenya",
            "KENYA, Kenya",
            "tAnZaNiA, Tanzania",
            "'  uganda  ', Uganda",
            "'south   africa', South africa"
    })
    void convertsToSentenceCase(String input, String expected) {
        assertThat(TextUtils.toSentenceCase(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "kenya, Kenya",
            "south africa, South Africa",
            "'UNITED   KINGDOM', United Kingdom",
            "bosnia and herzegovina, Bosnia and Herzegovina",
            "guinea-bissau, Guinea-Bissau"
    })
    void convertsToTitleCase(String input, String expected) {
        assertThat(TextUtils.toTitleCase(input)).isEqualTo(expected);
    }
}

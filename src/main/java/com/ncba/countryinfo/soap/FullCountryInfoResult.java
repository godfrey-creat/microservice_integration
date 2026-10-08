package com.ncba.countryinfo.soap;

import java.util.List;

/** Parsed FullCountryInfoResult element of the SOAP response. */
public record FullCountryInfoResult(
        String isoCode,
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String currencyIsoCode,
        String countryFlag,
        List<LanguageResult> languages) {

    public record LanguageResult(String isoCode, String name) {
    }
}

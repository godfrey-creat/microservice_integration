package com.ncba.countryinfo.dto;

import java.time.Instant;
import java.util.List;

/** What the API returns for a stored country (the entity itself is never exposed). */
public record CountryInfoResponse(
        Long id,
        String isoCode,
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String currencyIsoCode,
        String countryFlag,
        List<LanguageDto> languages,
        Instant createdAt,
        Instant updatedAt) {
}

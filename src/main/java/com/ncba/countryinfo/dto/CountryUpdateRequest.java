package com.ncba.countryinfo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of PUT /api/v1/countries/{id}. Replaces the editable fields of a stored country.
 * The ISO code is the natural key and is not editable. If "languages" is omitted the
 * existing languages are kept; if it is provided (even empty) it replaces them.
 */
public record CountryUpdateRequest(
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        @Size(max = 100) String capitalCity,
        @Size(max = 10) String phoneCode,
        @Size(max = 5) String continentCode,
        @Size(max = 5) String currencyIsoCode,
        @Size(max = 255) String countryFlag,
        @Valid List<LanguageDto> languages) {
}

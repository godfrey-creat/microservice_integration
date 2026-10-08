package com.ncba.countryinfo.dto;

/** Temporary Step 4 response; Step 5/6 will return the full stored country instead. */
public record CountryIsoCodeResponse(String receivedName, String countryName, String isoCode) {
}

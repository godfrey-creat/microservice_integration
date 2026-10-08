package com.ncba.countryinfo.service;

import com.ncba.countryinfo.dto.CountryInfoResponse;

/**
 * Outcome of registering a country.
 *
 * @param country the stored country
 * @param created true if a new row was inserted, false if an existing row was refreshed
 * @param source  where the data came from: live SOAP call, or the stored copy because SOAP was down
 */
public record RegistrationResult(CountryInfoResponse country, boolean created, Source source) {

    public enum Source { SOAP, DATABASE_FALLBACK }
}

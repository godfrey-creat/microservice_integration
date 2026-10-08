package com.ncba.countryinfo.service;

import com.ncba.countryinfo.dto.CountryInfoResponse;

/**
 * Outcome of registering a country.
 *
 * @param country the stored country
 * @param created true if a new row was inserted, false if an existing row was refreshed
 */
public record RegistrationResult(CountryInfoResponse country, boolean created) {
}

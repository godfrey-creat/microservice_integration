package com.ncba.countryinfo.exception;

/** The SOAP service answered normally but does not know the country. Maps to 404. */
public class CountryNotFoundException extends RuntimeException {
    public CountryNotFoundException(String message) {
        super(message);
    }
}

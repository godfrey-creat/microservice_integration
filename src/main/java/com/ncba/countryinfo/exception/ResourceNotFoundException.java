package com.ncba.countryinfo.exception;

/** A stored record (e.g. a country by id) does not exist. Maps to 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

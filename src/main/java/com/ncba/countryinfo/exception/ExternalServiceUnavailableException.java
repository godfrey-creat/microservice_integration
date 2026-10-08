package com.ncba.countryinfo.exception;

/** The SOAP service is down and no stored copy exists to fall back on. Maps to 503. */
public class ExternalServiceUnavailableException extends RuntimeException {
    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

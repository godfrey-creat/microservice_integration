package com.ncba.countryinfo.exception;

/** The SOAP call itself failed: timeout, connection error, HTTP error, SOAP Fault or unreadable XML. Maps to 502. */
public class SoapServiceException extends RuntimeException {
    public SoapServiceException(String message) {
        super(message);
    }

    public SoapServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

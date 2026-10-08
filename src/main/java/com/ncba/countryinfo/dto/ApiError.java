package com.ncba.countryinfo.dto;

import java.time.Instant;
import java.util.Map;

/** Uniform error body returned for every failed request. correlationId links it to the logs. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, String> fieldErrors) {
}

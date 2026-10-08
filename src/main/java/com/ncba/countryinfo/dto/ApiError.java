package com.ncba.countryinfo.dto;

import java.time.Instant;
import java.util.Map;

/** Uniform error body returned for every failed request. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}

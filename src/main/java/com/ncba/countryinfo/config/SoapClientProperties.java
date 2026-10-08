package com.ncba.countryinfo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** SOAP endpoint and timeouts, read from application.properties / environment variables. */
@Validated
@ConfigurationProperties(prefix = "country-info.soap")
public record SoapClientProperties(
        @NotBlank String endpointUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}

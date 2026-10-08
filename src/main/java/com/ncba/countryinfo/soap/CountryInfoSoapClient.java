package com.ncba.countryinfo.soap;

import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.exception.SoapServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Client for the CountryInfoService SOAP API.
 *
 * Resilience, from the outside in:
 *   1. Cache (Caffeine) - repeat lookups skip the network entirely.
 *   2. Retry            - up to 3 attempts with exponential backoff for transient failures.
 *   3. Circuit breaker  - after repeated failures, calls fail fast instead of piling up on a dead upstream.
 *   4. Timeouts         - connect/read timeouts on the HTTP client (SoapClientConfig).
 * CountryNotFoundException is a normal business answer: not retried, does not trip the breaker, not cached.
 */
@Component
public class CountryInfoSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoSoapClient.class);
    private static final String RESILIENCE_NAME = "countryInfoSoap";
    private static final Pattern ISO_CODE = Pattern.compile("^[A-Z]{2,3}$");
    private static final MediaType SOAP_11 = new MediaType("text", "xml", StandardCharsets.UTF_8);

    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    public CountryInfoSoapClient(RestClient soapRestClient, MeterRegistry meterRegistry) {
        this.restClient = soapRestClient;
        this.meterRegistry = meterRegistry;
    }

    /** Step 4: CountryISOCode(sCountryName) -> CountryISOCodeResult, e.g. "Kenya" -> "KE". */
    @Cacheable(cacheNames = "isoCodes", key = "#countryName")
    @Retry(name = RESILIENCE_NAME)
    @CircuitBreaker(name = RESILIENCE_NAME)
    public String getCountryIsoCode(String countryName) {
        String response = call("CountryISOCode", SoapEnvelopes.countryIsoCode(countryName));
        String isoCode = SoapResponseParser.parseCountryIsoCode(response);

        if (!ISO_CODE.matcher(isoCode).matches()) {
            log.info("soap_country_not_found countryName=\"{}\" serviceAnswer=\"{}\"", countryName, isoCode);
            throw new CountryNotFoundException("No country found with the name '%s'".formatted(countryName));
        }
        log.info("soap_iso_code_resolved countryName=\"{}\" isoCode={}", countryName, isoCode);
        return isoCode;
    }

    /** Step 5: FullCountryInfo(sCountryISOCode) -> FullCountryInfoResult. */
    @Cacheable(cacheNames = "fullCountryInfo", key = "#isoCode")
    @Retry(name = RESILIENCE_NAME)
    @CircuitBreaker(name = RESILIENCE_NAME)
    public FullCountryInfoResult getFullCountryInfo(String isoCode) {
        String response = call("FullCountryInfo", SoapEnvelopes.fullCountryInfo(isoCode));
        FullCountryInfoResult result = SoapResponseParser.parseFullCountryInfo(response);

        if (result.isoCode() == null || result.isoCode().isBlank()) {
            log.info("soap_full_info_not_found isoCode={}", isoCode);
            throw new CountryNotFoundException("No country information found for ISO code '%s'".formatted(isoCode));
        }
        log.info("soap_full_info_fetched isoCode={} name=\"{}\" languages={}",
                isoCode, result.name(), result.languages().size());
        return result;
    }

    private String call(String operation, String envelope) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return restClient.post()
                    .contentType(SOAP_11)
                    .header("SOAPAction", "\"\"")
                    .body(envelope)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            outcome = "http_" + e.getStatusCode().value();
            String fault = SoapResponseParser.faultStringOrNull(e.getResponseBodyAsString());
            log.warn("soap_http_error operation={} status={} fault=\"{}\"", operation, e.getStatusCode().value(), fault);
            throw new SoapServiceException("SOAP %s returned HTTP %d".formatted(operation, e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            outcome = "io_error";
            log.warn("soap_transport_error operation={} error=\"{}\"", operation, e.getMessage());
            throw new SoapServiceException("SOAP %s call failed: %s".formatted(operation, e.getMessage()), e);
        } finally {
            // Latency per operation and outcome -> /actuator/prometheus as soap_client_requests_seconds
            sample.stop(Timer.builder("soap.client.requests")
                    .description("Latency of calls to the CountryInfo SOAP service")
                    .tag("operation", operation)
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}

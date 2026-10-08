package com.ncba.countryinfo.soap;

import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.exception.SoapServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Client for the CountryInfoService SOAP API. */
@Component
public class CountryInfoSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoSoapClient.class);
    private static final Pattern ISO_CODE = Pattern.compile("^[A-Z]{2,3}$");
    private static final MediaType SOAP_11 = new MediaType("text", "xml", StandardCharsets.UTF_8);

    private final RestClient restClient;

    public CountryInfoSoapClient(RestClient soapRestClient) {
        this.restClient = soapRestClient;
    }

    /** Step 4: CountryISOCode(sCountryName) -> CountryISOCodeResult, e.g. "Kenya" -> "KE". */
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

    private String call(String operation, String envelope) {
        try {
            return restClient.post()
                    .contentType(SOAP_11)
                    .header("SOAPAction", "\"\"")
                    .body(envelope)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            String fault = SoapResponseParser.faultStringOrNull(e.getResponseBodyAsString());
            log.warn("soap_http_error operation={} status={} fault=\"{}\"", operation, e.getStatusCode().value(), fault);
            throw new SoapServiceException("SOAP %s returned HTTP %d".formatted(operation, e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            log.warn("soap_transport_error operation={} error=\"{}\"", operation, e.getMessage());
            throw new SoapServiceException("SOAP %s call failed: %s".formatted(operation, e.getMessage()), e);
        }
    }
}

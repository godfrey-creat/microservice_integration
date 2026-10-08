package com.ncba.countryinfo.service;

import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.soap.CountryInfoSoapClient;
import com.ncba.countryinfo.soap.FullCountryInfoResult;
import com.ncba.countryinfo.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Business logic: the controller handles HTTP, this class handles the flow. */
@Service
public class CountryInfoService {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoService.class);

    private final CountryInfoSoapClient soapClient;

    public CountryInfoService(CountryInfoSoapClient soapClient) {
        this.soapClient = soapClient;
    }

    /** Steps 3-5: sentence case -> ISO code (CountryISOCode) -> full info (FullCountryInfo). */
    public FullCountryInfoResult fetchCountryInfo(String rawName) {
        String isoCode = resolveIsoCode(rawName);
        return soapClient.getFullCountryInfo(isoCode);
    }

    /** Step 4: sentence case first (as the brief requires); title case as a fallback for multi-word names. */
    private String resolveIsoCode(String rawName) {
        String sentenceCase = TextUtils.toSentenceCase(rawName);
        log.info("resolve_iso_code_requested rawName=\"{}\" countryName=\"{}\"", rawName, sentenceCase);

        try {
            return soapClient.getCountryIsoCode(sentenceCase);
        } catch (CountryNotFoundException notFound) {
            // The SOAP service matches names case-sensitively ("South Africa", not "South africa").
            String titleCase = TextUtils.toTitleCase(rawName);
            if (titleCase.equals(sentenceCase)) {
                throw notFound;
            }
            log.info("retrying_with_title_case sentenceCase=\"{}\" titleCase=\"{}\"", sentenceCase, titleCase);
            return soapClient.getCountryIsoCode(titleCase);
        }
    }
}

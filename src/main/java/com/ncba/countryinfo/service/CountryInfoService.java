package com.ncba.countryinfo.service;

import com.ncba.countryinfo.dto.CountryIsoCodeResponse;
import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.soap.CountryInfoSoapClient;
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

    /** Steps 3-4: sentence-case the name, then resolve its ISO code via SOAP. */
    public CountryIsoCodeResponse resolveIsoCode(String rawName) {
        String sentenceCase = TextUtils.toSentenceCase(rawName);
        log.info("resolve_iso_code_requested rawName=\"{}\" countryName=\"{}\"", rawName, sentenceCase);

        try {
            return new CountryIsoCodeResponse(rawName, sentenceCase, soapClient.getCountryIsoCode(sentenceCase));
        } catch (CountryNotFoundException notFound) {
            // The SOAP service matches names case-sensitively ("South Africa", not "South africa"),
            // so multi-word names get one retry in title case.
            String titleCase = TextUtils.toTitleCase(rawName);
            if (titleCase.equals(sentenceCase)) {
                throw notFound;
            }
            log.info("retrying_with_title_case sentenceCase=\"{}\" titleCase=\"{}\"", sentenceCase, titleCase);
            return new CountryIsoCodeResponse(rawName, titleCase, soapClient.getCountryIsoCode(titleCase));
        }
    }
}

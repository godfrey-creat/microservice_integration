package com.ncba.countryinfo.soap;

import org.springframework.web.util.HtmlUtils;

/** Builds SOAP 1.1 request envelopes, matching the requests used in SoapUI. */
public final class SoapEnvelopes {

    public static final String SERVICE_NAMESPACE = "http://www.oorsprong.org/websamples.countryinfo";

    private static final String TEMPLATE = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  %s
               </soapenv:Body>
            </soapenv:Envelope>""";

    private SoapEnvelopes() {
    }

    /** CountryISOCode operation: takes sCountryName, returns CountryISOCodeResult. */
    public static String countryIsoCode(String countryName) {
        String body = "<web:CountryISOCode><web:sCountryName>%s</web:sCountryName></web:CountryISOCode>"
                .formatted(escape(countryName));
        return TEMPLATE.formatted(SERVICE_NAMESPACE, body);
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}

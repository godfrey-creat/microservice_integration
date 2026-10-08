package com.ncba.countryinfo.soap;

import com.ncba.countryinfo.exception.SoapServiceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SoapResponseParserTest {

    private static final String ISO_RESPONSE = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <m:CountryISOCodeResponse xmlns:m="http://www.oorsprong.org/websamples.countryinfo">
                  <m:CountryISOCodeResult>KE</m:CountryISOCodeResult>
                </m:CountryISOCodeResponse>
              </soap:Body>
            </soap:Envelope>""";

    private static final String FAULT_RESPONSE = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <soap:Fault>
                  <faultcode>soap:Server</faultcode>
                  <faultstring>Internal error</faultstring>
                </soap:Fault>
              </soap:Body>
            </soap:Envelope>""";

    @Test
    void parsesIsoCode() {
        assertThat(SoapResponseParser.parseCountryIsoCode(ISO_RESPONSE)).isEqualTo("KE");
    }

    @Test
    void soapFaultBecomesSoapServiceException() {
        assertThatThrownBy(() -> SoapResponseParser.parseCountryIsoCode(FAULT_RESPONSE))
                .isInstanceOf(SoapServiceException.class)
                .hasMessageContaining("Internal error");
    }

    @Test
    void rejectsXxePayload() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <CountryISOCodeResult>&xxe;</CountryISOCodeResult>""";
        assertThatThrownBy(() -> SoapResponseParser.parseCountryIsoCode(xxe))
                .isInstanceOf(SoapServiceException.class);
    }

    @Test
    void envelopeEscapesUserInput() {
        assertThat(SoapEnvelopes.countryIsoCode("<script>"))
                .contains("&lt;script&gt;")
                .doesNotContain("<script>");
    }
}

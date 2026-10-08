package com.ncba.countryinfo.soap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FullCountryInfoParserTest {

    private static final String FULL_INFO_RESPONSE = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <m:FullCountryInfoResponse xmlns:m="http://www.oorsprong.org/websamples.countryinfo">
                  <m:FullCountryInfoResult>
                    <m:sISOCode>KE</m:sISOCode>
                    <m:sName>Kenya</m:sName>
                    <m:sCapitalCity>Nairobi</m:sCapitalCity>
                    <m:sPhoneCode>254</m:sPhoneCode>
                    <m:sContinentCode>AF</m:sContinentCode>
                    <m:sCurrencyISOCode>KES</m:sCurrencyISOCode>
                    <m:sCountryFlag>http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Kenya.jpg</m:sCountryFlag>
                    <m:Languages>
                      <m:tLanguage>
                        <m:sISOCode>swa</m:sISOCode>
                        <m:sName>Swahili</m:sName>
                      </m:tLanguage>
                    </m:Languages>
                  </m:FullCountryInfoResult>
                </m:FullCountryInfoResponse>
              </soap:Body>
            </soap:Envelope>""";

    @Test
    void parsesFullCountryInfoWithLanguages() {
        FullCountryInfoResult info = SoapResponseParser.parseFullCountryInfo(FULL_INFO_RESPONSE);

        assertThat(info.isoCode()).isEqualTo("KE");          // not "swa" from the language
        assertThat(info.name()).isEqualTo("Kenya");
        assertThat(info.capitalCity()).isEqualTo("Nairobi");
        assertThat(info.phoneCode()).isEqualTo("254");
        assertThat(info.continentCode()).isEqualTo("AF");
        assertThat(info.currencyIsoCode()).isEqualTo("KES");
        assertThat(info.countryFlag()).endsWith("Kenya.jpg");
        assertThat(info.languages())
                .containsExactly(new FullCountryInfoResult.LanguageResult("swa", "Swahili"));
    }

    @Test
    void envelopeContainsIsoCode() {
        assertThat(SoapEnvelopes.fullCountryInfo("KE"))
                .contains("<web:FullCountryInfo>")
                .contains("<web:sCountryISOCode>KE</web:sCountryISOCode>");
    }
}

package com.ncba.countryinfo.controller;

import com.jayway.jsonpath.JsonPath;
import com.ncba.countryinfo.config.CorrelationIdFilter;
import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.exception.SoapServiceException;
import com.ncba.countryinfo.repository.CountryInfoRepository;
import com.ncba.countryinfo.soap.CountryInfoSoapClient;
import com.ncba.countryinfo.soap.FullCountryInfoResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through HTTP, service and an in-memory database, with the SOAP client mocked. */
@SpringBootTest
@ActiveProfiles("test")
class CountryInfoControllerIntegrationTest {

    private static final FullCountryInfoResult KENYA = new FullCountryInfoResult(
            "KE", "Kenya", "Nairobi", "254", "AF", "KES",
            "http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Kenya.jpg",
            List.of(new FullCountryInfoResult.LanguageResult("swa", "Swahili")));

    @Autowired WebApplicationContext context;
    @Autowired CorrelationIdFilter correlationIdFilter;
    @Autowired CountryInfoRepository repository;
    @MockitoBean CountryInfoSoapClient soapClient;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(correlationIdFilter).build();
        repository.deleteAll();
        when(soapClient.getCountryIsoCode("Kenya")).thenReturn("KE");
        when(soapClient.getFullCountryInfo("KE")).thenReturn(KENYA);
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        // Create: lower-case input is sentence-cased before the SOAP lookup -> 201
        String body = mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"kenya\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("X-Data-Source", "SOAP"))
                .andExpect(jsonPath("$.isoCode").value("KE"))
                .andExpect(jsonPath("$.capitalCity").value("Nairobi"))
                .andExpect(jsonPath("$.languages[0].name").value("Swahili"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        // Registering again refreshes the same row -> 200, same id (no duplicate)
        mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"KENYA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // Fetch all and by id
        mvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Kenya"));
        mvc.perform(get("/api/v1/countries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyIsoCode").value("KES"));

        // Update
        mvc.perform(put("/api/v1/countries/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Republic of Kenya","capitalCity":"Nairobi","phoneCode":"254",
                         "continentCode":"AF","currencyIsoCode":"KES",
                         "languages":[{"isoCode":"swa","name":"Swahili"},{"isoCode":"eng","name":"English"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Republic of Kenya"))
                .andExpect(jsonPath("$.languages.length()").value(2));

        // Delete, then it is gone
        mvc.perform(delete("/api/v1/countries/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/countries/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Country with id %d not found".formatted(id)));
    }

    @Test
    void unknownCountryReturns404WithCorrelationId() throws Exception {
        when(soapClient.getCountryIsoCode("Atlantis"))
                .thenThrow(new CountryNotFoundException("No country found with the name 'Atlantis'"));

        mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-123")
                        .content("{\"name\":\"atlantis\"}"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", "test-123"))
                .andExpect(jsonPath("$.correlationId").value("test-123"));
    }

    @Test
    void invalidInputReturns400() throws Exception {
        mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/countries/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soapOutageWithoutStoredDataReturns503() throws Exception {
        when(soapClient.getCountryIsoCode(anyString())).thenThrow(new SoapServiceException("timeout"));

        mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"kenya\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"));
    }

    @Test
    void soapOutageFallsBackToStoredCopy() throws Exception {
        mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"kenya\"}"))
                .andExpect(status().isCreated());

        when(soapClient.getCountryIsoCode(anyString())).thenThrow(new SoapServiceException("timeout"));

        mvc.perform(post("/api/v1/countries").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"kenya\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Data-Source", "DATABASE_FALLBACK"))
                .andExpect(jsonPath("$.isoCode").value("KE"));
    }
}

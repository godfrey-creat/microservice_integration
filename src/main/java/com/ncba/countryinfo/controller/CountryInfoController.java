package com.ncba.countryinfo.controller;

import com.ncba.countryinfo.dto.CountryNameRequest;
import com.ncba.countryinfo.service.CountryInfoService;
import com.ncba.countryinfo.soap.FullCountryInfoResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP layer only: validation and response. All logic lives in CountryInfoService. */
@RestController
@RequestMapping("/api/v1/countries")
public class CountryInfoController {

    private final CountryInfoService service;

    public CountryInfoController(CountryInfoService service) {
        this.service = service;
    }

    /**
     * Steps 3-5: { "name": "kenya" } -> sentence case -> ISO code -> full country info.
     * Step 6 will store the result in MySQL and return the stored record instead.
     */
    @PostMapping
    public FullCountryInfoResult register(@Valid @RequestBody CountryNameRequest request) {
        return service.fetchCountryInfo(request.name());
    }
}

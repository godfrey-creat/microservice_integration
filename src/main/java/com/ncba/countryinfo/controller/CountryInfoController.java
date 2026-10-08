package com.ncba.countryinfo.controller;

import com.ncba.countryinfo.dto.CountryIsoCodeResponse;
import com.ncba.countryinfo.dto.CountryNameRequest;
import com.ncba.countryinfo.service.CountryInfoService;
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

    /** Steps 3-4: { "name": "tanzania" } -> sentence case -> SOAP CountryISOCode. */
    @PostMapping
    public CountryIsoCodeResponse register(@Valid @RequestBody CountryNameRequest request) {
        return service.resolveIsoCode(request.name());
    }
}

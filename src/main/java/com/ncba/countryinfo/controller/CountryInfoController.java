package com.ncba.countryinfo.controller;

import com.ncba.countryinfo.dto.CountryInfoResponse;
import com.ncba.countryinfo.dto.CountryNameRequest;
import com.ncba.countryinfo.service.CountryInfoService;
import com.ncba.countryinfo.service.RegistrationResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** HTTP layer only: validation, status codes and headers. All logic lives in CountryInfoService. */
@RestController
@RequestMapping("/api/v1/countries")
public class CountryInfoController {

    private final CountryInfoService service;

    public CountryInfoController(CountryInfoService service) {
        this.service = service;
    }

    /** Steps 3-6: fetch from SOAP, store in MySQL, return the stored country. 201 if new, 200 if refreshed. */
    @PostMapping
    public ResponseEntity<CountryInfoResponse> register(@Valid @RequestBody CountryNameRequest request) {
        RegistrationResult result = service.registerCountry(request.name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(result.country().id()).toUri();
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).location(location).body(result.country());
    }
}

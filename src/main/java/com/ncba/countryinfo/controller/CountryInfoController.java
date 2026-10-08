package com.ncba.countryinfo.controller;

import com.ncba.countryinfo.dto.CountryInfoResponse;
import com.ncba.countryinfo.dto.CountryNameRequest;
import com.ncba.countryinfo.dto.CountryUpdateRequest;
import com.ncba.countryinfo.dto.PageResponse;
import com.ncba.countryinfo.service.CountryInfoService;
import com.ncba.countryinfo.service.RegistrationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return ResponseEntity.status(status)
                .location(location)
                .header("X-Data-Source", result.source().name())
                .body(result.country());
    }

    /** Step 7: fetch all (paged, sorted by name). */
    @GetMapping
    public PageResponse<CountryInfoResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    /** Step 7: fetch by id. */
    @GetMapping("/{id}")
    public CountryInfoResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    /** Step 7: update. */
    @PutMapping("/{id}")
    public CountryInfoResponse update(@PathVariable Long id, @Valid @RequestBody CountryUpdateRequest request) {
        return service.update(id, request);
    }

    /** Step 7: delete. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

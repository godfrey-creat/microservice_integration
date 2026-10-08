package com.ncba.countryinfo.controller;

import com.ncba.countryinfo.dto.CountryNameRequest;
import com.ncba.countryinfo.util.TextUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryInfoController {

    /**
     * Step 3: receive a country name and convert it to sentence case.
     * Step 4 will replace this temporary response with the SOAP lookup.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody CountryNameRequest request) {
        String countryName = TextUtils.toSentenceCase(request.name());
        return ResponseEntity.ok(Map.of(
                "receivedName", request.name(),
                "countryName", countryName));
    }
}

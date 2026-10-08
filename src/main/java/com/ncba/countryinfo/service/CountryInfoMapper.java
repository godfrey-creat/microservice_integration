package com.ncba.countryinfo.service;

import com.ncba.countryinfo.dto.CountryInfoResponse;
import com.ncba.countryinfo.dto.LanguageDto;
import com.ncba.countryinfo.model.CountryInfo;
import com.ncba.countryinfo.model.Language;
import com.ncba.countryinfo.soap.FullCountryInfoResult;

import java.util.List;

/** Converts between SOAP results, JPA entities and API DTOs, keeping each layer independent. */
final class CountryInfoMapper {

    private CountryInfoMapper() {
    }

    static void applySoapResult(CountryInfo entity, FullCountryInfoResult info) {
        entity.setIsoCode(info.isoCode());
        entity.setName(info.name());
        entity.setCapitalCity(info.capitalCity());
        entity.setPhoneCode(info.phoneCode());
        entity.setContinentCode(info.continentCode());
        entity.setCurrencyIsoCode(info.currencyIsoCode());
        entity.setCountryFlag(info.countryFlag());
        entity.replaceLanguages(info.languages().stream()
                .map(l -> new Language(l.isoCode(), l.name()))
                .toList());
    }

    static CountryInfoResponse toResponse(CountryInfo entity) {
        List<LanguageDto> languages = entity.getLanguages().stream()
                .map(l -> new LanguageDto(l.getIsoCode(), l.getName()))
                .toList();
        return new CountryInfoResponse(entity.getId(), entity.getIsoCode(), entity.getName(),
                entity.getCapitalCity(), entity.getPhoneCode(), entity.getContinentCode(),
                entity.getCurrencyIsoCode(), entity.getCountryFlag(), languages,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

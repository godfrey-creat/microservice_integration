package com.ncba.countryinfo.service;

import com.ncba.countryinfo.dto.CountryInfoResponse;
import com.ncba.countryinfo.dto.CountryUpdateRequest;
import com.ncba.countryinfo.dto.PageResponse;
import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.exception.ResourceNotFoundException;
import com.ncba.countryinfo.model.CountryInfo;
import com.ncba.countryinfo.model.Language;
import com.ncba.countryinfo.repository.CountryInfoRepository;
import com.ncba.countryinfo.soap.CountryInfoSoapClient;
import com.ncba.countryinfo.soap.FullCountryInfoResult;
import com.ncba.countryinfo.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Business logic. SOAP calls are made OUTSIDE any database transaction so a slow upstream
 * never holds a pooled DB connection; only the final save runs in a short transaction.
 */
@Service
public class CountryInfoService {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoService.class);

    private final CountryInfoSoapClient soapClient;
    private final CountryInfoRepository repository;
    private final TransactionTemplate transactionTemplate;

    public CountryInfoService(CountryInfoSoapClient soapClient, CountryInfoRepository repository,
                              TransactionTemplate transactionTemplate) {
        this.soapClient = soapClient;
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    // ---------- Steps 3-6: register a country from the SOAP service ----------

    /** Sentence case -> ISO code -> full info -> save to MySQL. */
    public RegistrationResult registerCountry(String rawName) {
        String isoCode = resolveIsoCode(rawName);
        FullCountryInfoResult info = soapClient.getFullCountryInfo(isoCode);

        RegistrationResult result;
        try {
            result = transactionTemplate.execute(status -> upsert(info));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests for the same new country: the loser retries as an update.
            log.info("register_country_race_detected isoCode={} - retrying as update", info.isoCode());
            result = transactionTemplate.execute(status -> upsert(info));
        }
        log.info("register_country_completed isoCode={} id={} created={}",
                info.isoCode(), result.country().id(), result.created());
        return result;
    }

    // ---------- Step 7: CRUD on stored countries ----------

    @Transactional(readOnly = true)
    public PageResponse<CountryInfoResponse> findAll(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable).map(CountryInfoMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CountryInfoResponse findById(Long id) {
        return CountryInfoMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public CountryInfoResponse update(Long id, CountryUpdateRequest request) {
        CountryInfo country = getOrThrow(id);
        country.setName(request.name().trim());
        country.setCapitalCity(request.capitalCity());
        country.setPhoneCode(request.phoneCode());
        country.setContinentCode(request.continentCode());
        country.setCurrencyIsoCode(request.currencyIsoCode());
        country.setCountryFlag(request.countryFlag());
        if (request.languages() != null) {
            country.replaceLanguages(request.languages().stream()
                    .map(l -> new Language(l.isoCode(), l.name()))
                    .toList());
        }
        CountryInfo saved = repository.saveAndFlush(country);
        log.info("country_updated id={} isoCode={}", id, saved.getIsoCode());
        return CountryInfoMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        CountryInfo country = getOrThrow(id);
        repository.delete(country);   // languages are removed too (cascade)
        log.info("country_deleted id={} isoCode={}", id, country.getIsoCode());
    }

    // ---------- helpers ----------

    private CountryInfo getOrThrow(Long id) {
        return repository.findWithLanguagesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country with id %d not found".formatted(id)));
    }

    /** Insert a new country, or refresh the existing row with the same ISO code. */
    private RegistrationResult upsert(FullCountryInfoResult info) {
        CountryInfo entity = repository.findByIsoCode(info.isoCode()).orElse(null);
        boolean created = entity == null;
        if (created) {
            entity = new CountryInfo();
        }
        CountryInfoMapper.applySoapResult(entity, info);
        CountryInfo saved = repository.saveAndFlush(entity);
        return new RegistrationResult(CountryInfoMapper.toResponse(saved), created);
    }

    /** Sentence case first (as the brief requires); title case as a fallback for multi-word names. */
    private String resolveIsoCode(String rawName) {
        String sentenceCase = TextUtils.toSentenceCase(rawName);
        log.info("resolve_iso_code_requested rawName=\"{}\" countryName=\"{}\"", rawName, sentenceCase);
        try {
            return soapClient.getCountryIsoCode(sentenceCase);
        } catch (CountryNotFoundException notFound) {
            String titleCase = TextUtils.toTitleCase(rawName);
            if (titleCase.equals(sentenceCase)) {
                throw notFound;
            }
            log.info("retrying_with_title_case sentenceCase=\"{}\" titleCase=\"{}\"", sentenceCase, titleCase);
            return soapClient.getCountryIsoCode(titleCase);
        }
    }
}

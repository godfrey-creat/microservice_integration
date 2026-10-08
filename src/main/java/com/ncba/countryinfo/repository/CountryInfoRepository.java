package com.ncba.countryinfo.repository;

import com.ncba.countryinfo.model.CountryInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryInfoRepository extends JpaRepository<CountryInfo, Long> {

    /** Loads the country and its languages in one query. */
    @EntityGraph(attributePaths = "languages")
    Optional<CountryInfo> findByIsoCode(String isoCode);

    @EntityGraph(attributePaths = "languages")
    Optional<CountryInfo> findWithLanguagesById(Long id);

    /** Used for the fallback when the SOAP service is unavailable. */
    @EntityGraph(attributePaths = "languages")
    Optional<CountryInfo> findFirstByNameIgnoreCase(String name);
}

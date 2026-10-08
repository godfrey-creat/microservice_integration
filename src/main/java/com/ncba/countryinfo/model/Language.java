package com.ncba.countryinfo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A language spoken in a country (the tLanguage elements of FullCountryInfo). */
@Entity
@Table(name = "country_language")
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso_code", nullable = false, length = 10)
    private String isoCode;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private CountryInfo country;

    protected Language() {
        // required by JPA
    }

    public Language(String isoCode, String name) {
        this.isoCode = isoCode;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getIsoCode() { return isoCode; }
    public String getName() { return name; }
    public CountryInfo getCountry() { return country; }
    void setCountry(CountryInfo country) { this.country = country; }
}

package com.ncba.countryinfo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A country as returned by the FullCountryInfo SOAP operation. The ISO code is the natural
 * key (unique), so registering the same country twice refreshes the row instead of duplicating it.
 */
@Entity
@Table(name = "country_info")
public class CountryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso_code", nullable = false, unique = true, length = 3)
    private String isoCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "capital_city", length = 100)
    private String capitalCity;

    @Column(name = "phone_code", length = 10)
    private String phoneCode;

    @Column(name = "continent_code", length = 5)
    private String continentCode;

    @Column(name = "currency_iso_code", length = 5)
    private String currencyIsoCode;

    @Column(name = "country_flag", length = 255)
    private String countryFlag;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    private List<Language> languages = new ArrayList<>();

    /** Optimistic locking: concurrent updates to the same country fail with 409 instead of silently overwriting. */
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Replaces all languages; orphanRemoval deletes the old rows. */
    public void replaceLanguages(List<Language> newLanguages) {
        languages.clear();
        newLanguages.forEach(this::addLanguage);
    }

    public void addLanguage(Language language) {
        language.setCountry(this);
        languages.add(language);
    }

    public Long getId() { return id; }
    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCapitalCity() { return capitalCity; }
    public void setCapitalCity(String capitalCity) { this.capitalCity = capitalCity; }
    public String getPhoneCode() { return phoneCode; }
    public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
    public String getContinentCode() { return continentCode; }
    public void setContinentCode(String continentCode) { this.continentCode = continentCode; }
    public String getCurrencyIsoCode() { return currencyIsoCode; }
    public void setCurrencyIsoCode(String currencyIsoCode) { this.currencyIsoCode = currencyIsoCode; }
    public String getCountryFlag() { return countryFlag; }
    public void setCountryFlag(String countryFlag) { this.countryFlag = countryFlag; }
    public List<Language> getLanguages() { return languages; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

CREATE TABLE country_info (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    iso_code          VARCHAR(3)   NOT NULL,
    name              VARCHAR(100) NOT NULL,
    capital_city      VARCHAR(100),
    phone_code        VARCHAR(10),
    continent_code    VARCHAR(5),
    currency_iso_code VARCHAR(5),
    country_flag      VARCHAR(255),
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_country_info_iso_code UNIQUE (iso_code)
);

CREATE INDEX idx_country_info_name ON country_info (name);

CREATE TABLE country_language (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    iso_code   VARCHAR(10)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    country_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_country_language_country
        FOREIGN KEY (country_id) REFERENCES country_info (id) ON DELETE CASCADE
);

CREATE INDEX idx_country_language_country ON country_language (country_id);

package com.ncba.countryinfo.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Turns on @Cacheable (Caffeine, configured in application.properties). */
@Configuration
@EnableCaching
public class CacheConfig {
}

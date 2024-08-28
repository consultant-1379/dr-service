/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.bos.dr.configuration;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache configuration.
 */
@Configuration
public class CacheConfiguration {

    @Value("${spring.caches.groovy-asset.access-expiry}")
    private Long groovyAssetCacheExpiry;

    @Value("${spring.caches.properties.access-expiry}")
    private Long propertiesCacheExpiry;

    @Value("${spring.caches.asset.access-expiry}")
    private Long assetCacheExpiry;

    /**
     * Properties cache.
     *
     * @return CaffeineCache
     */
    @Bean
    public CaffeineCache featurePackPropertiesCache() {
        return new CaffeineCache("fp_properties_cache",
                Caffeine.newBuilder()
                        .initialCapacity(20)
                        .maximumSize(1000)
                        .expireAfterAccess(propertiesCacheExpiry, TimeUnit.HOURS)
                        .build());
    }

    /**
     * Asset cache.
     *
     * @return CaffeineCache
     */
    @Bean
    public CaffeineCache assetCache() {
        return new CaffeineCache("asset_cache",
                Caffeine.newBuilder()
                        .initialCapacity(20)
                        .maximumSize(1000)
                        .expireAfterAccess(assetCacheExpiry, TimeUnit.SECONDS)
                        .build());
    }

    /**
     * Groovy asset cache.
     *
     * @return CaffeineCache
     */
    @Bean
    public CaffeineCache groovyAssetCache() {
        return new CaffeineCache("groovy_asset_cache",
                Caffeine.newBuilder()
                        .initialCapacity(20)
                        .maximumSize(1000)
                        .expireAfterAccess(groovyAssetCacheExpiry, TimeUnit.SECONDS)
                        .build());
    }
}

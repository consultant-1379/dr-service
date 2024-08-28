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
package com.ericsson.bos.dr.service;

import java.nio.charset.StandardCharsets;

import com.ericsson.bos.dr.jpa.AssetRepository;
import com.ericsson.bos.dr.jpa.model.AssetEntity;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manage Assets.
 */
@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    /**
     * Get the asset by name in a feature pack.
     *
     * @param name name of asset
     * @param featurePackId feature pack id
     * @return AssetEntity
     */
    @Transactional(readOnly = true)
    public AssetEntity getAsset(final String name, final long featurePackId) {
        return assetRepository.findByNameAndFeaturePackId(name, featurePackId)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Asset %s not found in feature pack %s", name, featurePackId)));
    }

    /**
     * Lookup asset contents from local cache or get from asset repository.
     * @param name asset name
     * @param featurePackId feature pack id
     * @return contents
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "asset_cache", key = "#name + \"_\" + #featurePackId")
    public byte[] getAssetContent(final String name, final long featurePackId) {
        return getAsset(name, featurePackId).getContents();
    }

    /**
     * Lookup groovy script in local cache or get from asset repository. The script
     * is put in cache if not already present and will remain in the cache unless the
     * configured cache access timeout has expired for the entry.
     * @param name script name
     * @param featurePackId feature pack id
     * @return groovy script
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "groovy_asset_cache", key = "#name + \"_\" + #featurePackId")
    public Class<Script> getGroovyScript(final String name, final long featurePackId) {
        final var asset = getAsset(name, featurePackId);
        final var scriptContent = new String(asset.getContents(), StandardCharsets.UTF_8);
        final var script = new GroovyShell().parse(scriptContent);
        return (Class<Script>) script.getClass();
    }
}
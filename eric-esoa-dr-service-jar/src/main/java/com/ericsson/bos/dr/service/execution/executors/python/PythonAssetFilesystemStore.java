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
package com.ericsson.bos.dr.service.execution.executors.python;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ericsson.bos.dr.jpa.model.AssetEntity;
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.service.AssetService;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import jakarta.persistence.PreRemove;
import org.apache.commons.collections4.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

/**
 * Manage Python assets on the filesystem.
 */
@Component
public class PythonAssetFilesystemStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonAssetFilesystemStore.class);

    private static final Map<Long, Set<String>> STORED_ASSETS = new HashedMap();

    private static String PYTHON_ASSETS_DIR;

    @Autowired
    private AssetService assetService;

    /**
     * Get the path to the python asset on the filesystem. If not existing, then the
     * asset is first written to the filesystem. Subsequent requests for the same asset
     * will return the path directly without checking for its existence on the filesystem. This
     * is to limit filesystem reads.
     * The method is thread-safe. If asset is not already stored, then the check for its existence and
     * writing  to the filesystem is performed in a synchronized block.
     *
     * @param featurePackId feature pack id
     * @param assetName     asset name
     * @return Path
     */
    public Path getPath(final Long featurePackId, final String assetName) {
        final var assetPath = Path.of(PYTHON_ASSETS_DIR, featurePackId.toString(), assetName);
        if (!isAssetStored(featurePackId, assetName)) {
            synchronized (this) {
                if (!Files.exists(assetPath)) {
                    final AssetEntity asset = assetService.getAsset(assetName, featurePackId);
                    writeAsset(new String(asset.getContents()), assetPath);
                }
                storeAsset(featurePackId, assetName);
            }
        }
        return assetPath;
    }

    private void storeAsset(final Long featurePackId, final String assetName) {
        final Set<String> assets = STORED_ASSETS.computeIfAbsent(featurePackId, id -> new HashSet<>());
        assets.add(assetName);
    }

    private boolean isAssetStored(final Long featurePackId, final String assetName) {
        return STORED_ASSETS.containsKey(featurePackId) && STORED_ASSETS.get(featurePackId).contains(assetName);
    }

    /**
     * Delete the feature pack assets when feature pack is deleted.
     *
     * @param featurePack feature pack entity
     */
    @PreRemove
    public void deleteAssetsForFeaturePack(final FeaturePackEntity featurePack) {
        final var featurePackDir = Paths.get(PYTHON_ASSETS_DIR, featurePack.getId().toString());
        try {
            LOGGER.info("Removing assets directory: {}", featurePackDir);
            STORED_ASSETS.remove(featurePack.getId());
            if (Files.isDirectory(featurePackDir)) {
                FileSystemUtils.deleteRecursively(featurePackDir);
            }
        } catch (IOException e) {
            LOGGER.warn("Error deleting directory {}", featurePackDir, e);
        }
    }

    private void writeAsset(final String contents, final Path path) {
        try {
            LOGGER.debug("Writing python asset: {}", path);
            Files.createDirectories(path.getParent());
            Files.writeString(path, contents);
        } catch (IOException e) {
            throw new DRServiceException(ErrorCode.EXECUTOR_FILE_ERROR, e.getMessage());
        }
    }

    /**
     * Set configured python assets directory to static variable, so
     * it is available in @PreRemove method.
     * @param dir python assets directory
     */
    @Value("${service.python-executor.assets-dir}")
    void setPythonAssetsDir(String dir) {
        PythonAssetFilesystemStore.PYTHON_ASSETS_DIR = dir;
    }
}
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

package com.ericsson.bos.dr.tests.unit.execution

import com.ericsson.bos.dr.jpa.model.AssetEntity
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity
import com.ericsson.bos.dr.service.AssetService
import com.ericsson.bos.dr.service.execution.executors.python.PythonAssetFilesystemStore
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PythonAssetFilesystemStoreSpec extends Specification {

    AssetService assetService = Mock(AssetService)
    PythonAssetFilesystemStore pythonAssetFilesystemStore = new PythonAssetFilesystemStore(assetService: assetService, pythonAssetsDir: System
            .getProperty("java.io.tmpdir"))

    AssetEntity assetEntity = new AssetEntity(contents: "print('hello')".bytes)

    long featurePackId = System.nanoTime()

    def "get path writes the asset to the filesystems if not previously stored"() {

        setup: "Mock get Asset"
        1 * assetService.getAsset(_, _) >> assetEntity

        when: "Get Path"
        Path path = pythonAssetFilesystemStore.getPath(System.currentTimeMillis(), "asset1.py")

        then: "Asset is written to the filesystem"
        Files.readAllBytes(path) == assetEntity.contents
    }

    def "get path does not write asset to the filesystems if previously stored"() {

        when: "Get Path multiple time for same asset"
        Path path1 = pythonAssetFilesystemStore.getPath(featurePackId, "asset1.py")
        Path path2 = pythonAssetFilesystemStore.getPath(featurePackId, "asset1.py")

        then: "Asset is fetched only once for writing to the filesystem"
        1 * assetService.getAsset(_, _) >>  assetEntity
        Files.exists(path1)
        Files.exists(path2)
    }

    def "get path writes to the filesystem only once when multiple threads"() {

        when: "Multiple threads attempt to get path for same asset"
        Thread thread1 = new GetPathThread(featurePackId: featurePackId, assetName: "asset1.py")
        Thread thread2 = new GetPathThread(featurePackId: featurePackId, assetName: "asset1.py")
        Thread thread3 = new GetPathThread(featurePackId: featurePackId, assetName: "asset1.py")

        thread1.start()
        thread2.start()
        thread3.start()

        thread1.join()
        thread2.join()
        thread3.join()

        then: "Asset is fetched only once for writing to the filesystem"
        1 * assetService.getAsset(_, _) >> assetEntity

        and: "Returned path exists for each thread"
        Files.exists(thread1.path)
        Files.exists(thread2.path)
        Files.exists(thread3.path)
    }


    def "feature pack assets are successfully deleted"() {

        setup: "Mock get assets"
        assetService.getAsset(_, _) >> assetEntity

        and: "Create asset directories for 2 feature packs"
        FeaturePackEntity featurePack1 = new FeaturePackEntity(id: 1)
        FeaturePackEntity featurePack2 = new FeaturePackEntity(id: 2)

        pythonAssetFilesystemStore.getPath(featurePack1.getId(), "asset1.py")
        pythonAssetFilesystemStore.getPath(featurePack2.getId(), "asset2.py")

        when: "Delete featurepack1 assets"
        pythonAssetFilesystemStore.deleteAssetsForFeaturePack(featurePack1)

        then: "Only featurePack1's asset directory is deleted"
        Path asset1Path = Paths.get(System.getProperty("java.io.tmpdir"), featurePack1.getId().toString())
        Path asset2Path = Paths.get(System.getProperty("java.io.tmpdir"), featurePack2.getId().toString())
        !Files.exists(asset1Path)
        Files.exists(asset2Path)
    }

    private class GetPathThread extends Thread {

        Long featurePackId
        String assetName
        Path path

        @Override
        void run() {
            path = pythonAssetFilesystemStore.getPath(featurePackId, assetName)
        }
    }
}
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

package com.ericsson.bos.dr.service.subscriptions.kafka.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;

import jakarta.persistence.PreRemove;

/**
 * This class is triggered when a <code>ListenerMessageSubscriptionEntity</code> is deleted from the database.
 * The associated message subscription JKS files are deleted. If all files associated with a FeaturePack are deleted
 * then the empty feature pack directory will also be deleted.
 */
@Component
public class DeleteKafkaJKSEntityListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteKafkaJKSEntityListener.class);

    private static final String JKS_FILE_EXTENSION = "jks";

    @Value(value = "${service.message-subscriptions.kafka.tls.jks-dir}")
    private String jksBaseDir;

    /**
     * Method will be called when a <code>ListenerMessageSubscriptionEntity</code> is deleted from the database.
     * The associated message subscription JKS files are deleted. If all files associated with a FeaturePack are deleted
     * then the empty feature pack directory will also be deleted.
     *
     * @param listenerMsgSubscriptionEntity
     *         the listener entity being deleted
     */
    @PreRemove
    public void deleteForSubscription(final ListenerMessageSubscriptionEntity listenerMsgSubscriptionEntity) {
        final var jksDir = Paths.get(jksBaseDir,
                getFeaturePackName(listenerMsgSubscriptionEntity),
                getListenerName(listenerMsgSubscriptionEntity),
                getSubscriptionName(listenerMsgSubscriptionEntity));
        LOGGER.debug("Removing JKS directory: {}", jksDir);
        try {
            if (Files.isDirectory(jksDir)) {
                FileSystemUtils.deleteRecursively(jksDir);
                deleteEmptyFeaturePackDirectories(listenerMsgSubscriptionEntity);
            }
        } catch (IOException e) {
            LOGGER.warn("Error deleting JKS directory {}", jksDir, e);
        }
    }

    private void deleteEmptyFeaturePackDirectories(final ListenerMessageSubscriptionEntity listenerMsgSubscriptionEntity) {
        final var featurePackDir = Paths.get(jksBaseDir, getFeaturePackName(listenerMsgSubscriptionEntity));
        if (Files.isDirectory(featurePackDir) && isDirectoryEmpty(featurePackDir)) {
            LOGGER.debug("No JKS files remaining for FeaturePack, removing directory: {}", featurePackDir);
            try {
                FileSystemUtils.deleteRecursively(featurePackDir);
            } catch (IOException e) {
                LOGGER.warn("Error deleting JKS FeaturePack directory {}", featurePackDir, e);
            }
        }
    }

    private boolean isDirectoryEmpty(final Path directory) {
        return FileUtils.listFiles(directory.toFile(), new String[] { JKS_FILE_EXTENSION }, true).isEmpty();
    }

    private String getFeaturePackName(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        return listenerMessageSubscriptionEntity.getListenerEntity().getFeaturePack().getName();
    }

    private String getSubscriptionName(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        return listenerMessageSubscriptionEntity.getName();
    }

    private String getListenerName(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        return listenerMessageSubscriptionEntity.getListenerEntity().getName();
    }
}

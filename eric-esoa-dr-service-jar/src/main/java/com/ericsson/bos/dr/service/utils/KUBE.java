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
package com.ericsson.bos.dr.service.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for reading kubernetes resource operations via <code>KubernetesClient</code>.
 */
public class KUBE {

    private static final Logger LOGGER = LoggerFactory.getLogger(KUBE.class);
    private static final String DR_SERVICE_LABEL = "app=eric-esoa-dr-service";

    private KUBE() {}

    /**
     * Get the names of all dr-service pods.
     * Returns an empty list if any kubernetes related error occurs reading the pods.
     * @return PodSummary list
     */
    public static List<PodSummary> getPodNames() {
        try {
            final KubernetesClient kubernetesClient = SpringContextHolder.getBean(KubernetesClient.class);
            final ListOptions listOptions = new ListOptions();
            listOptions.setLabelSelector(DR_SERVICE_LABEL);
            return kubernetesClient.pods().list(listOptions).getItems().stream().map(PodSummary::new).toList();
        } catch (final Exception e) {
            LOGGER.error("Failed to read pods", e);
        }
        return Collections.emptyList();
    }

    /**
     * Get a secret.
     *
     * @param secretName
     *         secret name
     * @return Optional secret
     */
    public static Optional<Secret> getSecret(final String secretName) {
        try {
            final KubernetesClient kubernetesClient = SpringContextHolder.getBean(KubernetesClient.class);
            return Optional.ofNullable(
                    kubernetesClient.secrets().withName(secretName).get()
            );
        } catch (final Exception e) {
            LOGGER.error("Failed to read secret " + secretName, e);
        }
        return Optional.empty();
    }

    /**
     * Summary for pod with single container.
     */
    public static class PodSummary {
        private final String name;
        private final String containerName;
        private final Integer containerRestartCount;
        private final boolean containerStarted;
        private final String containerState;

        /**
         * PodSummary
         * @param pod pod
         */
        PodSummary(final Pod pod) {
            this.name = pod.getMetadata().getName();
            final ContainerStatus containerStatus = pod.getStatus().getContainerStatuses().get(0);
            this.containerName = containerStatus.getName();
            this.containerRestartCount = containerStatus.getRestartCount();
            this.containerStarted = containerStatus.getStarted();
            this.containerState = containerStatus.getState().toString();
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("name", name)
                    .append("containerName", containerName)
                    .append("containerRestartCount", containerRestartCount)
                    .append("containerStarted", containerStarted)
                    .append("containerState", containerState)
                    .toString();
        }
    }
}
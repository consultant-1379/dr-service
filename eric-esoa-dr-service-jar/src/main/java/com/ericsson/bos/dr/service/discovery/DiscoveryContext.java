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
package com.ericsson.bos.dr.service.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ericsson.bos.dr.jpa.model.ApplicationEntity;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDtoSource;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions;

/**
 * The Discovery Context for an instance of a discovery job.
 */
public class DiscoveryContext {

    private long featurePackId;
    private String featurePackName;
    private long appId;
    private String appName;
    private long jobId;
    private String jobName;
    private boolean autoReconcile;
    private List<DiscoveredObject> sources = new ArrayList<>();
    private List<DiscoveredObject> targets = new ArrayList<>();
    private ApplicationConfigurationJobDto jobConf;
    private Map<String, Object> inputs;
    private List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

    private DiscoveryContext() {}

    /**
     * Initialize the discovery context
     * @param applicationEntity application entity
     * @param jobEntity job entity
     * @return DiscoveryContext
     */
    public static DiscoveryContext initialize(ApplicationEntity applicationEntity,
                                              final JobEntity jobEntity) {
        final var discoveryContext = new DiscoveryContext();
        discoveryContext.featurePackId = applicationEntity.getFeaturePack().getId();
        discoveryContext.featurePackName = applicationEntity.getFeaturePack().getName();
        discoveryContext.appId = applicationEntity.getId();
        discoveryContext.appName = applicationEntity.getName();
        discoveryContext.jobId = jobEntity.getId();
        discoveryContext.jobName = jobEntity.getApplicationJobName();
        discoveryContext.inputs = jobEntity.getInputs();
        discoveryContext.autoReconcile = Optional.ofNullable(jobEntity.getExecutionOptions())
                .map(ExecuteJobDtoExecutionOptions::getAutoReconcile).orElse(false);
        discoveryContext.jobConf = Optional.ofNullable(applicationEntity.findJob(jobEntity.getApplicationJobName()))
                .orElseThrow(() -> new DRServiceException(ErrorCode.JOB_NOT_FOUND, discoveryContext.getJobName()));
        return discoveryContext;
    }

    public List<DiscoveredObject> getSources() {
        return sources;
    }

    public void setSources(List<DiscoveredObject> sources) {
        this.sources = sources;
    }

    public List<DiscoveredObject> getTargets() {
        return targets;
    }

    public void setTargets(List<DiscoveredObject> targets) {
        this.targets = targets;
    }

    public ApplicationConfigurationDiscoverDto getDiscoveryJobConf() {
        return jobConf.getDiscover();
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public ApplicationConfigurationJobDto getJobConf() {
        return jobConf;
    }

    public long getFeaturePackId() {
        return featurePackId;
    }

    public String getFeaturePackName() {
        return featurePackName;
    }

    public long getAppId() {
        return appId;
    }

    public String getAppName() {
        return appName;
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getSourceAndTargetsLink() {
        return jobConf.getDiscover().getLinkSourceAndTarget();
    }

    /**
     * Get source enrichment action for discovery.
     * @return optional ApplicationConfigurationActionDto
     */
    public Optional<ApplicationConfigurationActionDto> getDiscoverySourceEnrichAction() {
        final ApplicationConfigurationDiscoverDtoSource source = getDiscoveryJobConf().getSource();
        return Optional.ofNullable(source)
                .map(ApplicationConfigurationDiscoverDtoSource::getEnrichAction);
    }

    /**
     * Get target enrichment action for discovery.
     * @return optional ApplicationConfigurationActionDto
     */
    public Optional<ApplicationConfigurationActionDto> getDiscoveryTargetEnrichAction() {
        final ApplicationConfigurationDiscoverDtoSource target = getDiscoveryJobConf().getTarget();
        return Optional.ofNullable(target).map(ApplicationConfigurationDiscoverDtoSource::getEnrichAction);
    }

    /**
     * Add an execution exception thrown during the discovery flow.
     * @param throwable throwable
     */
    public void addException(Throwable throwable) {
        exceptions.add(throwable);
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public boolean isAutoReconcile() {
        return autoReconcile;
    }
}
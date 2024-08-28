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

package com.ericsson.bos.dr.service.reconcile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.utils.MapUtils;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationReconcileDtoSource;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationReconcileDtoTarget;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Reconcile context.
 */
public class ReconcileContext {

    private long jobId;
    private String jobName;
    private long featurePackId;
    private String featurePackName;
    private List<String> filters;
    private Map<String, Object> inputs;
    private List<ExecuteReconcileDtoObjectsInner> reconcileObjects;
    private ApplicationConfigurationJobDto jobConf;
    private List<DiscoveryObjectEntity> sources;

    private ReconcileContext() {
    }
    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public long getFeaturePackId() {
        return featurePackId;
    }
    
    public void setFeaturePackId(long featurePackId) {
        this.featurePackId = featurePackId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    public String getFeaturePackName() {
        return featurePackName;
    }
    
    public void setFeaturePackName(final String featurePackName) {
        this.featurePackName = featurePackName;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public List<ExecuteReconcileDtoObjectsInner> getReconcileObjects() {
        return reconcileObjects;
    }

    public void setReconcileObjects(List<ExecuteReconcileDtoObjectsInner> reconcileObjects) {
        this.reconcileObjects = reconcileObjects;
    }

    public ApplicationConfigurationJobDto getJobConf() {
        return jobConf;
    }

    public void setJobConf(ApplicationConfigurationJobDto jobConf) {
        this.jobConf = jobConf;
    }

    public List<DiscoveryObjectEntity> getSources() {
        return sources;
    }

    public void setSources(List<DiscoveryObjectEntity> sources) {
        this.sources = sources;
    }

    /**
     * Get the named filter action
     *
     * @param filterName filter name
     * @return ApplicationConfigurationReconcileDtoTargetActionsFiltersInner
     */
    public ApplicationConfigurationActionDto getReconcileAction(String filterName) {
        final ApplicationConfigurationFilterDto filter = getJobConf().getDiscover().getFilters().get(filterName);
        if (filter != null) {
            return getJobConf().getReconcile().getTarget().getReconcileActions().entrySet().stream()
                    .filter(e -> e.getKey().equals(filter.getReconcileAction()))
                    .findAny()
                    .map(Map.Entry::getValue)
                    .orElse(null);
        }
        return null;
    }

    /**
     * Get the source enrichment action.
     *
     * @return optional ApplicationConfigurationActionDto
     */
    public Optional<ApplicationConfigurationActionDto> getSourceEnrichAction() {
        return  Optional.ofNullable(getJobConf().getReconcile().getSource())
                .map(ApplicationConfigurationReconcileDtoSource::getEnrichAction);
    }

    /**
     * Get the target enrichment action.
     *
     * @return optional ApplicationConfigurationActionDto
     */
    public Optional<ApplicationConfigurationActionDto> getTargetEnrichAction() {
        return Optional.ofNullable(getJobConf().getReconcile().getTarget()).
                map(ApplicationConfigurationReconcileDtoTarget::getEnrichAction);
    }

    /**
     * Inititalize the reconcile execution context.
     *
     * @param jobId
     *         job id
     * @param jobEntity
     *         Job Entity
     * @param reconcileDetails
     *         reconcile request details
     * @param jobConf
     *         job configuration
     * @return ReconcileContext
     */
    public static ReconcileContext initialize(final long jobId,
            final JobEntity jobEntity,
            final ExecuteReconcileDto reconcileDetails,
            final ApplicationConfigurationJobDto jobConf) {
        final var reconcileContext = new ReconcileContext();
        reconcileContext.setJobId(jobId);
        reconcileContext.setJobName(jobEntity.getName());
        reconcileContext.setFeaturePackId(jobEntity.getFeaturePackId());
        reconcileContext.setFeaturePackName(jobEntity.getFeaturePackName());
        reconcileContext.setInputs(MapUtils.merge(reconcileDetails.getInputs(), jobEntity.getInputs()));
        reconcileContext.setJobConf(jobConf);
        final List<String> filters;
        if (CollectionUtils.isNotEmpty(reconcileDetails.getFilters())) {
            // include filters passed in request
            filters = reconcileDetails.getFilters();
        } else {
            // no filters specified, include all filters in the job definition
            filters = reconcileContext.getJobConf().getDiscover().getFilters().entrySet().stream()
                    .map(Map.Entry::getKey).collect(Collectors.toList());
        }
        reconcileContext.setFilters(filters);
        reconcileContext.setReconcileObjects(reconcileDetails.getObjects());
        return reconcileContext;
    }
}
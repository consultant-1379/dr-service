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
package com.ericsson.bos.dr.jpa.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDtoApiPropertiesInner;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

/**
 * Job Specification Entity.
 */
@Table(name = "job_specification")
@Entity
public class JobSpecificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "application_id ")
    private Long applicationId;

    @Column(name = "application_name ")
    private String applicationName;

    @Column(name = "application_job_name")
    private String applicationJobName;

    @Column(name = "feature_pack_id")
    private Long featurePackId;

    @Column(name = "feature_pack_name")
    private String featurePackName;

    @Column(name = "inputs", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String,Object> inputs;

    @Column(name = "execution_options", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private ExecuteJobDtoExecutionOptions executionOptions;

    @Column(name = "api_property_names", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private List<String> apiPropertyNames = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String,Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String,Object> inputs) {
        this.inputs = inputs;
    }

    public ExecuteJobDtoExecutionOptions getExecutionOptions() {
        return executionOptions;
    }

    public void setExecutionOptions(ExecuteJobDtoExecutionOptions executionOptions) {
        this.executionOptions = executionOptions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationJobName() {
        return applicationJobName;
    }

    public void setApplicationJobName(String applicationJobName) {
        this.applicationJobName = applicationJobName;
    }

    public Long getFeaturePackId() {
        return featurePackId;
    }

    public void setFeaturePackId(Long featurePackId) {
        this.featurePackId = featurePackId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getFeaturePackName() {
        return featurePackName;
    }

    public void setFeaturePackName(String featurePackName) {
        this.featurePackName = featurePackName;
    }

    public List<String> getApiPropertyNames() {
        return apiPropertyNames;
    }

    /**
     * Set api property names.
     *
     * @param applicationConfigurationJobDto
     *         applicationConfigurationJobDto
     */
    public void setApiPropertyNames(final ApplicationConfigurationJobDto applicationConfigurationJobDto) {
        if (applicationConfigurationJobDto.getApi() != null) {
            apiPropertyNames = applicationConfigurationJobDto.getApi().getProperties()
                    .stream()
                    .map(ApplicationConfigurationJobDtoApiPropertiesInner::getName)
                    .toList();
        }
    }
}
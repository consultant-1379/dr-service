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

import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.RECONCILED;
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.RECONCILING;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Discovery Object Entity.
 */
@Table(name = "discovered_object")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class DiscoveryObjectEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(fetch = FetchType.EAGER)
    @Column(name = "source_properties", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String, Object> sourceProperties;

    @Column(name="job_id")
    private long jobId;

    @Basic(fetch = FetchType.EAGER)
    @Column(name = "target_properties", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String, Object> targetProperties;

    @OneToMany(mappedBy="discoveryObject", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<FilterEntity> filters = new HashSet<>();

    @Column(name = "status")
    private String status;

    @Column(name="error_message")
    private String errorMessage;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "creation_date")
    private Date creationDate;

    @LastModifiedDate
    @Column(name = "modified_date")
    private Date modifiedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public void setFilters(Set<FilterEntity> filters) {
        this.filters = filters;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<FilterEntity> getFilters() {
        return filters;
    }

    /**
     * Get named filter.
     * @param name filter name
     * @return optional FilterEntity
     */
    public Optional<FilterEntity> getFilter(String name) {
        return getFilters().stream().filter(f -> f.getName().equals(name)).findAny();
    }

    /**
     * Add a filter.
     * @param filterEntity filter entity
     */
    public void addFilter(FilterEntity filterEntity) {
        this.filters.add(filterEntity);
        filterEntity.setDiscoveryObject(this);
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public Map<String, Object> getSourceProperties() {
        return Optional.ofNullable(sourceProperties).orElse(new HashMap<>());
    }

    public void setSourceProperties(Map<String, Object> properties) {
        this.sourceProperties = properties;
    }

    public Map<String, Object> getTargetProperties() {
        return Optional.ofNullable(targetProperties).orElse(new HashMap<>());
    }

    public void setTargetProperties(Map<String, Object> targetProperties) {
        this.targetProperties = targetProperties;
    }

    public boolean isReconcileOngoingOrCompleted() {
        return status.equals(RECONCILING.toString()) || status.equals(RECONCILED.toString());
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
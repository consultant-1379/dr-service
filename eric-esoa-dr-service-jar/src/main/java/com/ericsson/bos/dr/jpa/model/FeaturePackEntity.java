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

import com.ericsson.bos.dr.service.execution.executors.python.PythonAssetFilesystemStore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * FeaturePack Entity.
 */
@Table(name = "feature_pack")
@EntityListeners({AuditingEntityListener.class, PythonAssetFilesystemStore.class})
@Entity
public class FeaturePackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy="featurePack", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ApplicationEntity> applications = new HashSet<>();
    
    @OneToOne(mappedBy="featurePack", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private PropertiesEntity properties;

    @OneToMany(mappedBy="featurePack", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<InputConfigurationEntity> inputs = new HashSet<>();

    @OneToMany(mappedBy="featurePack", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ListenerEntity> listeners = new HashSet<>();

    @OneToMany(mappedBy="featurePack", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<AssetEntity> assets = new HashSet<>();

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Set<ApplicationEntity> getApplications() {
        return applications;
    }

    public Optional<PropertiesEntity> getProperties() { return Optional.ofNullable(properties); }

    /**
     * Set properties.
     *
     * @param propertiesEntity
     *         properties entity
     */
    public void setProperties(final PropertiesEntity propertiesEntity) {
        this.properties = propertiesEntity;
        propertiesEntity.setFeaturePack(this);
    }

    public Set<AssetEntity> getAssets() {
        return assets;
    }

    public Set<ListenerEntity> getListeners() {
        return listeners;
    }

    public Set<InputConfigurationEntity> getInputs() {
        return inputs;
    }

    /**
     * Add application to the feature pack.
     * @param applicationEntity application entity
     */
    public void addApplication(final ApplicationEntity applicationEntity) {
        applications.add(applicationEntity);
        applicationEntity.setFeaturePack(this);
    }

    /**
     * Add listener to the feature pack.
     * @param listenerEntity listener entity
     */
    public void addListener(final ListenerEntity listenerEntity) {
        listeners.add(listenerEntity);
        listenerEntity.setFeaturePack(this);
    }

    /**
     * Add asset to the feature pack.
     * @param assetEntity asset entity
     */
    public void addAsset(final AssetEntity assetEntity) {
        assets.add(assetEntity);
        assetEntity.setFeaturePack(this);
    }

    /**
     * Add inputs to the feature pack.
     * @param inputConfigurationEntity inputs entity
     */
    public void addInputs(final InputConfigurationEntity inputConfigurationEntity) {
        inputs.add(inputConfigurationEntity);
        inputConfigurationEntity.setFeaturePack(this);
    }
}
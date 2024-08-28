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
package com.ericsson.bos.dr.model.mappers;

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.APPLICATION;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.ASSET;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.INPUT;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.LISTENER;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.PROPERTIES;

import java.util.List;

import com.ericsson.bos.dr.jpa.model.ApplicationEntity;
import com.ericsson.bos.dr.jpa.model.AssetEntity;
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.jpa.model.InputConfigurationEntity;
import com.ericsson.bos.dr.jpa.model.ListenerEntity;
import com.ericsson.bos.dr.jpa.model.PropertiesEntity;
import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.ListenerConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.PropertiesConfigurationDto;

/**
 * Map of list of <code>ConfigurationFile</code> to <code>FeaturePackEntity</code>.
 */
public class FeaturePackEntityMapper implements Mapper<List<ConfigurationFile>, FeaturePackEntity> {

    private final String name;
    private final String description;

    /**
     * FeaturePackEntityMapper.
     * @param name feature pack name
     * @param description feature pack description
     */
    public FeaturePackEntityMapper(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public FeaturePackEntity apply(List<ConfigurationFile> configFiles) {
        final var featurePackEntity = new FeaturePackEntity();
        featurePackEntity.setName(name);
        featurePackEntity.setDescription(description);

        configFiles.stream()
                .filter(cf -> APPLICATION.equals(cf.getType()))
                .map(cf -> applicationMapper().apply(cf))
                .forEach(featurePackEntity::addApplication);

        configFiles.stream()
                .filter(cf -> PROPERTIES.equals(cf.getType()))
                .map(cf -> propertiesMapper().apply(cf))
                .findFirst()
                .ifPresent(featurePackEntity::setProperties);

        configFiles.stream()
                .filter(cf -> INPUT.equals(cf.getType()))
                .map(cf -> inputMapper().apply(cf))
                .forEach(featurePackEntity::addInputs);

        configFiles.stream()
                .filter(cf -> LISTENER.equals(cf.getType()))
                .map(cf -> listenerMapper().apply(cf))
                .forEach(featurePackEntity::addListener);

        configFiles.stream()
                .filter(cf -> ASSET.equals(cf.getType()))
                .map(cf -> assetMapper().apply(cf))
                .forEach(featurePackEntity::addAsset);
        return featurePackEntity;
    }

    private Mapper<ConfigurationFile, ApplicationEntity> applicationMapper() {
        return configFile -> {
            final ApplicationConfigurationDto appConfigDto = (ApplicationConfigurationDto) configFile.getConfigSupplier().get();
            final var appEntity = new ApplicationEntity();
            appEntity.setName(appConfigDto.getName());
            appEntity.setContents(configFile.getOriginalContents());
            appEntity.setDescription(appConfigDto.getDescription());
            appEntity.setFilename(configFile.getFilename());
            appEntity.setConfig(appConfigDto);
            return appEntity;
        };
    }

    private Mapper<ConfigurationFile, PropertiesEntity> propertiesMapper() {
        return configFile -> {
            final PropertiesConfigurationDto propertiesDto = (PropertiesConfigurationDto) configFile.getConfigSupplier().get();
            final var propertiesEntity = new PropertiesEntity();
            propertiesEntity.setName("fp_properties");
            propertiesEntity.setContents(configFile.getOriginalContents());
            propertiesEntity.setFilename(configFile.getFilename());
            propertiesEntity.setConfig(propertiesDto);
            return propertiesEntity;
        };
    }

    private Mapper<ConfigurationFile, InputConfigurationEntity> inputMapper() {
        return configFile -> {
            final InputConfigurationDto inputDto = (InputConfigurationDto) configFile.getConfigSupplier().get();
            final var inputsEntity = new InputConfigurationEntity();
            inputsEntity.setName(inputDto.getName());
            inputsEntity.setDescription(inputDto.getDescription());
            inputsEntity.setContents(configFile.getOriginalContents());
            inputsEntity.setFilename(configFile.getFilename());
            inputsEntity.setConfig(inputDto);
            inputsEntity.setReadOnly(true);
            return inputsEntity;
        };
    }

    private Mapper<ConfigurationFile, AssetEntity> assetMapper() {
        return configFile -> {
            final var assetEntity = new AssetEntity();
            assetEntity.setName(configFile.getFilename());
            assetEntity.setFilename(configFile.getFilename());
            assetEntity.setContents(configFile.getOriginalContents());
            return assetEntity;
        };
    }

    private Mapper<ConfigurationFile, ListenerEntity> listenerMapper() {
        return configFile -> {
            final ListenerConfigurationDto listenerConfigDto =
                    (ListenerConfigurationDto) configFile.getConfigSupplier().get();
            final var listenerEntity = new ListenerEntity();
            listenerEntity.setName(listenerConfigDto.getName());
            listenerEntity.setDescription(listenerConfigDto.getDescription());
            listenerEntity.setContents(configFile.getOriginalContents());
            listenerEntity.setFilename(configFile.getFilename());
            listenerEntity.setConfig(listenerConfigDto);
            return listenerEntity;
        };
    }
}
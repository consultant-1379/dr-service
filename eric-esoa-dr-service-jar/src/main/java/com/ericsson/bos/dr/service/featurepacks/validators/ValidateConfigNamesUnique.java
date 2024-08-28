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
package com.ericsson.bos.dr.service.featurepacks.validators;

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.APPLICATION;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.INPUT;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.LISTENER;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType;
import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.ListenerConfigurationDto;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validate the names of the configuration files in the feature pack are unique.
 */
@Component
@Order(4)
public class ValidateConfigNamesUnique implements Validator {

    private static final Function<ConfigurationFile, String> APP_NAME_MAPPER =
            configFile -> ((ApplicationConfigurationDto) configFile.getConfigSupplier().get()).getName();

    private static final Function<ConfigurationFile, String> INPUT_NAME_MAPPER =
            configFile -> ((InputConfigurationDto) configFile.getConfigSupplier().get()).getName();
    private static final Function<ConfigurationFile, String> LISTENER_NAME_MAPPER =
            configFile -> ((ListenerConfigurationDto) configFile.getConfigSupplier().get()).getName();

    @Override
    public void validate(final String featurePackName, List<ConfigurationFile> featurePackFiles) {
        verifyUniqueNames(APPLICATION, APP_NAME_MAPPER, featurePackFiles);
        verifyUniqueNames(LISTENER, LISTENER_NAME_MAPPER,featurePackFiles);
        verifyUniqueNames(INPUT, INPUT_NAME_MAPPER, featurePackFiles);
    }

    private void verifyUniqueNames(FeaturePackFileType featurePackFileType, Function<ConfigurationFile, String> nameMapper,
                                   List<ConfigurationFile> configFiles) {
        final Set<String> names = new HashSet<>();
        final Set<String> nonUniqueNames = configFiles.stream()
                .filter(cf -> featurePackFileType.equals(cf.getType()))
                .map(nameMapper)
                .filter(name -> !names.add(name))
                .collect(Collectors.toSet());
        if (!nonUniqueNames.isEmpty()) {
            throw new DRServiceException(ErrorCode.DUPLICATE_CONFIG_NAME, nonUniqueNames.toString(), featurePackFileType.getFolder());
        }
    }
}
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;

/**
 * Generate job name with applicationJobName and the current time stamp.
 */
public class JobNameGenerator implements Function<ExecuteJobDto, String> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    @Override
    public String apply(final ExecuteJobDto jobDto) {
        return jobDto.getApplicationJobName() + "_" + LocalDateTime.now().format(formatter);
    }
}

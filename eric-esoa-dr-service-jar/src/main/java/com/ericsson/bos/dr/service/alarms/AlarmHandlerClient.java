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

package com.ericsson.bos.dr.service.alarms;

import java.util.function.Supplier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.configuration.ExecutorsConfiguration;
import com.ericsson.bos.so.alarm.model.Alarm;
import com.ericsson.bos.so.alarm.service.AlarmSender;
import com.ericsson.bos.so.alarm.util.AlarmUtil;

/**
 * Client for sending alarms to the Alarm Handler service.
 * An alarm will be created in the Alarm Handler service by merging the dynamic information
 * from the fault indication constructed here, with the static information from the Fault alarm
 * mapping ConfigMap in the eric-esoa-dr repo.
 */
@Component
public class AlarmHandlerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmHandlerClient.class);

    private static final String SERVICE_NAME_IN_FAULT_MAPPINGS = "eric-esoa-dr-service";

    @Value("${security.systemMonitoring.faultManagement.expiration}")
    private String alarmExpirationSeconds;

    /**
     * Set required=false as the instance of <code>AlarmSender</code> is provided by
     * the eric-esoa-alarm-helper lib. And that lib will not be available if alarms
     * are disabled.
     */
    @Autowired(required = false)
    private AlarmSender alarmSender;

    /**
     * Raise an alarm to the Alarm Handler service.
     *
     * @param faultName
     *         fault name
     * @param alarmDescription
     *         alarm description
     * @param faultyResource
     *         faulty resource
     */
    @Async(ExecutorsConfiguration.ALARM_ASYNC_EXECUTOR)
    public void raiseAlarm(final FaultName faultName, final Supplier<String> alarmDescription, final Supplier<String> faultyResource) {
        if (alarmSender == null) {
            return;
        }
        LOGGER.info("Raising alarm: name: {}, description: {}, resource: {}", faultName.getName(), alarmDescription, faultyResource);
        final Alarm alarm = new Alarm();
        alarm.setServiceName(SERVICE_NAME_IN_FAULT_MAPPINGS);
        alarm.setFaultName(faultName.getName());
        alarm.setDescription(alarmDescription.get());
        alarm.setEventTime(AlarmUtil.getCurrentTimeStamp());
        alarm.setExpiration(Integer.parseInt(alarmExpirationSeconds));
        alarm.setFaultyUrlResource(faultyResource.get());
        alarmSender.postAlarm(alarm, Thread.currentThread().getName());
    }

}

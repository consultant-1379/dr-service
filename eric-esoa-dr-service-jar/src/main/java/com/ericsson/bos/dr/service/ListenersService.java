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

package com.ericsson.bos.dr.service;

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.LISTENER_MESSAGE_SUBSCRIPTION_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.LISTENER_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.NO_TRIGGER_MATCH;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.bos.dr.jpa.FeaturePackRepository;
import com.ericsson.bos.dr.jpa.ListenerMessageSubscriptionRepository;
import com.ericsson.bos.dr.jpa.model.ApplicationEntity;
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.jpa.model.ListenerEntity;
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.model.mappers.ListenerMessageSubscriptionDtoMapper;
import com.ericsson.bos.dr.model.mappers.ListenerMessageSubscriptionEntityMapper;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.job.JobStatusCondition;
import com.ericsson.bos.dr.service.listeners.ListenerExpressionEvaluator;
import com.ericsson.bos.dr.service.listeners.ListenerSubstitutionCtx;
import com.ericsson.bos.dr.service.listeners.TriggerMatcher;
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber;
import com.ericsson.bos.dr.service.utils.JSON;
import com.ericsson.bos.dr.web.v1.api.model.CreateListenerMessageSubscriptionRequest;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum;
import com.ericsson.bos.dr.web.v1.api.model.ListenerConfigurationDtoAllOfTriggers;
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionDto;
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionListDto;
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionResponseDto;
import com.ericsson.bos.dr.web.v1.api.model.ListenerTriggerResponseDto;

/**
 * Listeners Service.
 */
@Service
public class ListenersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenersService.class);

    @Autowired
    private FeaturePackRepository featurePackRepository;

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ListenerExpressionEvaluator expressions;

    @Autowired
    private TriggerMatcher triggerMatcher;

    @Autowired
    private KafkaSubscriber kafkaSubscriber;

    @Autowired
    private ListenerMessageSubscriptionRepository listenerMessageSubscriptionRepository;

    /**
     * Trigger a Listener to perform a Discovery & Reconciliation job for a matching trigger condition.
     * <p>
     * The Listener configuration defines trigger conditions. The payload received in the trigger
     * request is applied to each of the trigger conditions to find a match. If a match is found then the discovery and
     * reconciliation is performed for the job defined in the condition.
     * <p>
     * The job id will be returned when the job has been created in the database. The  Discovery & Reconciliation will
     * proceed asynchronously from that point.
     *
     * @param featurePackName
     *         feature pack name
     * @param listenerName
     *         listener name
     * @param event
     *         event
     * @return String the discovery job id
     */
    @Transactional(readOnly = true)
    public String triggerAsync(final String featurePackName, final String listenerName, final Map<String, Object> event,
            final Long messageSubscriptionId) {
        final var featurePackEntity = getFeaturePackEntity(featurePackName);
        final var listenerEntity = getListenerEntity(featurePackEntity, listenerName);
        final var trigger = getTriggers(listenerEntity, featurePackEntity, event);
        final var applicationEntity = getApplicationEntity(trigger, featurePackEntity);

        final var listenerSubstitutionCtx = new ListenerSubstitutionCtx(event);
        final Map<String, Object> evaluatedInputs = evaluateInputs(trigger.getInputs(), listenerSubstitutionCtx, featurePackEntity.getId());
        final ExecuteJobDto discoveryJobRequest = createDiscoveryJobRequest(featurePackEntity,
                listenerEntity, applicationEntity, trigger, evaluatedInputs);

        return (messageSubscriptionId == null) ? discoveryService.startDiscovery(discoveryJobRequest) :
                discoveryService.startDiscovery(discoveryJobRequest, messageSubscriptionId);
    }

    /**
     * Trigger a Listener to perform a Discovery & Reconciliation job for a matching trigger condition.
     * <p>
     * The Listener configuration defines trigger conditions. The payload received in the trigger
     * request is applied to each of the trigger conditions to find a match. If a match is found then the discovery and
     * reconciliation is performed for the job defined in the condition.
     * <p>
     * The ListenerTriggerResponseDto response will be returned when the Discovery and Reconciliation has completed, either successfully or
     * with error.
     *
     * @param featurePackName feature pack name
     * @param listenerName    listener name
     * @param event           event
     * @return ListenerTriggerResponseDto
     */
    @Transactional(readOnly = true)
    public ListenerTriggerResponseDto trigger(final String featurePackName, final String listenerName, final Map<String, Object> event) {
        final String jobId = triggerAsync(featurePackName, listenerName, event, null);

        JobStatusCondition.awaitJobInState(Long.valueOf(jobId), EnumSet.of(StatusEnum.COMPLETED,
                StatusEnum.DISCOVERY_FAILED, StatusEnum.PARTIALLY_RECONCILED, StatusEnum.RECONCILE_FAILED));
        final var jobDto = jobService.detachAndGetJobById(jobId);
        if (StatusEnum.DISCOVERY_FAILED.equals(jobDto.getStatus())) {
            return new ListenerTriggerResponseDto().job(jobDto).outputs(Collections.emptyMap());
        }

        final var featurePackEntity = getFeaturePackEntity(featurePackName);
        final var listenerEntity = getListenerEntity(featurePackEntity, listenerName);

        final var listenerSubstitutionCtx = new ListenerSubstitutionCtx(event);
        listenerSubstitutionCtx.addResults(getDiscoveredObjects(jobId));
        return new ListenerTriggerResponseDto().job(jobService.detachAndGetJobById(jobId))
                .outputs(evaluateOutputs(listenerEntity.getConfig().getOutputs(), listenerSubstitutionCtx, featurePackEntity.getId()));
    }

    /**
     * Create a message subscription.
     * <p>
     *     Creates a subscription to a message broker, i.e. kafka.
     *     The subscription will be linked to a given listener in a feature pack.
     *     On receiving a message from the message broker the listener trigger will be called.
     * </p>
     *
     * @param featurePackName
     *         feature pack name
     * @param listenerName
     *         listener name
     * @param listenerMessageSubscriptionDto
     *         contains message subscription configuration
     * @return ListenerMessageSubscriptionResponseDto
     */
    @Transactional
    public ListenerMessageSubscriptionResponseDto createMessageSubscription(final String featurePackName, final String listenerName,
            final CreateListenerMessageSubscriptionRequest listenerMessageSubscriptionDto) {
        final var featurePackEntity = featurePackRepository.findByName(featurePackName)
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackName));

        final var listenerEntity = featurePackEntity.getListeners()
                .stream()
                .filter(l -> l.getName().equals(listenerName)).findAny()
                .orElseThrow(() -> new DRServiceException(LISTENER_NOT_FOUND, listenerName, featurePackName));

        final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity =
                new ListenerMessageSubscriptionEntityMapper(listenerEntity).apply(listenerMessageSubscriptionDto);
        final Long listenerMessageSubscriptionEntityId = listenerMessageSubscriptionRepository.save(listenerMessageSubscriptionEntity).getId();
        kafkaSubscriber.subscribe(listenerMessageSubscriptionEntity);
        return new ListenerMessageSubscriptionResponseDto().id(String.valueOf(listenerMessageSubscriptionEntityId));
    }

    /**
     * Delete a given listener message subscription.
     *
     * @param featurePackName
     *         feature pack name
     * @param listenerName
     *         listener name
     * @param messageSubscriptionId
     *         message subscription id
     */
    @Transactional
    public void deleteMessageSubscription(final String featurePackName, final String listenerName, final String messageSubscriptionId) {
        final var featurePackEntity = featurePackRepository.findByName(featurePackName)
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackName));

        final var listenerEntity = featurePackEntity.getListeners()
                .stream()
                .filter(lsnrEntity -> lsnrEntity.getName().equals(listenerName)).findAny()
                .orElseThrow(() -> new DRServiceException(LISTENER_NOT_FOUND, listenerName, featurePackName));

        final var listenerMessageSubscriptionEntity = listenerEntity.getListenerMessageSubscriptions()
                .stream()
                .filter(subscriptionEntity -> String.valueOf(subscriptionEntity.getId()).equals(messageSubscriptionId)).findAny().
                orElseThrow(() ->
                        new DRServiceException(LISTENER_MESSAGE_SUBSCRIPTION_NOT_FOUND, messageSubscriptionId, listenerName, featurePackName));

        kafkaSubscriber.unsubscribe(String.valueOf(listenerMessageSubscriptionEntity.getId()));
        listenerMessageSubscriptionRepository.delete(listenerMessageSubscriptionEntity);
    }

    /**
     * Get the list of message subscriptions for a given listener.
     *
     * @param featurePackName
     *         feature pack name
     * @param listenerName
     *         listener name
     * @return ListenerMessageSubscriptionListDto
     */
    @Transactional(readOnly = true)
    public ListenerMessageSubscriptionListDto getMessageSubscriptions(final String featurePackName, final String listenerName) {
        final var featurePackEntity = featurePackRepository.findByName(featurePackName)
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackName));

        final var listenerEntity = featurePackEntity.getListeners()
                .stream()
                .filter(l -> l.getName().equals(listenerName)).findAny()
                .orElseThrow(() -> new DRServiceException(LISTENER_NOT_FOUND, listenerName, featurePackName));

        final List<ListenerMessageSubscriptionDto> listenerMessageSubscriptionDtos =
                listenerEntity.getListenerMessageSubscriptions()
                .stream()
                .map(mc -> new ListenerMessageSubscriptionDtoMapper().apply(mc))
                .toList();
        return new ListenerMessageSubscriptionListDto().items(listenerMessageSubscriptionDtos).totalCount(listenerMessageSubscriptionDtos.size());
    }

    private ExecuteJobDto createDiscoveryJobRequest(final FeaturePackEntity featurePackEntity, final ListenerEntity listenerEntity,
                                                    final ApplicationEntity applicationEntity,
                                                    final ListenerConfigurationDtoAllOfTriggers trigger,
                                                    final Map<String, Object> evaluatedInputs) {
        return new ExecuteJobDto()
                .name(String.format("%s-%s", listenerEntity.getName(), trigger.getJobName()))
                .description(String.format("Triggered by listener %s with condition '%s' ",
                        listenerEntity.getName(), trigger.getCondition()))
                .featurePackId(featurePackEntity.getId().toString())
                .applicationId(applicationEntity.getId().toString())
                .applicationJobName(trigger.getJobName())
                .executionOptions(new ExecuteJobDtoExecutionOptions().autoReconcile(true))
                .inputs(evaluatedInputs);
    }

    private Map<String, Object> evaluateInputs(final Map<String, Object> inputs, final ListenerSubstitutionCtx evaluationContext,
                                               final long featurePackId) {
        return inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> expressions.evaluate(e.getValue().toString(), evaluationContext.get(), featurePackId)));
    }

    private Map<String, Object> evaluateOutputs(final Map<String, Object> outputs, final ListenerSubstitutionCtx evaluationContext,
                                                final long featurePackId) {
        if (outputs != null) {
            return outputs.entrySet().stream().collect(HashMap::new,
                    (m, e) -> m.put(e.getKey(), evaluateOutput(e, evaluationContext, featurePackId)),
                    HashMap::putAll);
        }
        return Collections.emptyMap();
    }

    private Object evaluateOutput(final Map.Entry<String, Object> entry, final ListenerSubstitutionCtx evaluationContext, final long featurePackId) {
        try {
            final String output = expressions.evaluate(entry.getValue().toString(), evaluationContext.get(), featurePackId);
            return JSON.isJsonStr(output) ? JSON.readObject(output) : output;
        } catch (final Exception ex) {
            LOGGER.error("Error evaluating output: " + entry.getKey(), ex);
            return null;
        }
    }

    private List<Map<String, Object>> getDiscoveredObjects(final String jobId) {
        return discoveryService.getAllDiscoveredObject(jobId).getItems().stream()
                .map(o -> (Map<String, Object>) JSON.convert(o, Map.class))
                .toList();
    }

    private ListenerEntity getListenerEntity(final FeaturePackEntity featurePackEntity, final String listenerName) {
        return featurePackEntity.getListeners().stream().filter(l -> l.getName().equals(listenerName)).findAny()
                .orElseThrow(() -> new DRServiceException(LISTENER_NOT_FOUND, listenerName, featurePackEntity.getName()));
    }

    private ListenerConfigurationDtoAllOfTriggers getTriggers(final ListenerEntity listenerEntity, final FeaturePackEntity featurePackEntity,
            final Map<String, Object> event) {
        return triggerMatcher.match(listenerEntity, event, featurePackEntity.getId())
                .orElseThrow(() -> new DRServiceException(NO_TRIGGER_MATCH, event.toString(), listenerEntity.getName(), featurePackEntity.getName()));
    }

    private ApplicationEntity getApplicationEntity(final ListenerConfigurationDtoAllOfTriggers trigger, final FeaturePackEntity featurePackEntity) {
        return featurePackEntity.getApplications().stream().filter(app -> app.getName().equals(trigger.getApplicationName()))
                .findAny()
                .orElseThrow(() -> new DRServiceException(APP_NOT_FOUND, trigger.getApplicationName(), featurePackEntity.getName()));
    }

    private FeaturePackEntity getFeaturePackEntity(final String featurePackName) {
        return featurePackRepository.findByName(featurePackName)
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackName));
    }
}
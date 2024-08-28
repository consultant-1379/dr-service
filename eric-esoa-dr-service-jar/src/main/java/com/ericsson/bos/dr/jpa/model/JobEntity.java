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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ericsson.bos.dr.service.job.JobEventPublisher;
import com.ericsson.bos.dr.service.job.JobStatusCondition;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.scheduling.support.CronExpression;

/**
 * Job Entity.
 */
@Table(name = "job")
@Entity
@EntityListeners({AuditingEntityListener.class, JobStatusCondition.class, JobEventPublisher.class})
public class JobEntity  {

    public static final String SCHEDULED_STATUS = "SCHEDULED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinColumn(name = "job_specification_id", referencedColumnName = "id")
    private JobSpecificationEntity jobSpecification = new JobSpecificationEntity();

    @Column(name = "job_schedule_id")
    private Long jobScheduleId;

    @Column(name = "message_subscription_id")
    private Long messageSubscriptionId;

    @Column(name = "discovered_objects_count")
    private Integer discoveredObjectsCount = 0;

    @Column(name = "reconciled_objects_count")
    private Integer reconciledObjectsCount = 0;

    @Column(name = "reconciled_objects_error_count")
    private Integer reconciledObjectsErrorCount = 0;

    @Column(name = "error_message", columnDefinition="VARCHAR(4000)")
    private String errorMessage;

    @Column(name = "start_date")
    private Date startDate;

    @LastModifiedDate
    @Column(name = "modified_date")
    private Date modifiedDate;

    @Column(name = "end_date")
    private Date completedDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "status")
    private String jobStatus;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "lock_time")
    private Long lockTime;

    @Column(name = "locked")
    private boolean locked;

    @Column(name = "executor")
    private String executor;

    @Column(name = "reconcile_request", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private ExecuteReconcileDto reconcileRequest;

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getName() {
        return jobSpecification.getName();
    }

    /**
     * Set job name.
     * @param name job name
     */
    public void setName(final String name) {
        jobSpecification.setName(name);
    }

    public Map<String,Object> getInputs() {
        return jobSpecification.getInputs();
    }

    /**
     * Set job inputs.
     * @param inputs job inputs
     */
    public void setInputs(final Map<String, Object> inputs) {
        jobSpecification.setInputs(inputs);
    }

    public ExecuteJobDtoExecutionOptions getExecutionOptions() {
        return jobSpecification.getExecutionOptions();
    }

    /**
     * Set job execution options.
     * @param executionOptions execution options
     */
    public void setExecutionOptions(final ExecuteJobDtoExecutionOptions executionOptions) {
        jobSpecification.setExecutionOptions(executionOptions);
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getDescription() {
        return jobSpecification.getDescription();
    }

    /**
     * Set job description.
     * @param description job description
     */
    public void setDescription(final String description) {
        jobSpecification.setDescription(description);
    }

    public Long getApplicationId() {
        return jobSpecification.getApplicationId();
    }

    /**
     * Set application id.
     * @param applicationId application id.
     */
    public void setApplicationId(final Long applicationId) {
        jobSpecification.setApplicationId(applicationId);
    }

    public String getApplicationJobName() {
        return jobSpecification.getApplicationJobName();
    }

    /**
     * Set application job name.
     * @param applicationJobName application job name
     */
    public void setApplicationJobName(final String applicationJobName) {
        jobSpecification.setApplicationJobName(applicationJobName);
    }

    public Integer getDiscoveredObjectsCount() {
        return discoveredObjectsCount;
    }

    public void setDiscoveredObjectsCount(Integer discoveredObjectsCount) {
        this.discoveredObjectsCount = discoveredObjectsCount;
    }

    public Integer getReconciledObjectsCount() {
        return reconciledObjectsCount;
    }

    public void setReconciledObjectsCount(Integer reconciledObjectsCount) {
        this.reconciledObjectsCount = reconciledObjectsCount;
    }

    public Integer getReconciledObjectsErrorCount() {
        return reconciledObjectsErrorCount;
    }

    public void setReconciledObjectsErrorCount(Integer reconciledObjectsErrorCount) {
        this.reconciledObjectsErrorCount = reconciledObjectsErrorCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getFeaturePackId() {
        return jobSpecification.getFeaturePackId();
    }

    /**
     * Set feature pack id.
     * @param featurePackId feature pack id
     */
    public void setFeaturePackId(final Long featurePackId) {
        jobSpecification.setFeaturePackId(featurePackId);
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getApplicationName() {
        return jobSpecification.getApplicationName();
    }

    /**
     * Set application name.
     * @param applicationName application name
     */
    public void setApplicationName(final String applicationName) {
        jobSpecification.setApplicationName(applicationName);
    }

    public String getFeaturePackName() {
        return jobSpecification.getFeaturePackName();
    }

    /**
     * Set feature pack name
     * @param featurePackName feature pack name
     */
    public void setFeaturePackName(final String featurePackName) {
        jobSpecification.setFeaturePackName(featurePackName);
    }

    public List<String> getApiPropertyNames() {
        return jobSpecification.getApiPropertyNames();
    }

    /**
     * Set api property names.
     *
     * @param applicationConfigurationJobDto
     *         applicationConfigurationJobDto
     */
    public void setApiPropertyNames(final ApplicationConfigurationJobDto applicationConfigurationJobDto) {
        jobSpecification.setApiPropertyNames(applicationConfigurationJobDto);
    }

    public ExecuteReconcileDto getReconcileRequest() {
        return reconcileRequest;
    }

    public void setReconcileRequest(ExecuteReconcileDto reconcileRequest) {
        this.reconcileRequest = reconcileRequest;
    }

    public Long getLockTime() {
        return lockTime;
    }

    public void setLockTime(Long lockTime) {
        this.lockTime = lockTime;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public JobSpecificationEntity getJobSpecification() {
        return jobSpecification;
    }

    public void setJobSpecification(JobSpecificationEntity jobSpecification) {
        this.jobSpecification = jobSpecification;
    }

    public Long getJobScheduleId() {
        return jobScheduleId;
    }

    public void setJobScheduleId(Long jobScheduleId) {
        this.jobScheduleId = jobScheduleId;
    }

    public Long getMessageSubscriptionId() {
        return messageSubscriptionId;
    }

    public void setMessageSubscriptionId(final Long messageSubscriptionId) {
        this.messageSubscriptionId = messageSubscriptionId;
    }

    /**
     * If the messageSubscriptionId property is set then the job was initiated by a message.
     *
     * @return true if job was initiated by a message, otherwise false
     */
    public boolean isJobInitiatedByMessage() {
        return messageSubscriptionId != null;

    }

    /**
     * If the jobScheduleId property is set then the job was initiated by a schedule.
     *
     * @return true if job was initiated by a schedule, otherwise false
     */
    public boolean isJobInitiatedBySchedule() {
        return jobScheduleId != null;
    }

    /**
     * Set the lock properties on the job.
     */
    public void lock() {
        this.locked = true;
        this.lockTime = System.currentTimeMillis();
    }

    /**
     * Reset the lock properties on the job.
     */
    public void unlock() {
        this.locked = false;
        this.lockTime = null;
    }

    /**
     * Set the next schedule for the job.
     * @param jobScheduleEntity job schedule entity
     */
    public void scheduleJob(final JobScheduleEntity jobScheduleEntity) {
        final CronExpression cronExpression = CronExpression.parse(jobScheduleEntity.getExpression());
        final LocalDateTime nextDueDate = cronExpression.next(LocalDateTime.now());
        setJobStatus(SCHEDULED_STATUS);
        setDueDate(nextDueDate);
        setJobScheduleId(jobScheduleEntity.getId());
    }
}
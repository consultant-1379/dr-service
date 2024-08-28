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
package com.ericsson.bos.dr.service.exceptions;

import java.util.stream.Stream;

import org.springframework.http.HttpStatus;

/**
 * Error codes defined in error.properties.
 */
public enum ErrorCode {

    FP_NOT_FOUND("DR-01", HttpStatus.NOT_FOUND),
    FP_IO_READ_ERROR("DR-02", HttpStatus.INTERNAL_SERVER_ERROR),
    EMPTY_FP_ARCHIVE("DR-03", HttpStatus.BAD_REQUEST),
    MISSING_APP_CONFIG("DR-04", HttpStatus.BAD_REQUEST),
    FP_IO_WRITE_ERROR("DR-05", HttpStatus.INTERNAL_SERVER_ERROR),
    APP_NOT_FOUND("DR-06", HttpStatus.NOT_FOUND),
    LISTENER_NOT_FOUND("DR-07", HttpStatus.NOT_FOUND),
    SCHEMA_ERROR("DR-09", HttpStatus.BAD_REQUEST),
    FP_ALREADY_EXISTS("DR-10", HttpStatus.CONFLICT),
    BAD_REQUEST_PARAM("DR-11", HttpStatus.BAD_REQUEST),
    JQ_ERROR("DR-13", HttpStatus.INTERNAL_SERVER_ERROR),
    JOB_NOT_FOUND_IN_CONFIG("DR-15", HttpStatus.BAD_REQUEST),
    MISSING_INPUTS("DR-16", HttpStatus.BAD_REQUEST),
    JOB_NOT_FOUND("DR-17", HttpStatus.NOT_FOUND),
    OPERATION_ONGOING("DR-18", HttpStatus.CONFLICT),
    SUBSTITUTION_FAILED("DR-19", HttpStatus.INTERNAL_SERVER_ERROR),
    EXECUTION_STEP_ERROR("DR-20", HttpStatus.INTERNAL_SERVER_ERROR),
    EXECUTOR_FILE_ERROR("DR-21", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_SORTING_PARAM("DR-22", HttpStatus.BAD_REQUEST),
    INVALID_FILTER_PARAM("DR-23", HttpStatus.BAD_REQUEST),
    FILTER_CONDITION_NOT_SUPPORTED("DR-24", HttpStatus.BAD_REQUEST),
    NON_UNIQUE_SOURCE_TARGET_MAPPING("DR-25", HttpStatus.BAD_REQUEST),
    FP_FOLDER_ERROR("DR-26", HttpStatus.BAD_REQUEST),
    NO_TRIGGER_MATCH("DR-27", HttpStatus.NOT_FOUND),
    DUPLICATE_CONFIG_NAME("DR-28", HttpStatus.BAD_REQUEST),
    FP_JOB_INPROGRESS("DR-29", HttpStatus.CONFLICT),
    MULTIPLE_PROPERTIES_CONFIG("DR-30", HttpStatus.BAD_REQUEST),
    CONFIG_NOT_FOUND("DR-31", HttpStatus.NOT_FOUND),
    CONFIG_EXISTS("DR-32", HttpStatus.CONFLICT),
    READ_ONLY_ACCESS("DR-33", HttpStatus.CONFLICT),
    CONFIG_NAME_MISMATCH("DR-34", HttpStatus.BAD_REQUEST),
    ID_OR_NAME_NOT_PROVIDED("DR-35", HttpStatus.BAD_REQUEST),
    INVALID_STATE_FOR_RECONCILE("DR-36", HttpStatus.CONFLICT),
    OBJECT_NOT_FOUND("DR-37", HttpStatus.NOT_FOUND),
    JOB_SCHEDULE_NOT_FOUND("DR-38", HttpStatus.NOT_FOUND),
    JOB_SCHEDULE_EXISTS("DR-39", HttpStatus.CONFLICT),
    INVALID_CRON_EXPRESSION("DR-40", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_SCHEDULED_JOB("DR-41", HttpStatus.CONFLICT),
    CANNOT_DELETE_ACTIVE_SCHEDULED_JOB("DR-42", HttpStatus.CONFLICT),
    LISTENER_MESSAGE_SUBSCRIPTION_NOT_FOUND("DR-43", HttpStatus.NOT_FOUND),
    CANNOT_ENABLE_JOB_SCHEDULE("DR-44", HttpStatus.CONFLICT),
    CONNECTED_SYSTEM_NOT_FOUND("DR-45", HttpStatus.NOT_FOUND),
    CONNECTED_PROPERTIES_NOT_FOUND("DR-46", HttpStatus.NOT_FOUND),
    FAILED_TO_CREATE_MESSAGE_SUBSCRIPTION("DR-47", HttpStatus.INTERNAL_SERVER_ERROR),
    FAILED_TO_DELETE_MESSAGE_SUBSCRIPTION("DR-48", HttpStatus.INTERNAL_SERVER_ERROR),
    FAILED_TO_CREATE_JKS("DR-49", HttpStatus.INTERNAL_SERVER_ERROR),
    JKS_FILE_ERROR("DR-50", HttpStatus.INTERNAL_SERVER_ERROR),
    KAKFA_BROKER_NOT_AVAILABLE("DR-51", HttpStatus.INTERNAL_SERVER_ERROR),
    KAFKA_TOPIC_NOT_FOUND("DR-52", HttpStatus.INTERNAL_SERVER_ERROR),
    KAFKA_SSL_AUTH_ERROR("DR-53", HttpStatus.INTERNAL_SERVER_ERROR),
    KAFKA_PROPERTIES_VALIDATION_ERROR("DR-54", HttpStatus.BAD_REQUEST),
    KAFKA_SECRET_NOT_FOUND("DR-55", HttpStatus.INTERNAL_SERVER_ERROR),
    KAFKA_SECRET_DATA_FIELD_NOT_FOUND("DR-56", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_FORCE_DELETE_INPROGRESS_SCHEDULED_JOBS("DR-57", HttpStatus.CONFLICT),
    GENERAL_ERROR("DR-500", HttpStatus.INTERNAL_SERVER_ERROR);



    public final String code;
    public final HttpStatus httpStatus;

    /**
     * ErrorCode
     *
     * @param code
     *         error code
     * @param httpStatus
     *         associated http status
     */
    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Get ErrorCode instance for specified code.
     * @param code string error code
     * @return ErrorCode
     */
    public static ErrorCode getInstance(final String code) {
        return Stream.of(ErrorCode.values()).filter(e -> e.getErrorCode().equals(code)).findFirst().orElse(null);
    }
}

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
package com.ericsson.bos.dr.web;

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.BAD_REQUEST_PARAM;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.GENERAL_ERROR;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.web.v1.api.model.ErrorResponseDto;
import com.ericsson.oss.orchestration.so.common.error.factory.ErrorMessageFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Custom exception handlers.
 */
@ControllerAdvice
public class DRControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(DRControllerAdvice.class);

    /**
     * Handle method validation exception.
     *
     * @param ex MethodArgumentNotValidException
     * @return ErrorResponseDto
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleValidationError(final MethodArgumentNotValidException ex) {
        LOGGER.error("Request body validation error", ex);
        final String errorData = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError ? (((FieldError) error).getField() + " " + error.getDefaultMessage())
                        : error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        final var errorMessage = ErrorMessageFactory.buildFrom(BAD_REQUEST_PARAM.getErrorCode(), errorData);
        final var errorResponseDto = new ErrorResponseDto().errorCode(errorMessage.getErrorCode())
                .errorMessage(errorMessage.getUserMessage());
        return new ResponseEntity<>(errorResponseDto, BAD_REQUEST_PARAM.getHttpStatus());
    }

    /**
     * Handle HttpMessageNotReadableException.
     *
     * @param ex
     *         HttpMessageNotReadableException
     * @return ErrorResponseDto
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleValidationError(final HttpMessageNotReadableException ex) {
        return getResponseEntity(ex);
    }
    
    /**
     * Handle DataIntegrityViolationException.
     *
     * @param ex
     *         DataIntegrityViolationException
     * @return ErrorResponseDto
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleValidationError(final DataIntegrityViolationException ex) {
        return getResponseEntity(ex);
    }

    /**
     * D&R service exception.
     *
     * @param ex DRServiceException
     * @return ErrorResponseDto
     */
    @ExceptionHandler(DRServiceException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleDRServiceException(final DRServiceException ex) {
        LOGGER.error("DR Service Exception", ex);
        final var errorResponseDto = new ErrorResponseDto().errorCode(ex.getErrorMessage().getErrorCode())
                .errorMessage(ex.getErrorMessage().getUserMessage());
        return new ResponseEntity<>(errorResponseDto, ex.getHttpStatus());
    }

    /**
     * General exception handler.
     *
     * @param ex Exception
     * @return ErrorResponseDto
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleUnexpectedError(final Exception ex) {
        LOGGER.error("Internal Server Error", ex);
        if (ex.getCause() instanceof DRServiceException) {
            return handleDRServiceException((DRServiceException) ex.getCause());
        } else {
            final var errorMessage = ErrorMessageFactory.buildFrom(GENERAL_ERROR.getErrorCode(), ExceptionUtils.getRootCauseMessage(ex));
            final var errorResponseDto = new ErrorResponseDto().errorCode(errorMessage.getErrorCode())
                    .errorMessage(errorMessage.getUserMessage());
            return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private ResponseEntity<ErrorResponseDto> getResponseEntity(final Exception ex) {
        LOGGER.error("Request body validation error", ex);
        final var errorMessage = ErrorMessageFactory.buildFrom(BAD_REQUEST_PARAM.getErrorCode(), ex.getMessage());
        final var errorResponseDto = new ErrorResponseDto().errorCode(errorMessage.getErrorCode())
                .errorMessage(errorMessage.getUserMessage());
        return new ResponseEntity<>(errorResponseDto, BAD_REQUEST_PARAM.getHttpStatus());
    }

}
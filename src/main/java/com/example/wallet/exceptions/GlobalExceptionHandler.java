package com.example.wallet.exceptions;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(EntityNotFoundException exception) {
        log.warn("Entity not found: {}", exception.getMessage());
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested resource was not found.");
        errorDetail.setProperty("reason", exception.getMessage());
        return errorDetail;
    }

    @ExceptionHandler(EntityExistsException.class)
    public ProblemDetail handleEntityExistsException(EntityExistsException exception) {
        log.warn("Entity already exists: {}", exception.getMessage());
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The resource you are trying to create already exists.");
        errorDetail.setProperty("reason", exception.getMessage());
        return errorDetail;
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handlePessimisticLockingFailureException(PessimisticLockingFailureException exception) {
        log.warn("Pessimistic locking failure or Deadlock detected: {}", exception.getMessage(), exception);
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A concurrent operation prevented your request from completing. Please retry.");
        errorDetail.setProperty("details", "The requested resource is currently locked or a deadlock was detected.");
        return errorDetail;
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFundsException(InsufficientFundsException exception) {
        log.warn("Insufficient funds: {}", exception.getMessage());
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Insufficient balance for the requested operation.");
        errorDetail.setProperty("currentBalanceIssue", exception.getMessage());
        return errorDetail;
    }

    @ExceptionHandler(SameWalletTransferException.class)
    public ProblemDetail handleSameWalletTransferException(SameWalletTransferException exception) {
        log.warn("Same wallet funds: {}", exception.getMessage());
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Same id`s from wallet and to wallet.");
        errorDetail.setProperty("details", exception.getMessage());
        return errorDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed for method arguments: {}", validationErrors);
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request payload.");
        errorDetail.setProperty("errors", validationErrors);
        return errorDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException exception) {
        String validationErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed due to constraint violations: {}", validationErrors);
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed.");
        errorDetail.setProperty("errors", validationErrors);
        return errorDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception exception) {
        log.error("An unhandled internal server error occurred: {}", exception.getMessage(), exception);
        ProblemDetail errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.");
        errorDetail.setProperty("traceId", UUID.randomUUID().toString());
        errorDetail.setProperty("moreInfo", "Please contact support with this trace ID.");
        return errorDetail;
    }
}

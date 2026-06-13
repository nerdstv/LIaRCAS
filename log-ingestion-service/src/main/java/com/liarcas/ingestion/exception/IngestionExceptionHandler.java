package com.liarcas.ingestion.exception;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

@RestControllerAdvice
public class IngestionExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(
            MethodArgumentNotValidException exception,
            ServletWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("urn:liarcas:problem:validation-error"));
        problemDetail.setTitle("Request validation failed");
        problemDetail.setDetail("One or more request fields are invalid.");
        problemDetail.setInstance(URI.create(request.getRequest().getRequestURI()));
        problemDetail.setProperty("errors", exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList());

        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedJson(
            HttpMessageNotReadableException exception,
            ServletWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("urn:liarcas:problem:malformed-json"));
        problemDetail.setTitle("Malformed JSON request");
        problemDetail.setDetail("Request body must contain valid JSON.");
        problemDetail.setInstance(URI.create(request.getRequest().getRequestURI()));
        problemDetail.setProperty("errors", List.of(new ValidationError("body", "Request body must contain valid JSON.")));

        return problemDetail;
    }

    private ValidationError toValidationError(FieldError fieldError) {
        return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    record ValidationError(String field, String message) {
    }
}

package com.liarcas.rootcause.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Produces RFC 7807 problem responses for query validation errors.
 */
@RestControllerAdvice
public class RootCauseExceptionHandler {

    /**
     * Converts invalid query filter/pagination input into a structured problem response.
     *
     * @param exception validation failure raised while processing the query
     * @param request current web request
     * @return problem detail payload describing the invalid request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidQuery(IllegalArgumentException exception, ServletWebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("urn:liarcas:problem:invalid-query"));
        problemDetail.setTitle("Invalid log query");
        problemDetail.setDetail(exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequest().getRequestURI()));
        return problemDetail;
    }

    /**
     * Converts malformed query parameters (e.g. non-numeric page/size, unparsable dates)
     * into a structured problem response.
     *
     * @param exception type conversion failure raised while binding request parameters
     * @param request current web request
     * @return problem detail payload describing the malformed parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleMalformedParameter(MethodArgumentTypeMismatchException exception, ServletWebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("urn:liarcas:problem:malformed-parameter"));
        problemDetail.setTitle("Malformed query parameter");
        problemDetail.setDetail("Parameter '" + exception.getName() + "' has an invalid value.");
        problemDetail.setInstance(URI.create(request.getRequest().getRequestURI()));
        return problemDetail;
    }
}

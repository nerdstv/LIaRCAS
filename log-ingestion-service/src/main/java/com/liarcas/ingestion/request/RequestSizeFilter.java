package com.liarcas.ingestion.request;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet filter that rejects HTTP requests to /logs exceeding the configured maximum size.
 * Returns 413 Payload Too Large when Content-Length header exceeds the limit.
 * 
 * This provides an outer guardrail before the request body is deserialized, reducing
 * memory and CPU overhead from very large payloads.
 */
public class RequestSizeFilter implements Filter {

    private final RequestSizeProperties requestSizeProperties;
    private final ObjectMapper objectMapper;

    /**
     * Creates a filter that enforces maximum payload size for /logs.
     *
     * @param requestSizeProperties configured payload size limits
     * @param objectMapper mapper used to serialize error responses
     */
    public RequestSizeFilter(RequestSizeProperties requestSizeProperties, ObjectMapper objectMapper) {
        this.requestSizeProperties = requestSizeProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Rejects requests to /logs when Content-Length exceeds the configured limit.
     *
     * @param servletRequest current servlet request
     * @param servletResponse current servlet response
     * @param filterChain remaining filter chain
     * @throws IOException when response writing fails
     * @throws ServletException when downstream filters fail
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        
        if (!(servletRequest instanceof HttpServletRequest request)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (!(servletResponse instanceof HttpServletResponse response)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Only apply size check to the /logs endpoint
        if (!"/logs".equals(request.getRequestURI())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Check Content-Length header if present
        String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null && !contentLengthHeader.isBlank()) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader);
                long maxSize = requestSizeProperties.getMaxSize();

                if (contentLength > maxSize) {
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    
                    String errorResponse = objectMapper.writeValueAsString(new ErrorResponse(
                            "urn:liarcas:problem:payload-too-large",
                            "Payload Too Large",
                            HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                            "Request payload exceeds the maximum allowed size of " + maxSize + " bytes.",
                            request.getRequestURI()
                    ));
                    
                    response.getWriter().write(errorResponse);
                    response.getWriter().flush();
                    return;
                }
            } catch (NumberFormatException e) {
                // If Content-Length is malformed, let it pass through and let
                // the container handle it as an invalid request
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Simple problem detail response matching RFC 7807 format.
     */
    public static class ErrorResponse {
        public String type;
        public String title;
        public int status;
        public String detail;
        public String instance;

        /**
         * Creates an RFC 7807 compatible error payload.
         *
         * @param type machine-readable problem type URI
         * @param title short human-readable summary
         * @param status HTTP status code
         * @param detail detailed error message
         * @param instance request path that caused the error
         */
        public ErrorResponse(String type, String title, int status, String detail, String instance) {
            this.type = type;
            this.title = title;
            this.status = status;
            this.detail = detail;
            this.instance = instance;
        }

        /**
         * Returns the problem type.
         *
         * @return problem type URI
         */
        public String getType() { return type; }

        /**
         * Returns the problem title.
         *
         * @return problem title
         */
        public String getTitle() { return title; }

        /**
         * Returns the HTTP status code.
         *
         * @return HTTP status code
         */
        public int getStatus() { return status; }

        /**
         * Returns the problem detail message.
         *
         * @return detail message
         */
        public String getDetail() { return detail; }

        /**
         * Returns the request instance path.
         *
         * @return request path
         */
        public String getInstance() { return instance; }
    }
}

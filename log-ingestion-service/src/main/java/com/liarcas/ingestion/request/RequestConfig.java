package com.liarcas.ingestion.request;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Registers request-related infrastructure beans for ingestion endpoints.
 */
@Configuration
@EnableConfigurationProperties(RequestSizeProperties.class)
public class RequestConfig {

    /**
     * Creates a servlet filter that rejects oversized log ingestion requests.
     *
     * @param requestSizeProperties max request-size configuration
     * @param objectMapper mapper used to serialize RFC 7807 responses
     * @return configured request size filter
     */
    @Bean
    RequestSizeFilter requestSizeFilter(RequestSizeProperties requestSizeProperties, ObjectMapper objectMapper) {
        return new RequestSizeFilter(requestSizeProperties, objectMapper);
    }
}

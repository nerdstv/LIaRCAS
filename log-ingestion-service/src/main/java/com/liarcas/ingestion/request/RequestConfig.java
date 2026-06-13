package com.liarcas.ingestion.request;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(RequestSizeProperties.class)
public class RequestConfig {

    @Bean
    RequestSizeFilter requestSizeFilter(RequestSizeProperties requestSizeProperties, ObjectMapper objectMapper) {
        return new RequestSizeFilter(requestSizeProperties, objectMapper);
    }
}

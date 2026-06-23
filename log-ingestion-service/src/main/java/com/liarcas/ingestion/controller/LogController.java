package com.liarcas.ingestion.controller;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.liarcas.ingestion.security.TenantPrincipal;
import com.liarcas.models.LogEvent;

/**
 * Receives log events from clients and forwards them to Kafka.
 */
@RestController
public class LogController {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    /**
     * Creates a controller that publishes incoming events to Kafka.
     *
     * @param kafkaTemplate Kafka producer for log events
     */
    public LogController(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Validates and enriches an incoming event, then publishes it to the raw logs topic.
     *
     * @param logEvent incoming event payload
     * @param authentication authenticated tenant context
     * @return the accepted and enriched event
     */
    @PostMapping("/logs")
    public LogEvent ingestLog(@Valid @RequestBody LogEvent logEvent, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof TenantPrincipal tenantPrincipal)) {
            throw new IllegalStateException("Authenticated tenant principal is required");
        }

        logEvent.setTenantId(tenantPrincipal.tenantId());

        if (logEvent.getId() == null || logEvent.getId().isBlank()) {
            logEvent.setId(UUID.randomUUID().toString());
        }

        if (logEvent.getTimestamp() == null) {
            logEvent.setTimestamp(Instant.now());
        }

        kafkaTemplate.send("raw-logs", logEvent.getId(), logEvent);
        System.out.println("Received log event: " + logEvent.getMessage());
        return logEvent;
    }
}

package com.liarcas.ingestion.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.liarcas.ingestion.security.TenantPrincipal;
import com.liarcas.models.LogEvent;

@RestController
public class LogController {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    public LogController(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/logs")
    public LogEvent ingestLog(@RequestBody LogEvent logEvent, Authentication authentication) {
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

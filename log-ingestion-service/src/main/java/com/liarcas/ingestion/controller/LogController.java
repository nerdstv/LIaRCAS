package com.liarcas.ingestion.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.liarcas.models.LogEvent;

@RestController
public class LogController {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    
    public LogController(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/logs")
    public LogEvent ingestLog(@RequestBody LogEvent logEvent) {
        
        if(logEvent.getId() == null){
            logEvent.setId(UUID.randomUUID().toString());
        }

        if(logEvent.getTimestamp() == null){
            logEvent.setTimestamp(Instant.now());
        }
        kafkaTemplate.send("raw-logs", logEvent.getId(), logEvent); 
        System.out.println("Received log event: " + logEvent.getMessage());
        return logEvent;
        
    }
}

package com.liarcas.processing.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.liarcas.models.LogEvent;

@Component
public class LogConsumer {
    @KafkaListener(topics = "raw-logs", groupId = "log-processing-group")
    public void consume(LogEvent message) {   
        System.out.println("Consumed log event: " + message);
    }
}
